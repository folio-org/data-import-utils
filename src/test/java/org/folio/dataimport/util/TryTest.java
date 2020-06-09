package org.folio.dataimport.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TryTest {

  private static final String RESULT_STRING = "result string";

  @Test
  public void shouldReturnSucceededFutureWhenTaskReturnedResult() {
    Try.itGet(() -> RESULT_STRING)
      .onComplete(ar -> {
        Assert.assertTrue(ar.succeeded());
        Assert.assertEquals(RESULT_STRING, ar.result());
      });
  }

  @Test
  public void shouldReturnFailedFutureWhenTaskThrewException() {
    RuntimeException taskException = new RuntimeException();
    Try.itGet(() -> {throw taskException;})
      .onComplete(ar -> {
        Assert.assertTrue(ar.failed());
        Assert.assertSame(ar.cause(), taskException);
      });
  }

  @Test
  public void shouldReturnSucceededFutureWhenJobReturnedResult() {
    Try.itDo(future -> future.complete(RESULT_STRING))
      .onComplete(ar -> {
        Assert.assertTrue(ar.succeeded());
        Assert.assertEquals(RESULT_STRING, ar.result());
      });
  }

  @Test
  public void shouldReturnFailedFutureWhenJobThrewException() {
    RuntimeException jobException = new RuntimeException();
    Try.itDo(future -> {throw jobException;})
      .onComplete(ar -> {
        Assert.assertTrue(ar.failed());
        Assert.assertSame(ar.cause(), jobException);
      });
  }
}
