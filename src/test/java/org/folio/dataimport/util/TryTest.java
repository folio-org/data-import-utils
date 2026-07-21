package org.folio.dataimport.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TryTest {

  private static final String RESULT_STRING = "result string";

  @Test
  void shouldReturnSucceededFutureWhenTaskReturnedResult() {
    Try.itGet(() -> RESULT_STRING)
      .onComplete(ar -> {
        assertTrue(ar.succeeded());
        assertEquals(RESULT_STRING, ar.result());
      });
  }

  @Test
  void shouldReturnFailedFutureWhenTaskThrewException() {
    RuntimeException taskException = new RuntimeException();
    Try.itGet(
        () -> {
          throw taskException;
        })
      .onComplete(ar -> {
        assertTrue(ar.failed());
        assertSame(ar.cause(), taskException);
      });
  }

  @Test
  void shouldReturnSucceededFutureWhenJobReturnedResult() {
    Try.itDo(future -> future.complete(RESULT_STRING))
      .onComplete(ar -> {
        assertTrue(ar.succeeded());
        assertEquals(RESULT_STRING, ar.result());
      });
  }

  @Test
  void shouldReturnFailedFutureWhenJobThrewException() {
    RuntimeException jobException = new RuntimeException();
    Try.itDo(
        future -> {
          throw jobException;
        })
      .onComplete(ar -> {
        assertTrue(ar.failed());
        assertSame(ar.cause(), jobException);
      });
  }
}
