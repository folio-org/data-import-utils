package org.folio.dataimport.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.folio.dataimport.util.RestUtil.doRequest;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.TOKEN;
import static org.folio.okapi.common.XOkapiHeaders.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(VertxExtension.class)
class RestUtilTest {

  @RegisterExtension
  static WireMockExtension mockServer = WireMockExtension.newInstance()
    .options(WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)))
    .build();

  private static final String TENANT_ID = "diku";
  private static final String TEST_TOKEN = "token";
  private static final String TEST_URL = "/change-manager/jobExecution/";

  @Test
  void shouldSendRequest(Vertx vertx, VertxTestContext context) {
    vertx.runOnContext(v -> {
      var headersMap = Map.of(
        URL, mockServer.baseUrl(),
        TENANT, TENANT_ID,
        TOKEN, TEST_TOKEN
      );
      var params = new ConnectionParams(headersMap);

      mockServer.stubFor(put(urlPathMatching(TEST_URL + ".*")).willReturn(ok()));

      var future = doRequest(params, TEST_URL + UUID.randomUUID(), HttpMethod.PUT, new JsonObject());
      future.onComplete(response -> context.verify(() -> {
        assertTrue(response.succeeded());
        assertNotNull(response.result());
        context.completeNow();
      }));
    });
  }

  @Test
  void shouldValidateFailedAsyncResult() {
    var failedAsyncResult = getAsyncResult(null, new IOException(), false, true);
    var promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(failedAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertInstanceOf(IOException.class, promise.future().cause());
  }

  @Test
  void shouldValidateNullAsyncResult() {
    var nullAsyncResult = getAsyncResult(null, null, true, false);
    var promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(nullAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertInstanceOf(BadRequestException.class, promise.future().cause());
  }

  @Test
  void shouldValidateNotFoundAsyncResult() {
    var response = new RestUtil.WrappedResponse(404, "", null);
    var notFoundAsyncResult = getAsyncResult(response, null, true, false);
    var promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(notFoundAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertInstanceOf(NotFoundException.class, promise.future().cause());
  }

  @Test
  void shouldValidateInternalErrorAsyncResult() {
    var response = new RestUtil.WrappedResponse(500, "", null);
    var internalErrorAsyncResult = getAsyncResult(response, null, true, false);
    var promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(internalErrorAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertInstanceOf(InternalServerErrorException.class, promise.future().cause());
  }

  @Test
  void shouldValidateOkAsyncResult() {
    var response = new RestUtil.WrappedResponse(200, "", null);
    var okAsyncResult = getAsyncResult(response, null, true, false);
    var promise = Promise.promise();
    assertTrue(RestUtil.validateAsyncResult(okAsyncResult, promise));
    assertFalse(promise.future().isComplete());
  }

  @Test
  void shouldValidateCreatedAsyncResult() {
    var response = new RestUtil.WrappedResponse(201, "", null);
    var createdAsyncResult = getAsyncResult(response, null, true, false);
    var promise = Promise.promise();
    assertTrue(RestUtil.validateAsyncResult(createdAsyncResult, promise));
    assertFalse(promise.future().isComplete());
  }

  @Test
  void shouldValidateNoContentAsyncResult() {
    var response = new RestUtil.WrappedResponse(204, "", null);
    var noContentAsyncResult = getAsyncResult(response, null, true, false);
    var promise = Promise.promise();
    assertTrue(RestUtil.validateAsyncResult(noContentAsyncResult, promise));
    assertFalse(promise.future().isComplete());
  }

  @Test
  void shouldValidateBadRequestAsyncResult() {
    var response = new RestUtil.WrappedResponse(422, null, null);
    var badRequestAsyncResult = getAsyncResult(response, null, true, false);
    var promise = Promise.promise();
    assertFalse(RestUtil.validateAsyncResult(badRequestAsyncResult, promise));
    assertTrue(promise.future().failed());
    assertInstanceOf(BadRequestException.class, promise.future().cause());
  }

  @Test
  void shouldRemoveTokenHeaderWhenSystemUserDisabled(Vertx vertx, VertxTestContext context) {
    var url = mockServer.baseUrl();
    System.setProperty("SYSTEM_USER_ENABLED", "false");

    var headersMap = Map.of(
      URL, url,
      TENANT, TENANT_ID,
      TOKEN, TEST_TOKEN
    );

    assertTokenHeaderRequest(vertx, context, headersMap, "/test-endpoint", url, true,
      () -> System.clearProperty("SYSTEM_USER_ENABLED"));
  }

  @Test
  void shouldRemoveTokenHeaderCaseInsensitivelyWhenSystemUserDisabled(Vertx vertx, VertxTestContext context) {
    var url = mockServer.baseUrl();
    System.setProperty("SYSTEM_USER_ENABLED", "false");

    // Use lower-case header keys to verify token removal is case-insensitive
    var headersMap = Map.of(
      URL.toLowerCase(), url,
      TENANT.toLowerCase(), TENANT_ID,
      TOKEN.toLowerCase(), TEST_TOKEN
    );

    assertTokenHeaderRequest(vertx, context, headersMap, "/test-endpoint-lower", url, true,
      () -> System.clearProperty("SYSTEM_USER_ENABLED"));
  }

  @Test
  void shouldNotRemoveTokenHeaderWhenSystemUserPropertyMissing(Vertx vertx, VertxTestContext context) {
    var url = mockServer.baseUrl();

    var headersMap = Map.of(
      URL, url,
      TENANT, TENANT_ID,
      TOKEN, TEST_TOKEN
    );

    assertTokenHeaderRequest(vertx, context, headersMap, "/test-endpoint", url, false, null);
  }

  /**
   * Sends a GET request through {@link RestUtil#doRequestWithSystemUser} and verifies the token, tenant
   * and url headers received by the mock server, optionally running cleanup after verification.
   */
  private void assertTokenHeaderRequest(Vertx vertx, VertxTestContext context, Map<String, String> headersMap,
                                        String endpoint, String url, boolean expectTokenRemoved,
                                        Runnable cleanup) {
    vertx.runOnContext(v -> {
      var params = new ConnectionParams(headersMap);
      mockServer.stubFor(get(endpoint).willReturn(ok()));

      var future = RestUtil.doRequestWithSystemUser(params, endpoint, HttpMethod.GET, null);
      future.onComplete(response -> {
        try {
          verifyTokenHeaderRequest(context, response, endpoint, url, expectTokenRemoved);
        } finally {
          if (cleanup != null) {
            cleanup.run();
          }
        }
      });
    });
  }

  private void verifyTokenHeaderRequest(VertxTestContext context, AsyncResult<RestUtil.WrappedResponse> response,
                                        String endpoint, String url, boolean expectTokenRemoved) {
    context.verify(() -> {
      assertTrue(response.succeeded());
      assertNotNull(response.result());

      var requests = mockServer.findAll(getRequestedFor(urlEqualTo(endpoint)));
      assertEquals(1, requests.size());
      var request = requests.getFirst();

      var tokenHeader = request.getHeader(TOKEN);
      if (expectTokenRemoved) {
        assertNull(tokenHeader, "Token header should be removed when SYSTEM_USER_ENABLED is false");
      } else {
        assertEquals(TEST_TOKEN, tokenHeader);
      }
      assertEquals(TENANT_ID, request.getHeader(TENANT));
      assertEquals(url, request.getHeader(URL));

      context.completeNow();
    });
  }

  private AsyncResult<RestUtil.WrappedResponse> getAsyncResult(RestUtil.WrappedResponse result, Throwable cause,
                                                               boolean succeeded, boolean failed) {
    return new AsyncResult<>() {
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
