package org.folio.dataimport.util.marc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.marc4j.marc.Record;

class MarcRecordEditorTest {

  private static final String MARC_CONTENT = """
    {
      "leader": "00000nam a2200000 a 4500",
      "fields": [
        {
          "001": "123456"
        },
        {
          "008": "260907s2026    xx            000 0 eng d"
        },
        {
          "100": {
            "ind1": "1",
            "ind2": " ",
            "subfields": [
              {
                "a": "Doe, John"
              }
            ]
          }
        },
        {
          "245": {
            "ind1": "1",
            "ind2": "0",
            "subfields": [
              {
                "a": "Test title"
              },
              {
                "b": "subtitle"
              }
            ]
          }
        },
        {
          "650": {
            "ind1": " ",
            "ind2": "0",
            "subfields": [
              {
                "a": "Testing"
              },
              {
                "a": "MARC"
              }
            ]
          }
        }
      ]
    }
    """;

  @Test
  void shouldReturnNullForNullHolder() {
    assertNull(MarcRecordEditor.computeMarcRecord(null));
  }

  @NullAndEmptySource
  @ParameterizedTest
  void shouldReturnNullForNullContent(String content) {
    var holder = holder(content);

    assertNull(MarcRecordEditor.computeMarcRecord(holder));
  }

  @Test
  void shouldParseMarcContent() {
    var holder = holder(MARC_CONTENT);

    var marcRecord = MarcRecordEditor.computeMarcRecord(holder);

    assertNotNull(marcRecord);
    assertEquals("123456", marcRecord.getControlNumber());
  }

  @Test
  void shouldReturnSameRecordForSameContentFromCache() {
    var holder1 = holder(MARC_CONTENT);
    var holder2 = holder(new JsonObject(MARC_CONTENT));

    Record record1 = MarcRecordEditor.computeMarcRecord(holder1);
    Record record2 = MarcRecordEditor.computeMarcRecord(holder2);

    assertNotNull(record1);
    assertNotNull(record2);
    assertSame(record1, record2);
  }

  @Test
  void shouldReturnNullForInvalidContent() {
    var holder = holder("{ invalid json");

    assertNull(MarcRecordEditor.computeMarcRecord(holder));
  }

  @Test
  void shouldRecalculateAndWriteBack() {
    var holder = holder(MARC_CONTENT);
    var marcRecord = MarcRecordEditor.computeMarcRecord(holder);

    assertNotNull(marcRecord);

    var result = MarcRecordEditor.recalculateAndWriteBack(holder, marcRecord);

    assertTrue(result);
    assertNotNull(holder.getMarcContent());
    assertFalse(holder.getMarcContent().toString().isBlank());

    var updatedRecord = MarcRecordEditor.computeMarcRecord(holder);

    assertNotNull(updatedRecord);
    assertEquals("123456", updatedRecord.getControlNumber());
  }

  @Test
  void shouldAddSubfieldToExistingField() {
    var holder = holder(MARC_CONTENT);

    var result = MarcRecordEditor.addFieldToMarcRecord(
      holder, "245", 'c', "John Doe"
    );

    assertTrue(result);

    assertEquals(
      Optional.of("John Doe"),
      MarcRecordEditor.getValueFromDataField(holder, "245", 'c')
    );
  }

  @Test
  void shouldReturnFalseWhenAddingFieldToMissingContent() {
    var holder = holder(null);

    assertFalse(
      MarcRecordEditor.addFieldToMarcRecord(holder, "245", 'c', "value")
    );
  }

  @Test
  void shouldAddControlledField() {
    var holder = holder(MARC_CONTENT);

    var result = MarcRecordEditor.addControlledFieldToMarcRecord(holder, "005", "123456789", false);

    assertTrue(result);
    assertEquals("123456789", MarcRecordEditor.getValueFromControlledField(holder, "005"));
  }

  @Test
  void shouldReplaceControlledFieldWhenReplaceIsTrue() {
    var holder = holder(MARC_CONTENT);

    assertTrue(
      MarcRecordEditor.addControlledFieldToMarcRecord(
        holder, "001", "NEW-ID", true
      )
    );

    assertEquals("NEW-ID", MarcRecordEditor.getValueFromControlledField(holder, "001"));
  }

  @Test
  void shouldThrowWhenAddingControlledFieldToNullContent() {
    var holder = holder(null);

    var exception = assertThrows(
      MarcContentException.class,
      () -> MarcRecordEditor.addControlledFieldToMarcRecordOrThrow(
        holder, "001", "value", false
      )
    );

    assertEquals(
      "Cannot add controlled field '001' to record 'test-id': "
      + "record, parsed record, or its content is null",
      exception.getMessage()
    );
  }

  @Test
  void shouldThrowWhenAddingControlledFieldToInvalidContent() {
    var holder = holder("{ invalid json");

    var exception = assertThrows(
      MarcContentException.class,
      () -> MarcRecordEditor.addControlledFieldToMarcRecordOrThrow(
        holder, "001", "value", false
      )
    );

    assertEquals(
      "Cannot add controlled field '001' to record 'test-id': "
      + "failed to parse the parsed record content",
      exception.getMessage()
    );
  }

  @Test
  void shouldReturnFalseInsteadOfThrowingForInvalidControlledFieldOperation() {
    var holder = holder("{ invalid json");

    assertFalse(
      MarcRecordEditor.addControlledFieldToMarcRecord(
        holder, "001", "value", false
      )
    );
  }

  @Test
  void shouldReadControlledField() {
    var holder = holder(MARC_CONTENT);

    assertEquals(
      "123456",
      MarcRecordEditor.getValueFromControlledField(holder, "001")
    );
  }

