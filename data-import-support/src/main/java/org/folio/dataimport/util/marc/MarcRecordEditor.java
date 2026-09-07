package org.folio.dataimport.util.marc;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.vertx.core.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marc4j.MarcException;
import org.marc4j.MarcJsonWriter;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.impl.Verifier;

/**
 * Shared parse -&gt; mutate (via {@link MarcFieldEditor}) -&gt; write-back logic, operating on any FOLIO record
 * shape through the {@link MarcContentHolder} SPI rather than importing a specific FOLIO {@code Record} type.
 *
 * <p>Owns the parsed-record-content cache: strong (equals/hashCode-based), content-addressed keys, so
 * structurally-equal parsed content maps to the same entry regardless of {@code String} instance identity, plus
 * {@code recordStats()} and no custom executor (all usage here is synchronous). Every write path invalidates the
 * pre-mutation content key before re-keying under the new content, so a mutated marc4j {@code Record} is never
 * left reachable under a cache key whose content no longer matches it.
 */
public final class MarcRecordEditor {

  private static final String INVALID_DATA_FIELD_MSG = "Field '%s' is not a data field.";
  private static final Logger LOGGER = LogManager.getLogger();
  private static final CacheLoader<String, Record> PARSED_RECORD_CONTENT_CACHE_LOADER;
  private static final LoadingCache<String, Record> PARSED_RECORD_CONTENT_CACHE;
  private static final int MAX_CACHE_SIZE = 2000;

  static {
    // this function is executed when creating a new item to be saved in the cache.
    // In this case, this is a MARC4J Record
    PARSED_RECORD_CONTENT_CACHE_LOADER = content -> MarcContentCodec.parse(content).orElse(null);

    PARSED_RECORD_CONTENT_CACHE =
      Caffeine.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        // strong (equals/hashCode-based) keys: the cache is content-addressed, so structurally-equal
        // parsed content must map to the same entry regardless of String instance identity.
        .recordStats()
        .build(PARSED_RECORD_CONTENT_CACHE_LOADER);
  }

  private MarcRecordEditor() {
  }

  public static MarcContentCacheStats getCacheStats() {
    return MarcContentCacheStats.fromCaffeine(PARSED_RECORD_CONTENT_CACHE.stats());
  }

  /**
   * Parses {@code holder}'s current marc content into a marc4j record, serving reads from (and loading into) the
   * shared cache. Public escape-hatch for caller logic that needs raw marc4j access not covered by one of this
   * class's named operations (e.g. mutating via a normalizer that isn't part of this class's API).
   *
   * @param holder holder wrapping the record to parse
   * @return the parsed marc4j record, or {@code null} if {@code holder} is null, has no content, or the content
   *   could not be parsed
   */
  public static Record computeMarcRecord(MarcContentHolder holder) {
    if (holder == null) {
      return null;
    }
    Object marcContent = holder.getMarcContent();
    if (marcContent == null || !isNotBlank(marcContent.toString())) {
      return null;
    }
    try {
      var content = MarcContentCodec.canonicalize(marcContent);
      return PARSED_RECORD_CONTENT_CACHE.get(content);
    } catch (Exception e) {
      LOGGER.warn("computeMarcRecord:: Error during the transformation to marc record", e);
      try {
        String fallbackContent = MarcContentCodec.canonicalize(marcContent);
        return MarcContentCodec.parse(fallbackContent).orElse(null);
      } catch (Exception ex) {
        LOGGER.warn("computeMarcRecord:: Error during the building of MarcReader", ex);
      }
      return null;
    }
  }

