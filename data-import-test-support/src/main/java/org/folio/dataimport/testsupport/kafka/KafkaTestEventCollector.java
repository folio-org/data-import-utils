package org.folio.dataimport.testsupport.kafka;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Continuously drains one or more Kafka topics into an in-memory, per-topic index so that
 * repeated test assertions check already-buffered data instead of re-polling the broker.
 *
 * <p>A single background thread subscribes once, for the lifetime of the collector, to every
 * topic passed to the constructor and keeps appending records to a thread-safe per-topic list.
 * {@link #awaitEvent} and {@link #assertNoEvent} then poll that in-memory index at a short
 * interval: a record that already arrived is matched on the first check, so assertions made
 * after the fact return immediately instead of waiting out a fixed per-call broker timeout —
 * and a single collector serves assertions against any number of topics without opening a new
 * consumer (and re-fetching broker metadata) per check, the way a one-shot poll-per-call helper
 * would.
 *
 * <p>Create one collector per test class (or per test) against the broker exposed by
 * {@link KafkaExtension}, produce the event(s) under test, then assert with
 * {@link #awaitEvent}/{@link #assertNoEvent}. Always {@link #close()} it — e.g. via
 * try-with-resources or {@code @AfterEach}/{@code @AfterAll} — to stop the background thread and
 * release the consumer.
 */
public final class KafkaTestEventCollector implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger();
  private static final Duration BROKER_POLL_INTERVAL = Duration.ofMillis(50);
  private static final Duration INDEX_CHECK_INTERVAL = Duration.ofMillis(20);

  private final KafkaConsumer<String, String> consumer;
  private final Map<String, List<ConsumerRecord<String, String>>> recordsByTopic = new ConcurrentHashMap<>();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final Thread pollingThread;

  /**
   * Starts a background consumer subscribed to {@code topics}.
   *
   * @param bootstrapServers the {@code host:port} of the broker, e.g. from
   *                          {@link KafkaExtension#getBootstrapServers()}
   * @param groupId           the consumer group id; use a unique value per collector instance so
   *                           collectors in different tests don't share committed offsets
   * @param topics             the fully-qualified topic names to collect from
   */
  public KafkaTestEventCollector(String bootstrapServers, String groupId, Collection<String> topics) {
    this.consumer = new KafkaConsumer<>(consumerConfig(bootstrapServers, groupId),
      new StringDeserializer(), new StringDeserializer());
    consumer.subscribe(topics);
    this.pollingThread = new Thread(this::pollLoop, "kafka-test-event-collector-" + groupId);
    this.pollingThread.setDaemon(true);
    this.pollingThread.start();
  }

  private void pollLoop() {
    try {
      while (!closed.get()) {
        var records = consumer.poll(BROKER_POLL_INTERVAL);
        records.forEach(consumerRecord ->
          recordsByTopic.computeIfAbsent(consumerRecord.topic(),
            key -> new CopyOnWriteArrayList<>()).add(consumerRecord));
      }
    } catch (WakeupException e) {
      if (!closed.get()) {
        LOGGER.warn("pollLoop:: Unexpected wakeup while collector was still open", e);
      }
    } finally {
      consumer.close();
    }
  }

  /**
   * Blocks until a record matching {@code predicate} has been observed on {@code topic}, or
   * throws once {@code timeout} elapses. A match already present in the in-memory index (i.e.
   * received before this call) is returned immediately.
   *
   * @return the first matching record
   * @throws AssertionError if no match appears within {@code timeout}
   */
  public ConsumerRecord<String, String> awaitEvent(String topic, Predicate<ConsumerRecord<String, String>> predicate,
                                                     Duration timeout) {
    var deadline = Instant.now().plus(timeout);
    while (true) {
      var match = findFirst(topic, predicate);
      if (match != null) {
        return match;
      }
      if (Instant.now().isAfter(deadline)) {
        throw new AssertionError("Timed out after " + timeout + " waiting for an event on topic '" + topic
          + "' matching the given predicate. Records observed on that topic so far: " + received(topic));
      }
      sleep(INDEX_CHECK_INTERVAL);
    }
  }

  /**
   * Asserts that no record matching {@code predicate} appears on {@code topic} within
   * {@code quietPeriod}. Unlike {@link #awaitEvent}, this always waits out the full quiet period
   * since absence can only be established once the window has fully elapsed.
   *
   * @throws AssertionError as soon as a matching record is observed
   */
  public void assertNoEvent(String topic, Predicate<ConsumerRecord<String, String>> predicate, Duration quietPeriod) {
    var deadline = Instant.now().plus(quietPeriod);
    while (Instant.now().isBefore(deadline)) {
      var match = findFirst(topic, predicate);
      if (match != null) {
        throw new AssertionError("Expected no event on topic '" + topic
          + "' matching the given predicate, but found: " + match);
      }
      sleep(INDEX_CHECK_INTERVAL);
    }
  }

  /**
   * Returns a snapshot of every record observed so far on {@code topic}.
   */
  public List<ConsumerRecord<String, String>> received(String topic) {
    return new ArrayList<>(recordsByTopic.getOrDefault(topic, List.of()));
  }

  private ConsumerRecord<String, String> findFirst(String topic, Predicate<ConsumerRecord<String, String>> predicate) {
    for (var record : recordsByTopic.getOrDefault(topic, List.of())) {
      if (predicate.test(record)) {
        return record;
      }
    }
    return null;
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for Kafka test events", e);
    }
  }

  /**
   * Stops the background poller and closes the underlying consumer. Safe to call more than once.
   */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      consumer.wakeup();
      try {
        pollingThread.join(Duration.ofSeconds(5).toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
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
