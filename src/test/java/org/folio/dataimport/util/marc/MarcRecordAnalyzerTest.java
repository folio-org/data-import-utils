package org.folio.dataimport.util.marc;

import static org.junit.Assert.assertEquals;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class MarcRecordAnalyzerTest {

  private RecordAnalyzer analyzer = new MarcRecordAnalyzer();

  @Test
  public void process_BIB_OK() {
    JsonObject record = new JsonObject("{\"leader\":\"13112cam a2200553Ii 4500\"}");
    MarcRecordType result = analyzer.process(record);
    assertEquals(MarcRecordType.BIB, result);
  }

  @Test
  public void process_Holding_OK() {
    JsonObject record = new JsonObject("{\"leader\":\"13112uam a2200553Ii 4500\"}");
    MarcRecordType result = analyzer.process(record);
    assertEquals(MarcRecordType.HOLDING, result);
  }

  @Test
  public void process_Authority_OK() {
    JsonObject record = new JsonObject("{\"leader\":\"13112zam a2200553Ii 4500\"}");
    MarcRecordType result = analyzer.process(record);
    assertEquals(MarcRecordType.AUTHORITY, result);
  }

  @Test
  public void process_Json_leaderKey_absent() {
    JsonObject record = new JsonObject("{}");
    MarcRecordType result = analyzer.process(record);
    assertEquals(MarcRecordType.NA, result);
  }

  @Test
  public void process_Json_leaderValue_hasNotRecordTypeSymbol() {
    JsonObject record = new JsonObject("{\"leader\":\"13112\"}");
    MarcRecordType result = analyzer.process(record);
    assertEquals(MarcRecordType.NA, result);
  }
}
