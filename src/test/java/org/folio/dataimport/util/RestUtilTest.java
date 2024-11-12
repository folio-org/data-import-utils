package org.folio.dataimport.util;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.RestVerticle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.folio.dataimport.util.RestUtil.OKAPI_TENANT_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_TOKEN_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_URL_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class RestUtilTest {
  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

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
    future.onComplete(response -> {
      assertTrue(response.succeeded());
      assertNotNull(response.result());
      async.complete();
    });
  }

  @Test
  public void shouldValidateFailedAsyncResult() {
    AsyncResult<RestUtil.WrappedResponse> failedAsyncResult = getAsyncResult(null, new IOException(), false, true);
    Promise promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(failedAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertTrue(promise.future().cause() instanceof IOException);
  }

  @Test
  public void shouldValidateNullAsyncResult() {
    AsyncResult<RestUtil.WrappedResponse> nullAsyncResult = getAsyncResult(null, null, true, false);
    Promise promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(nullAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertTrue(promise.future().cause() instanceof BadRequestException);
  }

  @Test
  public void shouldValidateNotFoundAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(404, "", null);
    AsyncResult<RestUtil.WrappedResponse> notFoundAsyncResult = getAsyncResult(response, null, true, false);
    Promise promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(notFoundAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertTrue(promise.future().cause() instanceof NotFoundException);
  }

  @Test
  public void shouldValidateInternalErrorAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(500, "", null);
    AsyncResult<RestUtil.WrappedResponse> internalErrorAsyncResult = getAsyncResult(response, null, true, false);
    Promise promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(internalErrorAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertTrue(promise.future().cause() instanceof InternalServerErrorException);
  }

  @Test
  public void shouldValidateOKAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(200, "", null);
    AsyncResult<RestUtil.WrappedResponse> okAsyncResult = getAsyncResult(response, null, true, false);
    Promise promise = Promise.promise();
    assertTrue(RestUtil.validateAsyncResult(okAsyncResult, promise));
    assertFalse(promise.future().isComplete());
  }

  @Test
  public void shouldValidateCreatedAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(201, "", null);
    AsyncResult<RestUtil.WrappedResponse> createdAsyncResult = getAsyncResult(response, null, true, false);
    Promise promise = Promise.promise();
    assertTrue(RestUtil.validateAsyncResult(createdAsyncResult, promise));
    assertFalse(promise.future().isComplete());
  }

  @Test
  public void shouldValidateNoContentAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(204, "", null);
    AsyncResult<RestUtil.WrappedResponse> noContentAsyncResult = getAsyncResult(response, null, true, false);
    Promise promise = Promise.promise();
    assertTrue(RestUtil.validateAsyncResult(noContentAsyncResult, promise));
    assertFalse(promise.future().isComplete());
  }

  @Test
  public void shouldValidateBadRequestAsyncResult() {
    RestUtil.WrappedResponse response = new RestUtil.WrappedResponse(422, null, null);
    AsyncResult<RestUtil.WrappedResponse> badRequestAsyncResult = getAsyncResult(response, null, true, false);
    Promise promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(badRequestAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertTrue(promise.future().cause() instanceof BadRequestException);
  }

  @Test
  public void shouldRemoveTokenHeaderWhenSystemUserDisabled(TestContext context) {
    Async async = context.async();

    final var url = "http://localhost:" + mockServer.port();
    final var tenant = "diku";
    final var token = "token";

    // Set the system property to disable system user
    System.setProperty("SYSTEM_USER_ENABLED", "false");

    Map<String, String> okapiHeaders = new HashMap<>();

    okapiHeaders.put(OKAPI_URL_HEADER, url);
    okapiHeaders.put(OKAPI_TENANT_HEADER, tenant);
    okapiHeaders.put(OKAPI_TOKEN_HEADER, token);
    var params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());

    WireMock.stubFor(WireMock.get("/test-endpoint")
      .willReturn(WireMock.ok()));

    var future = RestUtil.doRequestWithSystemUser(
      params, "/test-endpoint", HttpMethod.GET, null);

    future.onComplete(response -> {
      try {
        assertTrue(response.succeeded());
        assertNotNull(response.result());

        // Verify that the token header was removed
        var requests = WireMock.findAll(
          WireMock.getRequestedFor(WireMock.urlEqualTo("/test-endpoint")));
        assertEquals(1, requests.size());
        var request = requests.get(0);

        var tokenHeader = request.getHeader(OKAPI_TOKEN_HEADER);
        assertNull("Token header should be removed when SYSTEM_USER_ENABLED is false", tokenHeader);

        // Verify other headers
        assertEquals(tenant, request.getHeader(OKAPI_TENANT_HEADER));
        assertEquals(url, request.getHeader(OKAPI_URL_HEADER));

        async.complete();
      } catch (AssertionError e) {
        context.fail(e);
      } finally {
        System.clearProperty("SYSTEM_USER_ENABLED");
      }
    });
  }

  @Test
  public void shouldNotRemoveTokenHeaderWhenSystemUserPropertyMissing(TestContext context) {
    Async async = context.async();

    final var url = "http://localhost:" + mockServer.port();
    final var tenant = "diku";
    final var token = "token";

    Map<String, String> okapiHeaders = new HashMap<>();

    okapiHeaders.put(OKAPI_URL_HEADER, url);
    okapiHeaders.put(OKAPI_TENANT_HEADER, tenant);
    okapiHeaders.put(OKAPI_TOKEN_HEADER, token);
    var params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());

    WireMock.stubFor(WireMock.get("/test-endpoint")
      .willReturn(WireMock.ok()));

    var future = RestUtil.doRequestWithSystemUser(
      params, "/test-endpoint", HttpMethod.GET, null);

    future.onComplete(response -> {
      try {
        assertTrue(response.succeeded());
        assertNotNull(response.result());

        var requests = WireMock.findAll(
          WireMock.getRequestedFor(WireMock.urlEqualTo("/test-endpoint")));
        assertEquals(1, requests.size());
        var request = requests.get(0);

        // Verify headers
        assertEquals(token, request.getHeader(OKAPI_TOKEN_HEADER));
        assertEquals(tenant, request.getHeader(OKAPI_TENANT_HEADER));
        assertEquals(url, request.getHeader(OKAPI_URL_HEADER));

        async.complete();
      } catch (AssertionError e) {
        context.fail(e);
      }
    });
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
