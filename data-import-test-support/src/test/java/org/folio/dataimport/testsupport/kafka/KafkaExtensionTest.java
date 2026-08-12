package org.folio.dataimport.testsupport.kafka;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KafkaExtensionTest {

  @Test
  void getSupportShouldFailWhenContainerNotStarted() {
    var extension = new KafkaExtension();

    var error = assertThrows(IllegalStateException.class, extension::getSupport);

    assertTrue(error.getMessage().contains("not started"));
  }
}
