package org.folio.dataimport.testsupport.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.kafka.KafkaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class KafkaExtensionIT {

  @RegisterExtension
  static final KafkaExtension KAFKA = new KafkaExtension();

  @DisplayName("should provide non-blank bootstrap servers without URL scheme when extension is active")
  @Test
  void shouldProvideBootstrapServersWithoutScheme_whenExtensionIsActive() {
    assertThat(KAFKA.getBootstrapServers()).isNotBlank().doesNotContain("://");
  }

  @DisplayName("should build KafkaConfig with the requested envId when extension is active")
  @Test
  void shouldBuildKafkaConfigWithRequestedEnvId_whenExtensionIsActive() {
    // act
    KafkaConfig config = KAFKA.kafkaConfig("my-env");

    // assert
    assertThat(config.getEnvId()).isEqualTo("my-env");
  }

  @DisplayName("should return the same KafkaTestSupport instance on repeated getSupport calls")
  @Test
  void shouldReturnSameInstance_whenGetSupportCalledRepeatedly() {
    assertThat(KAFKA.getSupport()).isSameAs(KAFKA.getSupport());
  }
}
