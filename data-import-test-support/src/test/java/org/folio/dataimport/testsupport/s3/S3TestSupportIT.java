package org.folio.dataimport.testsupport.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3TestSupportIT {

  private S3TestSupport support;

  @BeforeEach
  void setUp() {
    support = new S3TestSupport("test-bucket", "test-path");
  }

  @AfterEach
  void tearDown() {
    support.stop();
  }

  @DisplayName("should set all S3.* system properties after start")
  @Test
  void shouldSetAllS3SystemProperties_afterStart() {
    // act
    support.start();

    // assert
    assertThat(System.getProperty(S3TestSupport.S3_ENDPOINT_PROPERTY)).isNotBlank();
    assertThat(System.getProperty(S3TestSupport.S3_REGION_PROPERTY)).isNotBlank();
    assertThat(System.getProperty(S3TestSupport.S3_ACCESS_KEY_PROPERTY)).isNotBlank();
    assertThat(System.getProperty(S3TestSupport.S3_SECRET_KEY_PROPERTY)).isNotBlank();
    assertThat(System.getProperty(S3TestSupport.S3_BUCKET_PROPERTY)).isNotBlank();
    assertThat(System.getProperty(S3TestSupport.S3_AWS_SDK_PROPERTY)).isNotBlank();
    assertThat(System.getProperty(S3TestSupport.S3_SUBPATH_PROPERTY)).isNotBlank();
  }

  @DisplayName("should clear all S3.* system properties after stop")
  @Test
  void shouldClearAllS3SystemProperties_afterStop() {
    // arrange
    support.start();

    // act
    support.stop();

    // assert
    assertThat(System.getProperty(S3TestSupport.S3_ENDPOINT_PROPERTY)).isNull();
    assertThat(System.getProperty(S3TestSupport.S3_REGION_PROPERTY)).isNull();
    assertThat(System.getProperty(S3TestSupport.S3_ACCESS_KEY_PROPERTY)).isNull();
    assertThat(System.getProperty(S3TestSupport.S3_SECRET_KEY_PROPERTY)).isNull();
    assertThat(System.getProperty(S3TestSupport.S3_BUCKET_PROPERTY)).isNull();
    assertThat(System.getProperty(S3TestSupport.S3_AWS_SDK_PROPERTY)).isNull();
    assertThat(System.getProperty(S3TestSupport.S3_SUBPATH_PROPERTY)).isNull();
  }

  @DisplayName("should return a non-blank HTTP endpoint after start")
  @Test
  void shouldReturnNonBlankEndpoint_afterStart() {
    // act
    support.start();

    // assert
    assertThat(support.getEndpoint()).isNotBlank().startsWith("http");
  }

  @DisplayName("should return a non-blank region after start")
  @Test
  void shouldReturnNonBlankRegion_afterStart() {
    // act
    support.start();

    // assert
    assertThat(support.getRegion()).isNotBlank();
  }

  @DisplayName("should return a non-blank access key after start")
  @Test
  void shouldReturnNonBlankAccessKey_afterStart() {
    // act
    support.start();

    // assert
    assertThat(support.getAccessKey()).isNotBlank();
  }

  @DisplayName("should return a non-blank secret key after start")
  @Test
  void shouldReturnNonBlankSecretKey_afterStart() {
    // act
    support.start();

    // assert
    assertThat(support.getSecretKey()).isNotBlank();
  }

  @DisplayName("should build a non-null FolioS3Client after start")
  @Test
  void shouldBuildNonNullS3Client_afterStart() {
    // arrange
    support.start();

    // act
    FolioS3Client client = support.buildS3Client();

    // assert
    assertThat(client).isNotNull();
  }
}
