package org.folio.dataimport.testsupport.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KafkaTestSupportTest {

  @Test
  void shouldFormatFolioTopicName() {
    var topic = KafkaTestSupport.topicName("test-env", "diku", "DI_COMPLETED");

    assertEquals("test-env.Default.diku.DI_COMPLETED", topic);
  }
}
