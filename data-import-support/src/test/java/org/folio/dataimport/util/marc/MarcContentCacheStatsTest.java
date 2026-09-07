package org.folio.dataimport.util.marc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.junit.jupiter.api.Test;

class MarcContentCacheStatsTest {

  @Test
  void shouldCreateStatsFromCaffeineCacheStats() {
    var caffeineStats = CacheStats.of(
      10, // hitCount
      5,  // missCount
      3,  // loadSuccessCount
      0,  // loadFailureCount
      100, // totalLoadTime
      0,  // evictionCount
      0   // evictionWeight
    );

    var stats = MarcContentCacheStats.fromCaffeine(caffeineStats);

    assertEquals(15, stats.requestCount());
    assertEquals(10, stats.hitCount());
    assertEquals(5, stats.missCount());
    assertEquals(3, stats.loadCount());
  }

  @Test
  void shouldCalculateDifferenceBetweenSnapshots() {
    var earlier = new MarcContentCacheStats(10, 7, 3, 2);
    var current = new MarcContentCacheStats(25, 18, 7, 5);

    var delta = current.minus(earlier);

    assertEquals(15, delta.requestCount());
    assertEquals(11, delta.hitCount());
    assertEquals(4, delta.missCount());
    assertEquals(3, delta.loadCount());
  }

  @Test
  void shouldClampNegativeDifferencesToZero() {
    var earlier = new MarcContentCacheStats(25, 18, 7, 5);
    var current = new MarcContentCacheStats(10, 7, 3, 2);

    var delta = current.minus(earlier);

    assertEquals(0, delta.requestCount());
    assertEquals(0, delta.hitCount());
    assertEquals(0, delta.missCount());
    assertEquals(0, delta.loadCount());
  }

  @Test
  void shouldHandleMixedPositiveAndNegativeDifferences() {
    var earlier = new MarcContentCacheStats(10, 5, 5, 3);
    var current = new MarcContentCacheStats(15, 4, 8, 2);

    var delta = current.minus(earlier);

    assertEquals(5, delta.requestCount());
    assertEquals(0, delta.hitCount());
    assertEquals(3, delta.missCount());
    assertEquals(0, delta.loadCount());
  }

  @Test
  void shouldReturnZeroDifferenceForEqualSnapshots() {
    var stats = new MarcContentCacheStats(10, 7, 3, 2);

    var delta = stats.minus(stats);

    assertEquals(new MarcContentCacheStats(0, 0, 0, 0), delta);
  }
}
