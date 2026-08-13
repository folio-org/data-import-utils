package org.folio.dataimport.testsupport.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.folio.okapi.common.XOkapiHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FolioHeadersTest {

  @Test
  void shouldBuildHeaderMapWithProvidedValues() {
    Map<String, String> headers = FolioHeaders.builder()
      .url("http://localhost:9130")
      .tenant("diku")
      .token("test-token")
      .userId("00000000-0000-0000-0000-000000000001")
      .requestId("req-1")
      .build();

    assertEquals("http://localhost:9130", headers.get(XOkapiHeaders.URL));
    assertEquals("diku", headers.get(XOkapiHeaders.TENANT));
    assertEquals("test-token", headers.get(XOkapiHeaders.TOKEN));
    assertEquals("00000000-0000-0000-0000-000000000001", headers.get(XOkapiHeaders.USER_ID));
    assertEquals("req-1", headers.get(XOkapiHeaders.REQUEST_ID));
  }

  @DisplayName("should build empty header map when no headers are set")
  @Test
  void shouldBuildEmptyHeaderMap_whenNoHeadersAreSet() {
    // act
    Map<String, String> headers = FolioHeaders.builder().build();

    // assert
    assertThat(headers).isEmpty();
  }

  @DisplayName("should include only set headers when partial build is performed")
  @Test
  void shouldIncludeOnlySetHeaders_whenPartialBuildIsPerformed() {
    // act
    Map<String, String> headers = FolioHeaders.builder()
      .tenant("test-tenant")
      .token("test-token")
      .build();

    // assert
    assertThat(headers).containsOnlyKeys(XOkapiHeaders.TENANT, XOkapiHeaders.TOKEN);
  }

  @DisplayName("should return immutable map when build is called")
  @Test
  void shouldReturnImmutableMap_whenBuildIsCalled() {
    // arrange
    Map<String, String> headers = FolioHeaders.builder().tenant("test-tenant").build();

    // assert
    assertThatThrownBy(() -> headers.put("extra", "value"))
      .isInstanceOf(UnsupportedOperationException.class);
  }
}
