package org.folio.dataimport.util.marc;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.asList;

public class MarcRecordAnalyzer implements RecordAnalyzer {
  private static final Logger LOGGER = LogManager.getLogger();
  private static final String LEADER_KEY = "leader";
  private static final int RECORD_TYPE_INDEX = 5;
  private static final Set<Character> BIB_CODES = new HashSet<>(asList('a', 'c', 'd', 'e', 'f', 'g', 'i', 'j', 'k', 'm', 'o', 'p', 'r', 't'));
  private static final Set<Character> HOLDING_CODES = new HashSet<>(asList('u', 'v', 'x', 'y'));
  private static final Set<Character> AUTHORITY_CODES = new HashSet<>(Collections.singletonList('z'));

  /**
   * Processes a json object to determine type of record BIB, HOLDING, AUTHORITY, NA.
   * <br>
   * <br>
   * Note. We assume that a json object with the field "leader" is MARC record.
   * Field "leader" contains a string and the 6th symbol of this string is described record type.
   *
   * @param record json record
   * @param <T>    enum that describes a record type.
   * @return a record type.
   * @see MarcRecordType
   */
  public <T> T process(JsonObject record) {
    final char recordTypeCode = getRecordTypeCode(record);
    return (T) getMarcRecordType(recordTypeCode);//NOSONAR
  }

  private char getRecordTypeCode(JsonObject record) {
    try {
      return isMarcRecord(record) ? record.getString(LEADER_KEY).charAt(RECORD_TYPE_INDEX) : Character.MIN_VALUE;
    } catch (Exception e) {
      LOGGER.error("Can`t get a record type character form the leader: {}",record.getString(LEADER_KEY), e );
      return Character.MIN_VALUE;
    }
  }

  private boolean isMarcRecord(JsonObject record) {
    return Objects.nonNull(record) && record.containsKey(LEADER_KEY);
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