  @Test
  void shouldReturnNullWhenControlledFieldDoesNotExist() {
    var holder = holder(MARC_CONTENT);

    assertNull(
      MarcRecordEditor.getValueFromControlledField(holder, "003")
    );
  }

  @Test
  void shouldReadDataFieldByIndicatorsAndSubfield() {
    var holder = holder(MARC_CONTENT);

    assertEquals(
      Optional.of("Test title"),
      MarcRecordEditor.getValueFromDataField(
        holder, "245", '1', '0', 'a'
      )
    );
  }

  @Test
  void shouldReadDataFieldBySubfield() {
    var holder = holder(MARC_CONTENT);

    assertEquals(
      Optional.of("subtitle"),
      MarcRecordEditor.getValueFromDataField(holder, "245", 'b')
    );
  }

  @Test
  void shouldReturnEmptyWhenDataFieldDoesNotExist() {
    var holder = holder(MARC_CONTENT);

    assertEquals(
      Optional.empty(),
      MarcRecordEditor.getValueFromDataField(holder, "700", 'a')
    );
  }

  @Test
  void shouldRejectControlFieldWhenReadingDataField() {
    var holder = holder(MARC_CONTENT);

    assertThrows(
      IllegalArgumentException.class,
      () -> MarcRecordEditor.getValueFromDataField(holder, "001", 'a')
    );
  }

  @Test
  void shouldRemoveFirstField() {
    var holder = holder(MARC_CONTENT);

    assertTrue(
      MarcRecordEditor.removeField(holder, "650")
    );

    assertFalse(
      MarcRecordEditor.isFieldExist(holder, "650", 'a', "Testing")
    );
  }

  @Test
  void shouldRemoveFieldWithMatchingSubfieldValue() {
    var holder = holder(MARC_CONTENT);

    assertTrue(MarcRecordEditor.removeField(holder, "650", 'a', "Testing"));
    assertFalse(MarcRecordEditor.isFieldExist(holder, "650", 'a', "Testing"));
  }

  @Test
  void shouldReturnFalseWhenRemovingMissingField() {
    var holder = holder(MARC_CONTENT);

    assertFalse(MarcRecordEditor.removeField(holder, "999"));
  }

  @Test
  void shouldAddDataField() {
    var holder = holder(MARC_CONTENT);

    var result = MarcRecordEditor.addDataFieldToMarcRecord(
      holder, "500", ' ', ' ', 'a', "A note"
    );

    assertTrue(result);

    assertEquals(
      Optional.of("A note"),
      MarcRecordEditor.getValueFromDataField(holder, "500", 'a')
    );
  }

  @Test
  void shouldCheckFieldExistence() {
    var holder = holder(MARC_CONTENT);

    assertTrue(MarcRecordEditor.isFieldExist(holder, "245", 'a', "Test title"));
    assertFalse(MarcRecordEditor.isFieldExist(holder, "245", 'a', "Unknown title"));
  }

  @Test
  void shouldReturnFalseWhenFieldExistenceValueIsNull() {
    var holder = holder(MARC_CONTENT);

    assertFalse(MarcRecordEditor.isFieldExist(holder, "245", 'a', null));
  }

  @Test
  void shouldRemoveAllFieldsWithTag() {
    var holder = holder(MARC_CONTENT);

    assertTrue(MarcRecordEditor.removeFieldFromMarcRecord(holder, "650"));

    assertFalse(MarcRecordEditor.isFieldExist(holder, "650", 'a', "Testing"));
  }

  @Test
  void shouldReturnFalseWhenRemovingAllFieldsWithMissingTag() {
    var holder = holder(MARC_CONTENT);

    assertFalse(MarcRecordEditor.removeFieldFromMarcRecord(holder, "999"));
  }

  @Test
  void shouldRemoveSubfieldsContainingValues() {
    var holder = holder(MARC_CONTENT);

    MarcRecordEditor.removeSubfieldsThatContainsValues(
      holder,
      List.of("650"),
      'a',
      List.of("Testing")
    );

    assertFalse(MarcRecordEditor.isFieldExist(holder, "650", 'a', "Testing"));

    assertTrue(MarcRecordEditor.isFieldExist(holder, "650", 'a', "MARC"));
  }

  @Test
  void shouldCheckSubfieldExistence() {
    var holder = holder(MARC_CONTENT);

    assertTrue(MarcRecordEditor.isSubfieldExist(holder, 'a'));

    assertFalse(MarcRecordEditor.isSubfieldExist(holder, 'z'));
  }

  @Test
  void shouldReturnCacheStatistics() {
    var before = MarcRecordEditor.getCacheStats();

    var holder = holder(MARC_CONTENT);

    MarcRecordEditor.computeMarcRecord(holder);
    MarcRecordEditor.computeMarcRecord(holder);

    var after = MarcRecordEditor.getCacheStats();
    var delta = after.minus(before);

    assertTrue(delta.requestCount() >= 2);
    assertTrue(delta.hitCount() >= 1);
    assertTrue(delta.missCount() >= 1);
    assertTrue(delta.loadCount() >= 1);
  }

  private static TestMarcContentHolder holder(Object content) {
    return new TestMarcContentHolder(content);
  }

  private static final class TestMarcContentHolder implements MarcContentHolder {

    private Object content;

    private TestMarcContentHolder(Object content) {
      this.content = content;
    }

    @Override
    public Object getMarcContent() {
      return content;
    }

    @Override
    public void setMarcContent(String content) {
      this.content = content;
    }

    @Override
    public String getRecordId() {
      return "test-id";
    }
  }
}

