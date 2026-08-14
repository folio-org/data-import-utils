package org.folio.dataimport.testsupport.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KafkaExtensionTest {

  @Test
  void getSupportShouldFailWhenContainerNotStarted() {
    var extension = new KafkaExtension();

    var error = assertThrows(IllegalStateException.class, extension::getSupport);

    assertTrue(error.getMessage().contains("not started"));
  }

  @DisplayName("should throw when getBootstrapServers is called before container is started")
  @Test
  void shouldThrow_whenGetBootstrapServersCalledBeforeContainerIsStarted() {
    // arrange
    var extension = new KafkaExtension();

    // assert
    assertThatThrownBy(extension::getBootstrapServers)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("not started");
  }

  @DisplayName("should throw when kafkaConfig is called before container is started")
  @Test
  void shouldThrow_whenKafkaConfigCalledBeforeContainerIsStarted() {
    // arrange
    var extension = new KafkaExtension();

    // assert
    assertThatThrownBy(() -> extension.kafkaConfig("test-env"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("not started");
  }
}
