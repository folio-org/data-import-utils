package org.folio.dataimport.util;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.RestVerticle;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.dataimport.util.RestUtil.OKAPI_TENANT_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_URL_HEADER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class RestUtilTest {

  @Rule
  public WireMockRule mockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @Test
  public void shouldSendRequest(TestContext context) {
    Async async = context.async();

    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL_HEADER, "http://localhost:" + mockServer.port());
    okapiHeaders.put(OKAPI_TENANT_HEADER, "diku");
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TOKEN, "token");
    OkapiConnectionParams params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());

    WireMock.stubFor(WireMock.put(new UrlPathPattern(new RegexPattern("/change-manager/jobExecution/.*"), true))
      .willReturn(WireMock.ok()));

    Future<RestUtil.WrappedResponse> future = RestUtil.doRequest(params, "/change-manager/jobExecution/" + UUID.randomUUID(), HttpMethod.PUT, new JsonObject());
    future.setHandler(response -> {
      assertTrue(response.succeeded());
      assertNotNull(response.result());
      async.complete();
    });
  }

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

  @Test
  public void shouldValidatePartialSuccessAsyncResult() {
    HttpClientResponseStub httpClientResponseStub = new HttpClientResponseStub();
    httpClientResponseStub.headers().add(CONTENT_TYPE, APPLICATION_JSON);
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(500, "", httpClientResponseStub);
    AsyncResult<RestUtil.WrappedResponse> partialSuccessAsyncResult = getAsyncResult(response, null, true, false);
    Future future = Future.future();
    assertTrue(RestUtil.validateAsyncResult(partialSuccessAsyncResult, future));
    assertFalse(future.isComplete());
  }

  @Test
  public void shouldReturnTrueWhenResponseContentTypeIsJsonAndStatusServerError() {
    HttpClientResponseStub responseStub = new HttpClientResponseStub();
    responseStub.setStatusCode(500);
    responseStub.headers().add(CONTENT_TYPE, APPLICATION_JSON);
    Assert.assertTrue(RestUtil.isPartialSuccess(responseStub));
  }

  @Test
  public void shouldReturnFalseWhenResponseContentTypeIsNotJson() {
    HttpClientResponseStub responseStub = new HttpClientResponseStub();
    responseStub.setStatusCode(500);
    Assert.assertFalse(RestUtil.isPartialSuccess(responseStub));
  }

  @Test
  public void shouldReturnFalseWhenResponseStatusIsNotServerError() {
    HttpClientResponseStub responseStub = new HttpClientResponseStub();
    responseStub.setStatusCode(200);
    responseStub.headers().add(CONTENT_TYPE, APPLICATION_JSON);
    Assert.assertFalse(RestUtil.isPartialSuccess(responseStub));
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

  class HttpClientResponseStub implements HttpClientResponse {

    private MultiMap headers = new VertxHttpHeaders();

    private int statusCode;

    @Override
    public HttpClientResponse resume() {
      return null;
    }

    @Override
    public HttpClientResponse exceptionHandler(Handler<Throwable> handler) {
      return null;
    }

    @Override
    public HttpClientResponse handler(Handler<Buffer> handler) {
      return null;
    }

    @Override
    public HttpClientResponse pause() {
      return null;
    }

    @Override
    public HttpClientResponse endHandler(Handler<Void> endHandler) {
      return null;
    }

    @Override
    public HttpVersion version() {
      return null;
    }

    @Override
    public int statusCode() {
      return statusCode;
    }

    public void setStatusCode(int code) {
      this.statusCode = code;
    }

    @Override
    public String statusMessage() {
      return null;
    }

    @Override
    public MultiMap headers() {
      return headers;
    }

    @Override
    public String getHeader(String headerName) {
      return headers.get(headerName);
    }

    @Override
    public String getHeader(CharSequence headerName) {
      return headers().get(headerName);
    }

    @Override
    public String getTrailer(String trailerName) {
      return null;
    }

    @Override
    public MultiMap trailers() {
      return null;
    }

    @Override
    public List<String> cookies() {
      return null;
    }

    @Override
    public HttpClientResponse bodyHandler(Handler<Buffer> bodyHandler) {
      return null;
    }

    @Override
    public HttpClientResponse customFrameHandler(Handler<HttpFrame> handler) {
      return null;
    }

    @Override
    public NetSocket netSocket() {
      return null;
    }

    @Override
    public HttpClientRequest request() {
      return null;
    }
  }
}
