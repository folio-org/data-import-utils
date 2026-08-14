package org.folio.dataimport.testsupport.vertx;

import static java.util.concurrent.TimeUnit.SECONDS;

import io.vertx.core.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Utility methods for blocking on Vert.x {@link Future} completion in JUnit 5 tests.
 */
public final class VertxTestUtil {

  private VertxTestUtil() {
  }

  /**
   * Blocks until the given {@link Future} completes (up to 60 seconds) and returns its result.
   * Wraps checked exceptions in {@link IllegalStateException} so callers need no try/catch.
   *
   * @param future the future to await
   * @param <T>    the result type
   * @return the result value
   * @throws IllegalStateException if the thread is interrupted or the future fails/times out
   */
  public static <T> T await(Future<T> future) {
    try {
      return future.toCompletionStage().toCompletableFuture().get(60, SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while awaiting Future", e);
    } catch (ExecutionException | TimeoutException e) {
      throw new IllegalStateException("Future did not complete successfully", e);
    }
  }
}
