package org.folio.dataimport.util;

/**
 * Keys of the data import event payload context ({@code DataImportEventPayload.getContext()})
 * that are shared between data import modules.
 *
 * <p>For Kafka record headers see {@link DataImportHeaders}.
 */
public final class DataImportContextKeys {

  /** Id of the job profile snapshot used to process the event. */
  public static final String JOB_PROFILE_SNAPSHOT_ID = "JOB_PROFILE_SNAPSHOT_ID";
  /** Id of the incoming record being imported. */
  public static final String INCOMING_RECORD_ID = "INCOMING_RECORD_ID";
  /** Id of the central tenant in an Enhanced Consortia Support (ECS) environment. */
  public static final String CENTRAL_TENANT_ID = "CENTRAL_TENANT_ID";
  /** Serialized mapping parameters snapshot. */
  public static final String MAPPING_PARAMS = "MAPPING_PARAMS";
  /** Serialized mapping rules. */
  public static final String MAPPING_RULES = "MAPPING_RULES";
  /** Ids of the entities found when a match profile matches multiple entities. */
  public static final String MULTI_MATCH_IDS = "MULTI_MATCH_IDS";
  /** Set by the matcher when the current match profile did not find a match. */
  public static final String NOT_MATCHED_NUMBER = "NOT_MATCHED_NUMBER";
  /** Number of the current retry of an update attempt after an optimistic locking error. */
  public static final String CURRENT_RETRY_NUMBER = "CURRENT_RETRY_NUMBER";

  private DataImportContextKeys() {
    /* This utility class should not be instantiated */
  }
}
