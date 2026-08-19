package org.folio.dataimport.testsupport.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class S3ExtensionIT {

  @RegisterExtension
  static final S3Extension S3 = new S3Extension("test-bucket", "test-path");

  @DisplayName("should build a non-null FolioS3Client when the extension is active")
  @Test
  void shouldBuildNonNullS3Client_whenExtensionIsActive() {
    // act
    FolioS3Client client = S3.buildS3Client();

    // assert
    assertThat(client).isNotNull();
  }

  @DisplayName("should return the same S3TestSupport instance on repeated getSupport calls")
  @Test
  void shouldReturnSameInstance_whenGetSupportCalledRepeatedly() {
    assertThat(S3.getSupport()).isSameAs(S3.getSupport());
  }

  @DisplayName("should publish the S3 endpoint through the S3_URL system property when the extension is active")
  @Test
  void shouldPublishEndpointThroughSystemProperty_whenExtensionIsActive() {
    assertThat(System.getProperty(S3TestSupport.S3_ENDPOINT_PROPERTY))
      .isNotBlank()
      .startsWith("http");
  }
}
