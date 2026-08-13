package org.folio.dataimport.testsupport.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KafkaProducerConsumerIT {

  private static KafkaTestSupport kafka;

  @BeforeAll
  static void startKafka() {
    kafka = new KafkaTestSupport();
    kafka.start();
  }

  @AfterAll
  static void stopKafka() {
    kafka.stop();
  }

  @DisplayName("should deliver message to consumer when producer sends without headers")
  @Test
  void shouldDeliverMessage_whenProducerSendsWithoutHeaders() {
    // arrange
    try (var producer = new KafkaTestProducer(kafka.getBootstrapServers());
         var consumer = new KafkaTestConsumer(kafka.getBootstrapServers(), "group-no-headers")) {
      consumer.subscribe(List.of("topic-no-headers"));

      // act
      producer.send("topic-no-headers", "key1", "value1", null);
      var records = consumer.poll(1, Duration.ofSeconds(30));

      // assert
      assertThat(records).hasSize(1);
      assertThat(records.getFirst().key()).isEqualTo("key1");
      assertThat(records.getFirst().value()).isEqualTo("value1");
    }
  }

  @DisplayName("should attach headers to delivered message when producer sends with headers")
  @Test
  void shouldAttachHeaders_whenProducerSendsWithHeaders() {
    // arrange
    Map<String, String> headers = Map.of("X-Okapi-Tenant", "diku", "X-Okapi-Token", "tok");
    try (var producer = new KafkaTestProducer(kafka.getBootstrapServers());
         var consumer = new KafkaTestConsumer(kafka.getBootstrapServers(), "group-with-headers")) {
      consumer.subscribe(List.of("topic-with-headers"));

      // act
      producer.send("topic-with-headers", "key2", "value2", headers);
      var records = consumer.poll(1, Duration.ofSeconds(30));

      // assert
      assertThat(records).hasSize(1);
      var headerMap = new HashMap<String, String>();
      records.getFirst().headers().forEach(h ->
        headerMap.put(h.key(), new String(h.value(), StandardCharsets.UTF_8)));
      assertThat(headerMap)
        .containsEntry("X-Okapi-Tenant", "diku")
        .containsEntry("X-Okapi-Token", "tok");
    }
  }

  @DisplayName("should return fewer records than expected when timeout elapses before count is reached")
  @Test
  void shouldReturnFewerRecords_whenTimeoutElapsesBeforeExpectedCount() {
    // arrange
    try (var producer = new KafkaTestProducer(kafka.getBootstrapServers());
         var consumer = new KafkaTestConsumer(kafka.getBootstrapServers(), "group-partial")) {
      consumer.subscribe(List.of("topic-partial"));
      producer.send("topic-partial", "k", "v", null);

      // act — request 10 but only 1 was produced; use a short timeout
      var records = consumer.poll(10, Duration.ofSeconds(3));

      // assert
      assertThat(records).isNotEmpty().hasSizeLessThan(10);
    }
  }

  @DisplayName("should return non-null ConsumerRecords from single poll call")
  @Test
  void shouldReturnNonNullConsumerRecords_fromSinglePollCall() {
    // arrange
    try (var consumer = new KafkaTestConsumer(kafka.getBootstrapServers(), "group-single-poll")) {
      consumer.subscribe(List.of("topic-single-poll"));

      // act
      ConsumerRecords<String, String> result = consumer.poll(Duration.ofMillis(500));

      // assert
      assertThat(result).isNotNull();
    }
  }
}
