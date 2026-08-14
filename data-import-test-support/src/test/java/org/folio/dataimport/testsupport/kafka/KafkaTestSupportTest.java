package org.folio.dataimport.testsupport.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KafkaTestSupportTest {

  @Test
  void shouldFormatFolioTopicName() {
    var topic = KafkaTestSupport.topicName("test-env", "diku", "DI_COMPLETED");

    assertEquals("test-env.Default.diku.DI_COMPLETED", topic);
  }

  @DisplayName("should expose KAFKA_HOST as the system property name for the broker host")
  @Test
  void shouldExposeKafkaHostAsSystemPropertyName() {
    assertThat(KafkaTestSupport.KAFKA_HOST_PROPERTY).isEqualTo("KAFKA_HOST");
  }

  @DisplayName("should expose KAFKA_PORT as the system property name for the broker port")
  @Test
  void shouldExposeKafkaPortAsSystemPropertyName() {
    assertThat(KafkaTestSupport.KAFKA_PORT_PROPERTY).isEqualTo("KAFKA_PORT");
  }
}
