package org.folio.dataimport.testsupport.kafka;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Minimal synchronous Kafka consumer for asserting DataImport test events.
 *
 * <p>Backed by the standard {@code kafka-clients} consumer with {@code earliest} offset reset, so
 * that events published before the consumer subscribed are still observed by the assertions.
 */
public final class KafkaTestConsumer implements AutoCloseable {

  private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

  private final KafkaConsumer<String, String> consumer;

  public KafkaTestConsumer(String bootstrapServers, String groupId) {
    this.consumer = new KafkaConsumer<>(consumerConfig(bootstrapServers, groupId),
      new StringDeserializer(), new StringDeserializer());
  }

  /**
   * Subscribes this consumer to the given topics.
   *
   * @param topics the topics to consume from
   */
  public void subscribe(Collection<String> topics) {
    consumer.subscribe(topics);
  }

  /**
   * Polls once for records using the supplied timeout.
   *
   * @param timeout the maximum time to block waiting for records
   * @return the records returned by the broker
   */
  public ConsumerRecords<String, String> poll(Duration timeout) {
    return consumer.poll(timeout);
  }

  /**
   * Polls repeatedly until at least {@code expectedCount} records are collected or the timeout
   * elapses, whichever comes first.
   *
   * @param expectedCount the minimum number of records to wait for
   * @param timeout       the overall timeout
   * @return the collected records
   */
  public List<ConsumerRecord<String, String>> poll(int expectedCount, Duration timeout) {
    var collected = new ArrayList<ConsumerRecord<String, String>>();
    var deadline = Instant.now().plus(timeout);
    while (collected.size() < expectedCount && Instant.now().isBefore(deadline)) {
      consumer.poll(POLL_INTERVAL).forEach(collected::add);
    }
    return collected;
  }

  @Override
  public void close() {
    consumer.close();
  }

  private static Map<String, Object> consumerConfig(String bootstrapServers, String groupId) {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    return config;
  }
}
