package org.folio.dataimport.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataImportContextKeysTest {

  @DisplayName("should prefix entity type with MATCHED_ when building matched entity key")
  @Test
  void shouldPrefixEntityTypeWithMatched_whenBuildingMatchedEntityKey() {
    // act
    String key = DataImportContextKeys.matchedEntityKey("MARC_BIBLIOGRAPHIC");

    // assert
    assertThat(key).isEqualTo("MATCHED_MARC_BIBLIOGRAPHIC");
  }
}
