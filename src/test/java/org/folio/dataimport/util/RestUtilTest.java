package org.folio.dataimport.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RestUtilTest {

  @Test
  public void shouldValidateFailedAsyncResult() {
    AsyncResult<RestUtil.WrappedResponse> failedAsyncResult = getAsyncResult(null, new IOException(), false, true);
    Future future = Future.future();
    assertFalse(RestUtil.validateAsyncResult(failedAsyncResult, future));
    assertTrue(future.failed());
    assertTrue(future.cause() instanceof IOException);
  }

  @Test
  public void shouldValidateNullAsyncResult() {
    AsyncResult<RestUtil.WrappedResponse> nullAsyncResult = getAsyncResult(null, null, true, false);
    Future future = Future.future();
    assertFalse(RestUtil.validateAsyncResult(nullAsyncResult, future));
    assertTrue(future.failed());
    assertTrue(future.cause() instanceof BadRequestException);
  }

  @Test
  public void shouldValidateNotFoundAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(404, "", null);
    AsyncResult<RestUtil.WrappedResponse> notFoundAsyncResult = getAsyncResult(response, null, true, false);
    Future future = Future.future();
    assertFalse(RestUtil.validateAsyncResult(notFoundAsyncResult, future));
    assertTrue(future.failed());
    assertTrue(future.cause() instanceof NotFoundException);
  }

  @Test
  public void shouldValidateInternalErrorAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(500, "", null);
    AsyncResult<RestUtil.WrappedResponse> internalErrorAsyncResult = getAsyncResult(response, null, true, false);
    Future future = Future.future();
    assertFalse(RestUtil.validateAsyncResult(internalErrorAsyncResult, future));
    assertTrue(future.failed());
    assertTrue(future.cause() instanceof InternalServerErrorException);
  }

  @Test
  public void shouldValidateOKAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(200, "", null);
    AsyncResult<RestUtil.WrappedResponse> okAsyncResult = getAsyncResult(response, null, true, false);
    Future future = Future.future();
    assertTrue(RestUtil.validateAsyncResult(okAsyncResult, future));
    assertFalse(future.isComplete());
  }

  @Test
  public void shouldValidateCreatedAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(201, "", null);
    AsyncResult<RestUtil.WrappedResponse> createdAsyncResult = getAsyncResult(response, null, true, false);
    Future future = Future.future();
    assertTrue(RestUtil.validateAsyncResult(createdAsyncResult, future));
    assertFalse(future.isComplete());
  }

  @Test
  public void shouldValidateNoContentAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(204, "", null);
    AsyncResult<RestUtil.WrappedResponse> noContentAsyncResult = getAsyncResult(response, null, true, false);
    Future future = Future.future();
    assertTrue(RestUtil.validateAsyncResult(noContentAsyncResult, future));
    assertFalse(future.isComplete());
  }

  @Test
  public void shouldValidateBadRequestAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(422, null, null);
    AsyncResult<RestUtil.WrappedResponse> badRequestAsyncResult = getAsyncResult(response, null, true, false);
    Future future = Future.future();
    assertFalse(RestUtil.validateAsyncResult(badRequestAsyncResult, future));
    assertTrue(future.failed());
    assertTrue(future.cause() instanceof BadRequestException);
  }

  private AsyncResult<RestUtil.WrappedResponse> getAsyncResult(RestUtil.WrappedResponse result, Throwable cause, boolean succeeded, boolean failed) {
    return new AsyncResult<RestUtil.WrappedResponse>() {
      @Override
      public RestUtil.WrappedResponse result() {
        return result;
      }
      @Override
      public Throwable cause() {
        return cause;
      }
      @Override
      public boolean succeeded() {
        return succeeded;
      }
      @Override
      public boolean failed() {
        return failed;
      }
    };
  }
}
