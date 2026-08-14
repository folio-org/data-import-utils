package org.folio.dataimport.testsupport.kafka;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Minimal synchronous Kafka producer for publishing DataImport test events.
 *
 * <p>Backed by the standard {@code kafka-clients} producer and configured to acknowledge every send,
 * which makes it convenient for feeding events into a module under test and asserting the outcome.
 */
public final class KafkaTestProducer implements AutoCloseable {

  private final KafkaProducer<String, String> producer;

  public KafkaTestProducer(String bootstrapServers) {
    this.producer = new KafkaProducer<>(producerConfig(bootstrapServers),
      new StringSerializer(), new StringSerializer());
  }

  /**
   * Sends a record to the given topic and blocks until the broker acknowledges it.
   *
   * @param topic   the destination topic
   * @param key     the record key
   * @param value   the record payload
   * @param headers optional headers (e.g. Okapi headers) to attach, may be {@code null}
   * @return the resulting record metadata
   */
  public RecordMetadata send(String topic, String key, String value, Map<String, String> headers) {
    var producerRecord = new ProducerRecord<>(topic, key, value);
    if (headers != null) {
      headers.forEach((name, headerValue) ->
        producerRecord.headers().add(new RecordHeader(name, headerValue.getBytes(StandardCharsets.UTF_8))));
    }
    try {
      return producer.send(producerRecord).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while sending Kafka record", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Failed to send Kafka record to topic " + topic, e);
    }
  }

  @Override
  public void close() {
    producer.close();
  }

  private static Map<String, Object> producerConfig(String bootstrapServers) {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.ACKS_CONFIG, "all");
    config.put(ProducerConfig.CLIENT_ID_CONFIG, "data-import-test-producer");
    return config;
  }
}
