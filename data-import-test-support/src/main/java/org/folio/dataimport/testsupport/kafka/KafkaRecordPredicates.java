package org.folio.dataimport.testsupport.kafka;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Common {@link Predicate} factories for matching {@link ConsumerRecord}s, meant to be composed
 * with {@link KafkaTestEventCollector#awaitEvent} / {@link KafkaTestEventCollector#assertNoEvent}.
 */
public final class KafkaRecordPredicates {

  private KafkaRecordPredicates() {
    throw new UnsupportedOperationException("Cannot instantiate utility class.");
  }

  /**
   * Matches a record whose key equals {@code key}.
   */
  public static Predicate<ConsumerRecord<String, String>> keyEquals(String key) {
    return consumerRecord -> Objects.equals(consumerRecord.key(), key);
  }

  /**
   * Matches a record whose value contains {@code substring}.
   */
  public static Predicate<ConsumerRecord<String, String>> valueContains(String substring) {
    return consumerRecord -> consumerRecord.value() != null
                             && consumerRecord.value().contains(substring);
  }

  /**
   * Matches a record carrying a header named {@code name} with value {@code value}.
   */
  public static Predicate<ConsumerRecord<String, String>> headerEquals(String name, String value) {
    return consumerRecord -> {
      var header = consumerRecord.headers().lastHeader(name);
      return header != null && value.equals(new String(header.value(), StandardCharsets.UTF_8));
    };
  }

  /**
   * Matches a record whose value is JSON and whose field at {@code jsonPointer} (RFC 6901, e.g.
   * {@code "/eventPayload/id"}) equals {@code expected}. Records with a null or non-JSON value
   * never match.
   */
  public static Predicate<ConsumerRecord<String, String>> jsonBodyAt(String jsonPointer, Object expected) {
    var pointer = JsonPointer.from(jsonPointer);
    return consumerRecord -> {
      if (consumerRecord.value() == null) {
        return false;
      }
      try {
        return Objects.equals(expected, pointer.queryJson(new JsonObject(consumerRecord.value())));
      } catch (DecodeException e) {
        return false;
      }
    };
  }
}
