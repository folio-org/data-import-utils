package org.folio.dataimport.util.test;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Testing {@link GenericHandlerAnswer}
 */
public class GenericHandlerAnswerTest {

  private StubObject stubObject = Mockito.mock(StubObject.class);

  @Test
  public void shouldReturnFutureWithExpectedResult() {
    // given
    JsonObject expectedResult = new JsonObject();
    Future expectedFuture = Future.succeededFuture(expectedResult);
    Mockito.doAnswer(new GenericHandlerAnswer<>(expectedFuture, 0))
      .when(stubObject)
      .targetMethod(ArgumentMatchers.any());

    // when
    Promise<JsonObject> promise = Promise.promise();
    stubObject.targetMethod(promise);

    // then
    promise.future().onComplete(ar -> {
      assertTrue(promise.future().succeeded());
      JsonObject actualResult = promise.future().result();
      assertEquals(expectedResult, actualResult);
    });
  }
}
