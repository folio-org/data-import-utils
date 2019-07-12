package org.folio.dataimport.util;

import io.vertx.core.Future;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.function.Supplier;

@RunWith(JUnit4.class)
public class TryTest {

  @Test
  public void shouldReturnSucceededFutureWhenTaskReturnedResult() {
    Supplier<String> task = () -> "result string";
    Future<String> resultFuture = Try.itGet(task);
    resultFuture.setHandler(ar -> {
      Assert.assertTrue(ar.succeeded());
      Assert.assertEquals(task.get(), ar.result());
    });
  }

  @Test
  public void shouldReturnFailedFutureWhenTaskThrewException() {
    RuntimeException taskException = new RuntimeException();
    Supplier<String> task = () -> {throw taskException;};
    Future<String> resultFuture = Try.itGet(task);
    resultFuture.setHandler(ar -> {
      Assert.assertTrue(ar.failed());
      Assert.assertSame(ar.cause(), taskException);
    });
  }

  @Test
  public void shouldReturnSucceededFutureWhenJobReturnedResult() {
    String resultString = "result string";
    Try.Job<Future<String>> job = future -> future.complete(resultString);
    Future<String> resultFuture = Try.itDo(job);
    resultFuture.setHandler(ar -> {
      Assert.assertTrue(ar.succeeded());
      Assert.assertEquals(resultString, ar.result());
    });
  }

  @Test
  public void shouldReturnFailedFutureWhenJobThrewException() {
    RuntimeException jobException = new RuntimeException();
    Try.Job<Future<String>> job = future -> {throw jobException;};
    Future<String> resultFuture = Try.itDo(job);
    resultFuture.setHandler(ar -> {
      Assert.assertTrue(ar.failed());
      Assert.assertSame(ar.cause(), jobException);
    });
  }
}
