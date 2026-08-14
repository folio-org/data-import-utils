package org.folio.dataimport.util.marc;

import static java.util.Arrays.asList;

import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MarcRecordAnalyzer implements RecordAnalyzer {
  private static final Logger LOGGER = LogManager.getLogger();
  private static final String LEADER_KEY = "leader";
  private static final int RECORD_TYPE_INDEX = 6;
  private static final Set<Character> BIB_CODES =
    new HashSet<>(asList('a', 'c', 'd', 'e', 'f', 'g', 'i', 'j', 'k', 'm', 'o', 'p', 'r', 't'));
  private static final Set<Character> HOLDING_CODES = new HashSet<>(asList('u', 'v', 'x', 'y'));
  private static final Set<Character> AUTHORITY_CODES = new HashSet<>(Collections.singletonList('z'));

  /**
   * Processes a json object to determine type of record BIB, HOLDING, AUTHORITY, NA.
   * <br>
   * <br>
   * Note. We assume that a json object with the field "leader" is MARC record.
   * Field "leader" contains a string and the 6th symbol of this string is described record type.
   *
   * @param marcRecord json record
   * @return a record type.
   * @see MarcRecordType
   */
  public MarcRecordType process(JsonObject marcRecord) {
    final char recordTypeCode = getRecordTypeCode(marcRecord);
    return getMarcRecordType(recordTypeCode);
  }

  private char getRecordTypeCode(JsonObject marcRecord) {
    try {
      return isMarcRecord(marcRecord) ? marcRecord.getString(LEADER_KEY).charAt(RECORD_TYPE_INDEX)
                                      : Character.MIN_VALUE;
    } catch (Exception e) {
      LOGGER.error("Can`t get a record type character form the leader: {}", marcRecord.getString(LEADER_KEY), e);
      return Character.MIN_VALUE;
    }
  }

  private boolean isMarcRecord(JsonObject marcRecord) {
    return Objects.nonNull(marcRecord) && marcRecord.containsKey(LEADER_KEY);
  }

  private MarcRecordType getMarcRecordType(char recordTypeCode) {
    if (BIB_CODES.contains(recordTypeCode)) {
      return MarcRecordType.BIB;
    } else if (HOLDING_CODES.contains(recordTypeCode)) {
      return MarcRecordType.HOLDING;
    } else if (AUTHORITY_CODES.contains(recordTypeCode)) {
      return MarcRecordType.AUTHORITY;
    }

    return MarcRecordType.NA;
  }
}
