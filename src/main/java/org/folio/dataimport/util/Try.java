package org.folio.dataimport.util;

import io.vertx.core.Future;

import java.util.function.Supplier;

/**
 * Util class which contains methods with boiler-plate code for exceptions handling under async methods calls.
 */
public class Try {

  private Try() {
  }

  /**
   * Executes a {@code task} and returns either a future with result or a future with an exception.
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
   * Executes a job. It creates new future and pass it to a job as a parameter, so you are able to control it.
   * If a {@code job} threw an exception then the future will completed and propagated with this exception.
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

  /**
   * Represents an operation that accepts a single input argument and returns no result
   * or throws an exception if unable to do so.
   *
   * @param <T> the type of the input to the Job
   */
  @FunctionalInterface
  public interface Job<T> {

    /**
     * Performs this operation on the given argument, or throws an exception if unable to do so
     *
     * @param t the job argument
     * @throws Exception
     */
    void accept(T t) throws Exception;//NOSONAR
  }

}
