package org.folio.dataimport.testsupport.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseIntegrationTestDefaultsTest {

  @Test
  void shouldExposeSensibleTenantDefaults() {
    var base = new BaseIntegrationTest() {
      @Override
      protected String getModuleName() {
        return "mod-test-1.0.0";
      }
    };

    assertEquals("diku", base.getTenantId());
    assertNull(base.getToken());
    assertEquals("mod-test-1.0.0", base.getTenantAttributes().getModuleTo());
  }

  @DisplayName("should return empty map from getExtraSpecHeaders by default")
  @Test
  void shouldReturnEmptyMap_whenGetExtraSpecHeadersCalledByDefault() {
    // arrange
    var base = new BaseIntegrationTest() {
      @Override
      protected String getModuleName() {
        return "mod-test-1.0.0";
      }
    };

    // assert
    assertThat(base.getExtraSpecHeaders()).isEmpty();
  }
}
