package org.folio.dataimport.util.marc;

import io.vertx.core.json.JsonObject;

public interface RecordAnalyzer {
  MarcRecordType process(JsonObject marcRecord);
}
