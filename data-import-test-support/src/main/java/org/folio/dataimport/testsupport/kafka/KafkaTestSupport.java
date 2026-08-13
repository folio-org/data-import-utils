package org.folio.dataimport.testsupport.kafka;

import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.KafkaTopicNameHelper;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Shared Apache Kafka Testcontainer bootstrap for DataImport integration tests.
 *
 * <p>Starts a single broker container, publishes its address through the {@code KAFKA_HOST} and
 * {@code KAFKA_PORT} system properties that FOLIO modules read on start-up, and builds FOLIO-style
 * {@link KafkaConfig} instances and topic names for use with {@link KafkaTestProducer} and
 * {@link KafkaTestConsumer}.
 */
public final class KafkaTestSupport implements AutoCloseable {

  public static final String KAFKA_HOST_PROPERTY = "KAFKA_HOST";
  public static final String KAFKA_PORT_PROPERTY = "KAFKA_PORT";
  public static final String DEFAULT_IMAGE = "apache/kafka-native:4.2.0";

  private static final Logger LOGGER = LogManager.getLogger();
  private static final Pattern URL_SCHEME_REPLACEMENT_PATTERN = Pattern.compile("^\\w+://");

  private final KafkaContainer container;

  public KafkaTestSupport() {
    this(DEFAULT_IMAGE);
  }

  @SuppressWarnings("java:S2095")
  public KafkaTestSupport(String dockerImage) {
    this.container = new KafkaContainer(dockerImage).withStartupAttempts(3);
  }

  /**
   * Formats a fully-qualified FOLIO topic name for the given environment, tenant and event type.
   *
   * @param envId     the FOLIO environment identifier
   * @param tenantId  the tenant identifier
   * @param eventType the DataImport event type, e.g. {@code DI_COMPLETED}
   * @return the fully-qualified topic name
   */
  public static String topicName(String envId, String tenantId, String eventType) {
    return KafkaTopicNameHelper.formatTopicName(envId, KafkaTopicNameHelper.getDefaultNameSpace(),
      tenantId, eventType);
  }

  /**
   * Starts the broker and publishes its address to the FOLIO Kafka system properties.
   */
  public void start() {
    LOGGER.info("start:: Starting shared Kafka test container");
    container.start();
    System.setProperty(KAFKA_HOST_PROPERTY, getHost());
    System.setProperty(KAFKA_PORT_PROPERTY, String.valueOf(getPort()));
  }

  /**
   * Stops the broker and clears the FOLIO Kafka system properties.
   */
  public void stop() {
    LOGGER.info("stop:: Stopping shared Kafka test container");
    System.clearProperty(KAFKA_HOST_PROPERTY);
    System.clearProperty(KAFKA_PORT_PROPERTY);
    container.close();
  }

  @Override
  public void close() {
    stop();
  }

  /**
   * Returns the {@code host:port} bootstrap servers of the running broker, without a URL scheme.
   *
   * @return the bootstrap servers connection string
   */
  public String getBootstrapServers() {
    return URL_SCHEME_REPLACEMENT_PATTERN.matcher(container.getBootstrapServers()).replaceFirst("");
  }

  public String getHost() {
    return container.getHost();
  }

  public int getPort() {
    return container.getFirstMappedPort();
  }

  /**
   * Builds a FOLIO {@link KafkaConfig} pointed at the running broker.
   *
   * @param envId the FOLIO environment identifier used for topic prefixes
   * @return a ready-to-use configuration
   */
  public KafkaConfig kafkaConfig(String envId) {
    return KafkaConfig.builder()
      .kafkaHost(getHost())
      .kafkaPort(String.valueOf(getPort()))
      .envId(envId)
      .build();
  }
}
