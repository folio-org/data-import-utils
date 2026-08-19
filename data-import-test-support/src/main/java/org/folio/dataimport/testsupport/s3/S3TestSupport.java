package org.folio.dataimport.testsupport.s3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared LocalStack S3 Testcontainer bootstrap for DataImport integration tests.
 *
 * <p>Starts a single LocalStack container with the S3 service, publishes the S3 endpoint address
 * through the {@code S3.*} system properties that FOLIO modules read on start-up, and builds
 * {@link FolioS3Client} instances for use in test setup and cleanup.
 */
public final class S3TestSupport implements AutoCloseable {

  public static final String S3_ENDPOINT_PROPERTY = "S3_URL";
  public static final String S3_REGION_PROPERTY = "S3_REGION";
  public static final String S3_ACCESS_KEY_PROPERTY = "S3_ACCESS_KEY_ID";
  public static final String S3_SECRET_KEY_PROPERTY = "S3_SECRET_ACCESS_KEY";
  public static final String S3_BUCKET_PROPERTY = "S3_BUCKET";
  public static final String S3_AWS_SDK_PROPERTY = "S3_IS_AWS";
  public static final String S3_SUBPATH_PROPERTY = "S3_SUB_PATH";
  public static final String DEFAULT_IMAGE = "localstack/localstack:s3-community-archive";

  private static final Logger LOGGER = LogManager.getLogger();

  private final LocalStackContainer container;
  private final String bucket;
  private final String subPath;

  /**
   * Creates a support instance backed by the default LocalStack image.
   *
   * @param bucket  the S3 bucket name used by the module under test
   * @param subPath the S3 sub-path used by the module under test
   */
  public S3TestSupport(String bucket, String subPath) {
    this(DEFAULT_IMAGE, bucket, subPath);
  }

  /**
   * Creates a support instance backed by a custom LocalStack image.
   *
   * @param dockerImage the LocalStack Docker image, e.g. {@code localstack/localstack:latest}
   * @param bucket      the S3 bucket name used by the module under test
   * @param subPath     the S3 sub-path used by the module under test
   */
  @SuppressWarnings("java:S2095")
  public S3TestSupport(String dockerImage, String bucket, String subPath) {
    this.container = new LocalStackContainer(DockerImageName.parse(dockerImage))
      .withServices(LocalStackContainer.Service.S3);
    this.bucket = bucket;
    this.subPath = subPath;
  }

  /**
   * Starts the LocalStack container and publishes all {@code S3.*} system properties so modules
   * that read their S3 configuration from system properties pick up the test endpoint automatically.
   */
  public void start() {
    LOGGER.info("start:: Starting shared S3/LocalStack test container");
    container.start();
    System.setProperty(S3_ENDPOINT_PROPERTY, container.getEndpoint().toString());
    System.setProperty(S3_REGION_PROPERTY, container.getRegion());
    System.setProperty(S3_ACCESS_KEY_PROPERTY, container.getAccessKey());
    System.setProperty(S3_SECRET_KEY_PROPERTY, container.getSecretKey());
    System.setProperty(S3_BUCKET_PROPERTY, bucket);
    System.setProperty(S3_AWS_SDK_PROPERTY, "false");
    System.setProperty(S3_SUBPATH_PROPERTY, subPath);
  }

  /**
   * Stops the LocalStack container and clears all {@code S3.*} system properties.
   */
  public void stop() {
    LOGGER.info("stop:: Stopping shared S3/LocalStack test container");
    System.clearProperty(S3_ENDPOINT_PROPERTY);
    System.clearProperty(S3_REGION_PROPERTY);
    System.clearProperty(S3_ACCESS_KEY_PROPERTY);
    System.clearProperty(S3_SECRET_KEY_PROPERTY);
    System.clearProperty(S3_BUCKET_PROPERTY);
    System.clearProperty(S3_AWS_SDK_PROPERTY);
    System.clearProperty(S3_SUBPATH_PROPERTY);
    container.close();
  }

  @Override
  public void close() {
    stop();
  }

  /**
   * Returns the S3 endpoint URL of the running container.
   *
   * @return the endpoint URL string
   */
  public String getEndpoint() {
    return container.getEndpoint().toString();
  }

  /**
   * Returns the AWS region of the running container.
   *
   * @return the region string
   */
  public String getRegion() {
    return container.getRegion();
  }

  /**
   * Returns the AWS access key of the running container.
   *
   * @return the access key
   */
  public String getAccessKey() {
    return container.getAccessKey();
  }

  /**
   * Returns the AWS secret key of the running container.
   *
   * @return the secret key
   */
  public String getSecretKey() {
    return container.getSecretKey();
  }

  /**
   * Returns the bucket name this support instance was configured with.
   *
   * @return the bucket name
   */
  public String getBucket() {
    return bucket;
  }

  /**
   * Returns the sub-path this support instance was configured with.
   *
   * @return the sub-path
   */
  public String getSubPath() {
    return subPath;
  }

  /**
   * Builds a {@link FolioS3Client} pointed at the running LocalStack container using the bucket
   * and sub-path this instance was constructed with.
   *
   * @return a ready-to-use S3 client
   */
  public FolioS3Client buildS3Client() {
    return S3ClientFactory.getS3Client(
      S3ClientProperties.builder()
        .endpoint(getEndpoint())
        .accessKey(getAccessKey())
        .secretKey(getSecretKey())
        .bucket(bucket)
        .awsSdk(false)
        .region(getRegion())
        .subPath(subPath)
        .build()
    );
  }
}
