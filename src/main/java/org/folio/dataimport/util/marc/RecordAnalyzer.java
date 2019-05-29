package org.folio.dataimport.util.marc;

import io.vertx.core.json.JsonObject;

public interface RecordAnalyzer {
  <T> T process(JsonObject record);
}
