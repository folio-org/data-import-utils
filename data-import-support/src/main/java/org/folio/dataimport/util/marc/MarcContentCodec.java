package org.folio.dataimport.util.marc;

import io.vertx.core.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcJsonWriter;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.Record;

/**
 * String/{@link Record}-level parse, serialize and canonicalize helpers, with no dependency on
 * any FOLIO {@code Record} type and no cache. Unlike {@link MarcFieldEditor} (pure marc4j, no I/O), this class
 * owns the marc4j reader/writer I/O boundary: it is what turns MARC-in-JSON {@link String} content into a
 * parsed marc4j {@link Record} and back.
 *
 * <p>Both {@link #parse(String)} and {@link #serializeWithRecalculatedLeader(Record)}
 * deliberately do not catch or wrap any exception they encounter - callers own deciding whether/how to recover
 * (e.g. a caller may retry with different content, or fall back and log). Swallowing exceptions here would
 * take away a caller's ability to react to a real exception propagating out of a parse/serialize attempt (see
 * {@link MarcRecordEditor#computeMarcRecord}'s two-try fallback structure for an example).
 */
public final class MarcContentCodec {

  private MarcContentCodec() {
  }

  /**
   * Parses MARC-in-JSON {@code content} into a marc4j {@link Record}.
   *
   * <p>Does not catch any exception raised while reading {@code content} - a malformed or unparseable
   * {@code content} propagates whatever exception marc4j's {@link MarcJsonReader} throws, rather than being
   * converted into an empty {@link Optional}.
   *
   * @param content MARC-in-JSON content to parse
   * @return {@link Optional} containing the parsed record, or {@link Optional#empty()} if the reader produced
   *   no record for {@code content}
   */
  public static Optional<Record> parse(String content) {
    var reader = new MarcJsonReader(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    if (reader.hasNext()) {
      return Optional.of(reader.next());
    }
    return Optional.empty();
  }

  /**
   * Serializes {@code marcRecord} to MARC-in-JSON, recalculating its leader in the process by round-tripping
   * through a throwaway {@link MarcStreamWriter} (ISO 2709) before writing the final {@link MarcJsonWriter}
   * output.
   *
   * <p>Does not catch any exception raised while writing - in particular, marc4j's {@link org.marc4j.MarcException}
   * for a record whose ISO 2709 serialization would exceed the MARC21 99999-byte record-length limit propagates
   * unchanged, so callers can keep distinguishing that specific failure from any other.
   *
   * @param marcRecord marc4j record to serialize
   * @return the serialized, leader-recalculated MARC-in-JSON content
   * @throws Exception whatever marc4j or the underlying streams raise while writing
   */
  public static String serializeWithRecalculatedLeader(Record marcRecord) throws Exception {
    try (var os = new ByteArrayOutputStream()) {
      MarcWriter streamWriter = new MarcStreamWriter(new ByteArrayOutputStream());
      MarcWriter jsonWriter = new MarcJsonWriter(os);
      try (AutoCloseable closeStreamWriter = streamWriter::close; AutoCloseable closeJsonWriter = jsonWriter::close) {
        // use stream writer to recalculate leader
        streamWriter.write(marcRecord);
        jsonWriter.write(marcRecord);

        return new JsonObject(os.toString()).encode();
      }
    }
  }

  /**
   * Canonicalizes parsed record content (either a {@link String} or a structured type such as
   * {@link JsonObject}/{@code Map}) into a single canonical JSON string, so that content differing only in
   * whitespace or key order produces the same canonical form.
   *
   * @param content parsed record content
   * @return canonicalized content string
   */
  public static String canonicalize(Object content) {
    return canonicalizeJson(content).encode();
  }

  /**
   * Canonicalizes parsed record content (either a {@link String} or a structured type such as
   * {@link JsonObject}/{@code Map}) into a single canonical JSON string, so that content differing only in
   * whitespace or key order produces the same canonical form.
   *
   * @param content parsed record content
   * @return canonicalized JsonObject
   */
  public static JsonObject canonicalizeJson(Object content) {
    return (content instanceof String contentStr) ? new JsonObject(contentStr) : JsonObject.mapFrom(content);
  }
}
