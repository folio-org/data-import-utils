package org.folio.dataimport.util;

/**
 * Keys of the data import event payload context ({@code DataImportEventPayload.getContext()})
 * that are shared between data import modules.
 *
 * <p>For Kafka record headers see {@link DataImportHeaders}.
 */
public final class DataImportContextKeys {

  /** Id of the job execution, put into reply event payloads that leave the data import flow. */
  public static final String JOB_EXECUTION_ID = "JOB_EXECUTION_ID";
  /** Id of the job profile snapshot used to process the event. */
  public static final String JOB_PROFILE_SNAPSHOT_ID = "JOB_PROFILE_SNAPSHOT_ID";
  /** Id of the incoming record being imported. */
  public static final String INCOMING_RECORD_ID = "INCOMING_RECORD_ID";
  /** Error message of a failed event (DI_ERROR). */
  public static final String ERROR = "ERROR";

  /** Id of the central tenant in an Enhanced Consortia Support (ECS) environment. */
  public static final String CENTRAL_TENANT_ID = "CENTRAL_TENANT_ID";
  /** Flag set to "true" when a shared instance was updated in the central tenant. */
  public static final String CENTRAL_TENANT_INSTANCE_UPDATED = "CENTRAL_TENANT_INSTANCE_UPDATED";

  /** Serialized mapping parameters snapshot. */
  public static final String MAPPING_PARAMS = "MAPPING_PARAMS";
  /** Serialized mapping rules. */
  public static final String MAPPING_RULES = "MAPPING_RULES";

  /** Ids of the entities found when a match profile matches multiple entities. */
  public static final String MULTI_MATCH_IDS = "MULTI_MATCH_IDS";
  /** Set by the matcher when the current match profile did not find a match. */
  public static final String NOT_MATCHED_NUMBER = "NOT_MATCHED_NUMBER";
  /** Relations between match fields and mapping parameters used to resolve ids from names/codes. */
  public static final String MATCHING_PARAMETERS_RELATIONS = "MATCHING_PARAMETERS_RELATIONS";

  /** Flag ("true" or "false") telling whether a MARC bib record was created for the instance. */
  public static final String MARC_BIB_RECORD_CREATED = "MARC_BIB_RECORD_CREATED";
  /** Id of the MARC authority record. */
  public static final String AUTHORITY_RECORD_ID = "AUTHORITY_RECORD_ID";

  /** Serialized permanent locations of the mapped holdings. */
  public static final String HOLDINGS_IDENTIFIERS = "HOLDINGS_IDENTIFIERS";
  /** MARC field used to map multiple holdings from one record. */
  public static final String MULTIPLE_HOLDINGS_FIELD = "MULTIPLE_HOLDINGS_FIELD";

  /** Number of the current retry of an update attempt after an optimistic locking error. */
  public static final String CURRENT_RETRY_NUMBER = "CURRENT_RETRY_NUMBER";
  /** Intermediate results kept between retries of an update after an optimistic locking error. */
  public static final String OL_ACCUMULATIVE_RESULTS = "OL_ACCUMULATIVE_RESULTS";

  private static final String MATCHED_PREFIX = "MATCHED_";

  private DataImportContextKeys() {
    /* This utility class should not be instantiated */
  }

  /**
   * Returns the key under which the entity matched by a match profile is stored.
   *
   * @param entityType entity type value, e.g. {@code MARC_BIBLIOGRAPHIC}
   * @return context key, e.g. {@code MATCHED_MARC_BIBLIOGRAPHIC}
   */
  public static String matchedEntityKey(String entityType) {
    return MATCHED_PREFIX + entityType;
  }
}