  /**
   * Recalculates the leader (via a stream-writer round trip) and rewrites {@code holder}'s marc content for
   * {@code marcRecord}, then refreshes the cache entry for the new content. Public escape-hatch for caller logic
   * that mutates a marc4j record through means outside this class's named operations.
   *
   * @param holder     holder whose marc content should be replaced
   * @param marcRecord mutated marc4j record to serialize
   * @return true if the leader was recalculated and the record content was updated, false if an error occurred
   */
  public static boolean recalculateAndWriteBack(MarcContentHolder holder, Record marcRecord) {
    try {
      // marcRecord has already been mutated in place by the caller. The cache entry keyed by the holder's
      // current (pre-mutation) content string now points at an object whose fields no longer match that key -
      // invalidate it before anyone else can observe the stale mapping, and before we re-key it below.
      String staleContentKey = MarcContentCodec.canonicalize(holder.getMarcContent());
      PARSED_RECORD_CONTENT_CACHE.invalidate(staleContentKey);

      String parsedContentString = MarcContentCodec.serializeWithRecalculatedLeader(marcRecord);
      // save parsed content string to cache then set it on the holder
      PARSED_RECORD_CONTENT_CACHE.put(parsedContentString, marcRecord);
      holder.setMarcContent(parsedContentString);
      return true;
    } catch (Exception e) {
      if (isOversizedRecordException(e)) {
        LOGGER.warn("recalculateAndWriteBack:: Record {} exceeds the MARC21 99999-byte length limit "
          + "and cannot be serialized", holder.getRecordId(), e);
      } else {
        LOGGER.warn("recalculateAndWriteBack:: Failed to recalculate leader and parsed record for "
          + "record: {}", holder.getRecordId(), e);
      }
      return false;
    }
  }

  /**
   * Detects marc4j's oversized-record failure - {@code MarcStreamWriter} refuses to write a record whose
   * ISO 2709 serialization would exceed the MARC21 99999-byte record-length limit. Checking the exception type
   * first, then the message, keeps this from misclassifying unrelated {@link MarcException}s (e.g. an oversized
   * individual field, which marc4j reports with a different message) as this specific, actionable condition.
   */
  private static boolean isOversizedRecordException(Exception e) {
    return e instanceof MarcException && e.getMessage() != null && e.getMessage().contains("99999 bytes");
  }

  public static boolean addFieldToMarcRecord(MarcContentHolder holder, String field, char subfield, String value) {
    boolean result = false;
    try {
      if (holder.getMarcContent() != null) {
        Record marcRecord = computeMarcRecord(holder);
        if (marcRecord != null) {
          MarcFieldEditor.addSubfieldToField(marcRecord, field, subfield, value);
          result = recalculateAndWriteBack(holder, marcRecord);
        }
      }
    } catch (Exception e) {
      LOGGER.warn("addFieldToMarcRecord:: Failed to add additional subfield {} for field {} to record {}",
        subfield, field, holder.getRecordId(), e);
    }
    return result;
  }

  public static boolean addControlledFieldToMarcRecord(MarcContentHolder holder, String field, String value,
                                                        boolean replace) {
    try {
      addControlledFieldToMarcRecordOrThrow(holder, field, value, replace);
      return true;
    } catch (Exception e) {
      LOGGER.warn("addControlledFieldToMarcRecord:: Failed to add additional controlled field {} to record {}",
        field, holder.getRecordId(), e);
      return false;
    }
  }

  /**
   * Throwing core of {@link #addControlledFieldToMarcRecord(MarcContentHolder, String, String, boolean)}. Same
   * behaviour, except failures - null/blank content, an unparseable record, or a failed write-back - raise a
   * {@link MarcContentException} carrying the failure detail instead of being swallowed into a boolean. This lets
   * callers that need the real cause (e.g. to chain it into their own exception, such as a caller performing a
   * time-sensitive update that must surface a parse/write failure rather than silently no-op) call this directly
   * instead of losing the cause to {@link #addControlledFieldToMarcRecord}'s boolean contract.
   */
  public static void addControlledFieldToMarcRecordOrThrow(MarcContentHolder holder, String field, String value,
                                                            boolean replace) {
    if (holder.getMarcContent() == null) {
      throw new MarcContentException(format(
        "Cannot add controlled field '%s' to record '%s': record, parsed record, or its content is null",
        field, holder.getRecordId()));
    }
    Record marcRecord = computeMarcRecord(holder);
    if (marcRecord == null) {
      throw new MarcContentException(format(
        "Cannot add controlled field '%s' to record '%s': failed to parse the parsed record content",
        field, holder.getRecordId()));
    }
    MarcFieldEditor.addOrReplaceControlField(marcRecord, field, value, replace);
    if (!recalculateAndWriteBack(holder, marcRecord)) {
      throw new MarcContentException(format(
        "Cannot add controlled field '%s' to record '%s': failed to recalculate leader and write back the "
        + "parsed record content", field, holder.getRecordId()));
    }
  }

