package org.folio.dataimport.testsupport.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.kafka.KafkaConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KafkaTestSupportIT {

  private KafkaTestSupport support;

  @BeforeEach
  void setUp() {
    support = new KafkaTestSupport();
  }

  @AfterEach
  void tearDown() {
    support.stop();
  }

  @DisplayName("should set KAFKA_HOST and KAFKA_PORT system properties after start")
  @Test
  void shouldSetSystemProperties_afterStart() {
    // act
    support.start();

    // assert
    assertThat(System.getProperty(KafkaTestSupport.KAFKA_HOST_PROPERTY)).isNotBlank();
    assertThat(System.getProperty(KafkaTestSupport.KAFKA_PORT_PROPERTY)).isNotBlank();
  }

  @DisplayName("should clear KAFKA_HOST and KAFKA_PORT system properties after stop")
  @Test
  void shouldClearSystemProperties_afterStop() {
    // arrange
    support.start();

    // act
    support.stop();

    // assert
    assertThat(System.getProperty(KafkaTestSupport.KAFKA_HOST_PROPERTY)).isNull();
    assertThat(System.getProperty(KafkaTestSupport.KAFKA_PORT_PROPERTY)).isNull();
  }

  @DisplayName("should return bootstrap servers string without URL scheme after start")
  @Test
  void shouldReturnBootstrapServersWithoutScheme_afterStart() {
    // arrange
    support.start();

    // act
    String servers = support.getBootstrapServers();

    // assert
    assertThat(servers).isNotBlank().doesNotContain("://");
  }

  @DisplayName("should build KafkaConfig with correct envId, host and port after start")
  @Test
  void shouldBuildKafkaConfigWithCorrectValues_afterStart() {
    // arrange
    support.start();

    // act
    KafkaConfig config = support.kafkaConfig("test-env");

    // assert
    assertThat(config.getEnvId()).isEqualTo("test-env");
    assertThat(config.getKafkaHost()).isEqualTo(support.getHost());
    assertThat(config.getKafkaPort()).isEqualTo(String.valueOf(support.getPort()));
  }
}
