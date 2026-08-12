package org.folio.dataimport.util.marc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

class MarcRecordAnalyzerTest {

  private final RecordAnalyzer analyzer = new MarcRecordAnalyzer();

  @Test
  void process_Bib_Ok() {
    var marcRecord = new JsonObject("{\"leader\":\"13112cam a2200553Ii 4500\"}");
    var result = analyzer.process(marcRecord);
    assertEquals(MarcRecordType.BIB, result);
  }

  @Test
  void process_Holding_Ok() {
    var marcRecord = new JsonObject("{\"leader\":\"13112cum a2200553Ii 4500\"}");
    var result = analyzer.process(marcRecord);
    assertEquals(MarcRecordType.HOLDING, result);
  }

  @Test
  void process_Authority_Ok() {
    var marcRecord = new JsonObject("{\"leader\":\"13112czm a2200553Ii 4500\"}");
    var result = analyzer.process(marcRecord);
    assertEquals(MarcRecordType.AUTHORITY, result);
  }

  @Test
  void process_Json_leaderKey_absent() {
    var marcRecord = new JsonObject("{}");
    var result = analyzer.process(marcRecord);
    assertEquals(MarcRecordType.NA, result);
  }

  @Test
  void process_Json_leaderValue_hasNotRecordTypeSymbol() {
    var marcRecord = new JsonObject("{\"leader\":\"13112\"}");
    var result = analyzer.process(marcRecord);
    assertEquals(MarcRecordType.NA, result);
  }
}