  public static String getValueFromControlledField(MarcContentHolder holder, String tag) {
    try {
      Record marcRecord = computeMarcRecord(holder);
      if (marcRecord != null) {
        return MarcFieldEditor.getControlFieldValue(marcRecord, tag);
      }
    } catch (Exception e) {
      LOGGER.warn("getValueFromControlledField:: Failed to read controlled field {} from record {}", tag,
        holder.getRecordId(), e);
      return null;
    }
    return null;
  }

  public static Optional<String> getValueFromDataField(MarcContentHolder holder, String tag, char ind1, char ind2,
                                                        char subfield) {
    checkForControlField(tag);

    return Optional.ofNullable(computeMarcRecord(holder))
      .map(marcRecord -> MarcFieldEditor.getDataFieldSubfieldValue(marcRecord, tag, ind1, ind2, subfield));
  }

  public static Optional<String> getValueFromDataField(MarcContentHolder holder, String tag, char subfield) {
    checkForControlField(tag);

    return Optional.ofNullable(computeMarcRecord(holder))
      .map(marcRecord -> MarcFieldEditor.getDataFieldSubfieldValue(marcRecord, tag, subfield));
  }

  public static boolean removeField(MarcContentHolder holder, String fieldName, char subfield, String value) {
    boolean isFieldRemoveSucceed = false;
    try {
      if (holder.getMarcContent() != null) {
        Record marcRecord = computeMarcRecord(holder);
        if (marcRecord != null) {
          if (StringUtils.isEmpty(value)) {
            isFieldRemoveSucceed = MarcFieldEditor.removeFirstField(marcRecord, fieldName);
          } else {
            isFieldRemoveSucceed = MarcFieldEditor.removeFieldWithSubfieldValue(marcRecord, fieldName, subfield,
              value);
          }

          if (isFieldRemoveSucceed) {
            isFieldRemoveSucceed = recalculateAndWriteBack(holder, marcRecord);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.warn("removeField:: Failed to remove controlled field {} from record {}",
        fieldName, holder.getRecordId(), e);
    }
    return isFieldRemoveSucceed;
  }

  public static boolean removeField(MarcContentHolder holder, String field) {
    return removeField(holder, field, '\0', null);
  }

  public static boolean addDataFieldToMarcRecord(MarcContentHolder holder, String tag, char ind1, char ind2,
                                                  char subfield, String value) {
    boolean result = false;
    try {
      if (holder.getMarcContent() != null) {
        MarcFactory factory = MarcFactory.newInstance();
        Record marcRecord = computeMarcRecord(holder);
        if (marcRecord != null) {
          DataField dataField = factory.newDataField(tag, ind1, ind2);
          dataField.addSubfield(factory.newSubfield(subfield, value));
          MarcFieldEditor.addDataFieldInOrder(marcRecord, dataField);
          result = recalculateAndWriteBack(holder, marcRecord);
        }
      }
    } catch (Exception e) {
      LOGGER.warn("addDataFieldToMarcRecord:: Failed to add additional data field {} to record {}",
        tag, holder.getRecordId(), e);
    }
    return result;
  }

  public static boolean isFieldExist(MarcContentHolder holder, String tag, char subfield, String value) {
    if (value == null) {
      // nothing to match against - deliberately "not found", rather than letting the trim() below
      // NPE and get masked as a caught-and-logged "error during the search" false.
      return false;
    }
    try {
      Record marcRecord = computeMarcRecord(holder);
      if (marcRecord != null) {
        return MarcFieldEditor.fieldExists(marcRecord, tag, subfield, value);
      }
    } catch (Exception e) {
      LOGGER.warn("isFieldExist:: Error during the search a field in the record", e);
      return false;
    }
    return false;
  }

  /**
   * Removes subfields with the given code and any of the given values, from every field with one of the given
   * tags, and writes the result back onto {@code holder}. Deliberately has no try/catch of its own - any parse
   * or write failure propagates to the caller.
   *
   * @param holder       holder wrapping the record to mutate
   * @param tags         tags that could contain the subfield
   * @param subfieldCode subfield code to remove
   * @param values       values of the subfield to remove
   */
  public static void removeSubfieldsThatContainsValues(MarcContentHolder holder, List<String> tags,
                                                       char subfieldCode, List<String> values) {
    if (holder.getMarcContent() != null) {
      Record marcRecord = computeMarcRecord(holder);
      if (marcRecord != null) {
        MarcFieldEditor.removeSubfieldValues(marcRecord, tags, subfieldCode, values);

        // marcRecord may be a cache hit shared with other holders whose content still equals the pre-mutation
        // key below - invalidate that stale mapping before writing the new one, exactly like
        // recalculateAndWriteBack does, so a mutated marc4j Record is never left reachable under a stale key.
        String staleContentKey = MarcContentCodec.canonicalize(holder.getMarcContent());
        PARSED_RECORD_CONTENT_CACHE.invalidate(staleContentKey);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MarcWriter marcStreamWriter = new MarcStreamWriter(new ByteArrayOutputStream());
        MarcWriter marcJsonWriter = new MarcJsonWriter(baos);
        // use stream writer to recalculate leader
        marcStreamWriter.write(marcRecord);
        marcJsonWriter.write(marcRecord);

        String parsedContentString = new JsonObject(baos.toString()).encode();
        PARSED_RECORD_CONTENT_CACHE.put(parsedContentString, marcRecord);
        holder.setMarcContent(parsedContentString);
      }
    }
  }

  /**
   * Removes all fields with the given tag from the record wrapped by {@code holder}, recalculating the leader in
   * the process.
   *
   * @param holder   holder wrapping the record to mutate
   * @param fieldTag tag of the field(s) to remove
   * @return true if at least one field was found and removed (regardless of whether the follow-up write-back
   *   succeeded, matching the convention of always returning the same record instance whether or not the write
   *   actually landed)
   */
  public static boolean removeFieldFromMarcRecord(MarcContentHolder holder, String fieldTag) {
    Record marcRecord = computeMarcRecord(holder);
    if (marcRecord == null) {
      return false;
    }
    boolean fieldsRemoved = MarcFieldEditor.removeAllFieldsWithTag(marcRecord, fieldTag);
    if (fieldsRemoved) {
      recalculateAndWriteBack(holder, marcRecord);
    }
    return fieldsRemoved;
  }

  public static boolean isSubfieldExist(MarcContentHolder holder, char subFieldCode) {
    try {
      Record marcRecord = computeMarcRecord(holder);
      if (marcRecord != null) {
        return MarcFieldEditor.subfieldExists(marcRecord, subFieldCode);
      }
    } catch (Exception e) {
      LOGGER.warn("isSubfieldExist:: Error during the search a subfield in the record", e);
      return false;
    }
    return false;
  }

  private static void checkForControlField(String tag) {
    if (Verifier.isControlField(tag)) {
      String msg = INVALID_DATA_FIELD_MSG.formatted(tag);
      LOGGER.warn("getValueFromDataField:: {}", msg);
      throw new IllegalArgumentException(msg);
    }
  }
}
