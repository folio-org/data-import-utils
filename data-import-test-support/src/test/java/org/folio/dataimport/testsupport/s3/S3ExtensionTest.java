package org.folio.dataimport.testsupport.s3;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3ExtensionTest {

  @DisplayName("should throw when getSupport is called before the extension is registered")
  @Test
  void shouldThrow_whenGetSupportCalledBeforeRegistration() {
    // arrange
    var extension = new S3Extension("test-bucket", "test-path");

    // assert
    assertThatThrownBy(extension::getSupport)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("not started");
  }

  @DisplayName("should throw when buildS3Client is called before the extension is registered")
  @Test
  void shouldThrow_whenBuildS3ClientCalledBeforeRegistration() {
    // arrange
    var extension = new S3Extension("test-bucket", "test-path");

    // assert
    assertThatThrownBy(extension::buildS3Client)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("not started");
  }
}
