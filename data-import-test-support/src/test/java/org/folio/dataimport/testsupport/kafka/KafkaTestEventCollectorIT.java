package org.folio.dataimport.testsupport.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dataimport.testsupport.kafka.KafkaRecordPredicates.headerEquals;
import static org.folio.dataimport.testsupport.kafka.KafkaRecordPredicates.jsonBodyAt;
import static org.folio.dataimport.testsupport.kafka.KafkaRecordPredicates.keyEquals;
import static org.folio.dataimport.testsupport.kafka.KafkaRecordPredicates.valueContains;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KafkaTestEventCollectorIT {

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

  @DisplayName("should return already collected event immediately when event arrived before await call")
  @Test
  void shouldReturnAlreadyCollectedEvent_whenEventArrivedBeforeAwaitCall() {
    // arrange
    try (var producer = new KafkaTestProducer(kafka.getBootstrapServers());
         var collector = new KafkaTestEventCollector(kafka.getBootstrapServers(), "group-already-arrived",
           List.of("topic-already-arrived"))) {
      producer.send("topic-already-arrived", "key1", "{\"status\":\"DONE\"}", null);
      collector.awaitEvent("topic-already-arrived", keyEquals("key1"), Duration.ofSeconds(30));

      // act
      var start = System.nanoTime();
      var match = collector.awaitEvent("topic-already-arrived", keyEquals("key1"), Duration.ofSeconds(30));
      var elapsed = Duration.ofNanos(System.nanoTime() - start);

      // assert
      assertThat(match.value()).isEqualTo("{\"status\":\"DONE\"}");
      assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
    }
  }

  @DisplayName("should collect events from multiple topics using a single collector")
  @Test
  void shouldCollectEventsFromMultipleTopics_usingSingleCollector() {
    // arrange
    try (var producer = new KafkaTestProducer(kafka.getBootstrapServers());
         var collector = new KafkaTestEventCollector(kafka.getBootstrapServers(), "group-multi-topic",
           List.of("topic-multi-a", "topic-multi-b"))) {

      // act
      producer.send("topic-multi-a", "a1", "value-a", null);
      producer.send("topic-multi-b", "b1", "value-b", null);
      var onA = collector.awaitEvent("topic-multi-a", valueContains("value-a"), Duration.ofSeconds(30));
      var onB = collector.awaitEvent("topic-multi-b", valueContains("value-b"), Duration.ofSeconds(30));

      // assert
      assertThat(onA.topic()).isEqualTo("topic-multi-a");
      assertThat(onB.topic()).isEqualTo("topic-multi-b");
    }
  }

  @DisplayName("should match on header value when awaiting event by header predicate")
  @Test
  void shouldMatchOnHeaderValue_whenAwaitingEventByHeaderPredicate() {
    // arrange
    Map<String, String> headers = Map.of("X-Okapi-Tenant", "diku");
    try (var producer = new KafkaTestProducer(kafka.getBootstrapServers());
         var collector = new KafkaTestEventCollector(kafka.getBootstrapServers(), "group-header-match",
           List.of("topic-header-match"))) {

      // act
      producer.send("topic-header-match", "k", "v", headers);
      var match = collector.awaitEvent("topic-header-match", headerEquals("X-Okapi-Tenant", "diku"),
        Duration.ofSeconds(30));

      // assert
      assertThat(match.key()).isEqualTo("k");
    }
  }

  @DisplayName("should match on JSON body field when awaiting event by json pointer predicate")
  @Test
  void shouldMatchOnJsonBodyField_whenAwaitingEventByJsonPointerPredicate() {
    // arrange
    try (var producer = new KafkaTestProducer(kafka.getBootstrapServers());
         var collector = new KafkaTestEventCollector(kafka.getBootstrapServers(), "group-json-match",
           List.of("topic-json-match"))) {

      // act
      producer.send("topic-json-match", "k", "{\"eventType\":\"DI_COMPLETED\"}", null);
      var match = collector.awaitEvent("topic-json-match", jsonBodyAt("/eventType", "DI_COMPLETED"),
        Duration.ofSeconds(30));

      // assert
      assertThat(match.value()).contains("DI_COMPLETED");
    }
  }

  @DisplayName("should throw with diagnostic message when timeout elapses before matching event arrives")
  @Test
  void shouldThrowWithDiagnosticMessage_whenTimeoutElapsesBeforeMatchingEventArrives() {
    // arrange
    try (var collector = new KafkaTestEventCollector(kafka.getBootstrapServers(), "group-timeout",
      List.of("topic-timeout"))) {

      // act & assert
      assertThatThrownBy(() ->
        collector.awaitEvent("topic-timeout", keyEquals("missing-key"), Duration.ofMillis(300)))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("topic-timeout");
    }
  }

  @DisplayName("should pass when no matching event arrives during the quiet period")
  @Test
  void shouldPass_whenNoMatchingEventArrivesDuringQuietPeriod() {
    // arrange
    try (var collector = new KafkaTestEventCollector(kafka.getBootstrapServers(), "group-no-event",
      List.of("topic-no-event"))) {

      // act & assert
      collector.assertNoEvent("topic-no-event", keyEquals("any-key"), Duration.ofMillis(300));
    }
  }

  @DisplayName("should throw when a matching event arrives during the quiet period")
  @Test
  void shouldThrow_whenMatchingEventArrivesDuringQuietPeriod() {
    // arrange
    try (var producer = new KafkaTestProducer(kafka.getBootstrapServers());
         var collector = new KafkaTestEventCollector(kafka.getBootstrapServers(), "group-unexpected-event",
           List.of("topic-unexpected-event"))) {
      producer.send("topic-unexpected-event", "key", "value", null);

      // act & assert
      assertThatThrownBy(() ->
        collector.assertNoEvent("topic-unexpected-event", keyEquals("key"), Duration.ofSeconds(5)))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("topic-unexpected-event");
    }
  }
}
