package org.folio.dataimport.util;

import io.vertx.core.Future;

import java.util.function.Supplier;

/**
 * Util class which contains methods with boiler-plate code for exceptions handling under async methods calls
 */
public class Try {

  /**
   * Return future with result returned by specified task.
   * If the {@code task} throws an exception, the returned future will be failed with this exception
   *
   * @param task task
   * @return future with result from {@code task} execution
   */
  public static <T> Future<T> itGet(Supplier<T> task) {
    Future<T> future = Future.future();
    try {
      future.complete(task.get());
    } catch (Exception e) {
      future.fail(e);
    }
    return future;
  }

  /**
   * Return succeed or failed future handled by specified job.
   * If the {@code job} throws an exception, the returned future will be failed with this exception
   *
   * @param job job
   * @return return handled future by specified job
   */
  public static <T> Future<T> itDo(Job<Future<T>> job) {
    Future<T> future = Future.future();
    try {
      job.accept(future);
    } catch (Exception e) {
      future.fail(e);
    }
    return future;
  }

  @FunctionalInterface
  public interface Job<T> {
    void accept(T t) throws Exception;//NOSONAR
  }

}
