package org.folio.dataimport.testsupport.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3TestSupportTest {

  @DisplayName("should expose 'S3_URL' as the endpoint system property name")
  @Test
  void shouldExposeS3EndpointAsSystemPropertyName() {
    assertThat(S3TestSupport.S3_ENDPOINT_PROPERTY).isEqualTo("S3_URL");
  }

  @DisplayName("should expose 'S3_REGION' as the region system property name")
  @Test
  void shouldExposeS3RegionAsSystemPropertyName() {
    assertThat(S3TestSupport.S3_REGION_PROPERTY).isEqualTo("S3_REGION");
  }

  @DisplayName("should expose 'S3_ACCESS_KEY_ID' as the access key system property name")
  @Test
  void shouldExposeS3AccessKeyAsSystemPropertyName() {
    assertThat(S3TestSupport.S3_ACCESS_KEY_PROPERTY).isEqualTo("S3_ACCESS_KEY_ID");
  }

  @DisplayName("should expose 'S3_SECRET_ACCESS_KEY' as the secret key system property name")
  @Test
  void shouldExposeS3SecretKeyAsSystemPropertyName() {
    assertThat(S3TestSupport.S3_SECRET_KEY_PROPERTY).isEqualTo("S3_SECRET_ACCESS_KEY");
  }

  @DisplayName("should expose 'S3_BUCKET' as the bucket system property name")
  @Test
  void shouldExposeS3eBucketAsSystemPropertyName() {
    assertThat(S3TestSupport.S3_BUCKET_PROPERTY).isEqualTo("S3_BUCKET");
  }

  @DisplayName("should expose 'S3_IS_AWS' as the AWS SDK flag system property name")
  @Test
  void shouldExposeS3AwsSdkAsSystemPropertyName() {
    assertThat(S3TestSupport.S3_AWS_SDK_PROPERTY).isEqualTo("S3_IS_AWS");
  }

  @DisplayName("should expose 'S3_SUB_PATH' as the sub-path system property name")
  @Test
  void shouldExposeS3SubpathAsSystemPropertyName() {
    assertThat(S3TestSupport.S3_SUBPATH_PROPERTY).isEqualTo("S3_SUB_PATH");
  }

  @DisplayName("should expose the default LocalStack S3 docker image name")
  @Test
  void shouldExposeDefaultDockerImage() {
    assertThat(S3TestSupport.DEFAULT_IMAGE).isEqualTo("localstack/localstack:s3-community-archive");
  }

  @DisplayName("should return the bucket name passed to the constructor")
  @Test
  void shouldReturnConfiguredBucket() {
    // arrange
    var support = new S3TestSupport("my-bucket", "my-path");

    // assert
    assertThat(support.getBucket()).isEqualTo("my-bucket");
  }

  @DisplayName("should return the sub-path passed to the constructor")
  @Test
  void shouldReturnConfiguredSubPath() {
    // arrange
    var support = new S3TestSupport("my-bucket", "my-path");

    // assert
    assertThat(support.getSubPath()).isEqualTo("my-path");
  }
}
