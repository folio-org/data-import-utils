package org.folio.dataimport.testsupport.s3;

import java.util.concurrent.atomic.AtomicReference;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * JUnit 5 extension that boots a single shared LocalStack S3 Testcontainer for the whole test run.
 *
 * <p>Register it as a {@code static} field annotated with {@code @RegisterExtension} and build an
 * S3 client through {@link #buildS3Client()}. Because {@link S3TestSupport} publishes the S3
 * endpoint through JVM-wide system properties, a single container is started per JVM no matter how
 * many test classes register the extension.
 *
 * <pre>{@code
 * @RegisterExtension
 * protected static final S3Extension S3 = new S3Extension("my-bucket", "my-module");
 * }</pre>
 */
public class S3Extension implements BeforeAllCallback {

  private static final Namespace NAMESPACE = Namespace.create(S3Extension.class);
  private static final String RESOURCE_KEY = "s3-container";
  private static final AtomicReference<S3TestSupport> RUNNING = new AtomicReference<>();

  private final String dockerImage;
  private final String bucket;
  private final String subPath;

  /**
   * Creates an extension backed by the default LocalStack image.
   *
   * @param bucket  the S3 bucket name used by the module under test
   * @param subPath the S3 sub-path used by the module under test
   */
  public S3Extension(String bucket, String subPath) {
    this(S3TestSupport.DEFAULT_IMAGE, bucket, subPath);
  }

  /**
   * Creates an extension backed by a custom LocalStack image.
   *
   * @param dockerImage the LocalStack Docker image, e.g. {@code localstack/localstack:latest}
   * @param bucket      the S3 bucket name used by the module under test
   * @param subPath     the S3 sub-path used by the module under test
   */
  public S3Extension(String dockerImage, String bucket, String subPath) {
    this.dockerImage = dockerImage;
    this.bucket = bucket;
    this.subPath = subPath;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    var store = context.getRoot().getStore(NAMESPACE);
    store.computeIfAbsent(RESOURCE_KEY, key -> new S3Resource(dockerImage, bucket, subPath));
  }

  /**
   * Returns the running S3 harness.
   *
   * @return the shared {@link S3TestSupport}
   */
  public S3TestSupport getSupport() {
    var support = RUNNING.get();
    if (support == null) {
      throw new IllegalStateException(
        "S3 container is not started; register S3Extension with @RegisterExtension");
    }
    return support;
  }

  /**
   * Builds a {@link FolioS3Client} pointed at the running LocalStack container using the bucket
   * and sub-path this extension was constructed with.
   *
   * @return a ready-to-use S3 client
   */
  public FolioS3Client buildS3Client() {
    return getSupport().buildS3Client();
  }

  private static final class S3Resource implements AutoCloseable {

    private final S3TestSupport support;

    S3Resource(String dockerImage, String bucket, String subPath) {
      support = new S3TestSupport(dockerImage, bucket, subPath);
      support.start();
      RUNNING.set(support);
    }

    @Override
    public void close() {
      support.stop();
      RUNNING.set(null);
    }
  }
}
