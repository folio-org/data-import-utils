package org.folio.dataimport.testsupport.kafka;

import java.util.concurrent.atomic.AtomicReference;
import org.folio.kafka.KafkaConfig;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * JUnit 5 extension that boots a single shared Kafka Testcontainer for the whole test run.
 *
 * <p>Register it as a {@code static} field annotated with {@code @RegisterExtension} and read the
 * broker coordinates through {@link #kafkaConfig(String)} or {@link #getBootstrapServers()}. Because
 * {@link KafkaTestSupport} publishes the broker address through JVM-wide system properties, a single
 * broker is started per JVM no matter how many test classes register the extension.
 */
public class KafkaExtension implements BeforeAllCallback {

  private static final Namespace NAMESPACE = Namespace.create(KafkaExtension.class);
  private static final String RESOURCE_KEY = "kafka-container";
  private static final AtomicReference<KafkaTestSupport> RUNNING = new AtomicReference<>();

  private final String dockerImage;

  /**
   * Creates an extension backed by the default Kafka image.
   */
  public KafkaExtension() {
    this(null);
  }

  /**
   * Creates an extension backed by a custom Kafka image.
   *
   * @param dockerImage the Kafka Docker image, e.g. {@code apache/kafka-native:4.2.0}
   */
  public KafkaExtension(String dockerImage) {
    this.dockerImage = dockerImage;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    var store = context.getRoot().getStore(NAMESPACE);
    store.computeIfAbsent(RESOURCE_KEY, key -> new KafkaResource(dockerImage));
  }

  /**
   * Returns the running Kafka harness.
   *
   * @return the shared {@link KafkaTestSupport}
   */
  public KafkaTestSupport getSupport() {
    var support = RUNNING.get();
    if (support == null) {
      throw new IllegalStateException(
        "Kafka container is not started; register KafkaExtension with @RegisterExtension");
    }
    return support;
  }

  /**
   * Returns the {@code host:port} bootstrap servers of the running broker.
   *
   * @return the bootstrap servers connection string
   */
  public String getBootstrapServers() {
    return getSupport().getBootstrapServers();
  }

  /**
   * Builds a FOLIO {@link KafkaConfig} pointed at the running broker.
   *
   * @param envId the FOLIO environment identifier
   * @return a ready-to-use configuration
   */
  public KafkaConfig kafkaConfig(String envId) {
    return getSupport().kafkaConfig(envId);
  }

  private static final class KafkaResource implements AutoCloseable {

    private final KafkaTestSupport support;

    KafkaResource(String dockerImage) {
      support = dockerImage == null ? new KafkaTestSupport() : new KafkaTestSupport(dockerImage);
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
