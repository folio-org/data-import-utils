package org.folio.dataimport.util;

import static org.folio.HttpStatus.HTTP_CREATED;
import static org.folio.HttpStatus.HTTP_INTERNAL_SERVER_ERROR;
import static org.folio.HttpStatus.HTTP_NOT_FOUND;
import static org.folio.HttpStatus.HTTP_NO_CONTENT;
import static org.folio.HttpStatus.HTTP_OK;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.okapi.common.XOkapiHeaders;

/**
 * Util class with static method for sending http request.
 */
public final class RestUtil {

  static final String SYSTEM_USER_ENV_VAR = "SYSTEM_USER_ENABLED";

  private static final Logger LOGGER = LogManager.getLogger();
  private static final String STATUS_CODE_IS_NOT_SUCCESS_MSG =
    "Response HTTP code is not equals 200, 201, 204. Response code: {}";

  private RestUtil() {
  }

  /**
   * Creates an HTTP request, removing the token header if system user is disabled.
   *
   * @param params  Okapi connection parameters.
   * @param url     Relative URL for the HTTP request.
   * @param method  HTTP method (GET, POST, etc.).
   * @param payload Body of the request.
   * @return A future representing the asynchronous HTTP response.
   */

  public static <T> Future<WrappedResponse> doRequestWithSystemUser(ConnectionParams params, String url,
                                                                    HttpMethod method, T payload) {
    var headers = params.getHeaders();
    if (isSystemUserEnabled()) {
      LOGGER.trace(
        "doRequestWithSystemUser:: Do request without {} header for system user, url: {}, method: {}, tenant: {}",
        XOkapiHeaders.TOKEN, url, method, params.getTenantId());
      headers.remove(XOkapiHeaders.TOKEN);
    }
    return doRequest0(url, method, params, headers, payload);
  }

  /**
   * Create http request.
   *
   * @param params  Okapi connection parameters.
   * @param url     Relative URL for the HTTP request.
   * @param method  HTTP method (GET, POST, etc.).
   * @param payload Body of the request.
   * @return A future representing the asynchronous HTTP response.
   */
  public static <T> Future<WrappedResponse> doRequest(ConnectionParams params, String url,
                                                      HttpMethod method, T payload) {
    return doRequest0(url, method, params, params.getHeaders(), payload);
  }

  /**
   * Checks if the system user is enabled based on a system property.
   *
   * <p>
   * This method reads the `SYSTEM_USER_ENABLED` system property and parses
   * its value as a boolean. If the property is not found or cannot be parsed,
   * it defaults to `true`. The method then negates the parsed value and returns it.
   *
   * <p>
   * Note: This functionality is specific to the Eureka environment.
   *
   * @return {@code true} if the system user is set for Eureka env; otherwise {@code false}.
   */
  public static boolean isSystemUserEnabled() {
    return !Boolean.parseBoolean(System.getenv().getOrDefault(SYSTEM_USER_ENV_VAR,
      System.getProperty(SYSTEM_USER_ENV_VAR, "true")));
  }

  /**
   * Validate http response and fail future if necessary.
   *
   * @param asyncResult - http response callback
   * @param promise     - future of callback
   * @return - boolean value is response ok
   */
  public static boolean validateAsyncResult(AsyncResult<WrappedResponse> asyncResult, Promise<?> promise) {
    boolean result = false;
    if (asyncResult.failed()) {
      LOGGER.error("Error during HTTP request: ", asyncResult.cause());
      promise.fail(asyncResult.cause());
    } else if (asyncResult.result() == null) {
      LOGGER.error("Error during get response");
      promise.fail(new BadRequestException());
    } else if (isCode(asyncResult, HTTP_NOT_FOUND)) {
      logStatusCodeNotSuccess(getCode(asyncResult));
      promise.fail(new NotFoundException());
    } else if (isCode(asyncResult, HTTP_INTERNAL_SERVER_ERROR)) {
      logStatusCodeNotSuccess(getCode(asyncResult));
      promise.fail(new InternalServerErrorException());
    } else if (isSuccess(asyncResult)) {
      result = true;
    } else {
      logStatusCodeNotSuccess(getCode(asyncResult));
      promise.fail(new BadRequestException());
    }
    return result;
  }

  private static void logStatusCodeNotSuccess(int code) {
    LOGGER.error(STATUS_CODE_IS_NOT_SUCCESS_MSG, code);
  }

  private static <T> Future<WrappedResponse> doRequest0(String url, HttpMethod method, ConnectionParams params,
                                                        Map<String, String> headers, T payload) {
    Promise<WrappedResponse> promise = Promise.promise();

    try {
      var requestUrl = params.getConnectionUrl() + url;
      var client = WebClient.wrap(getHttpClient(params));
      var request = client.requestAbs(method, requestUrl);

      Optional.ofNullable(headers)
        .ifPresent(h -> {
          h.put("Content-type", "application/json");
          h.put("Accept", "application/json, text/plain");
          h.forEach(request::putHeader);
        });

      if (method == HttpMethod.PUT || method == HttpMethod.POST) {
        var buffer = Buffer.buffer(new ObjectMapper().writeValueAsString(payload));
        request.sendBuffer(buffer).onComplete(handleResponse(promise));
      } else {
        request.send().onComplete(handleResponse(promise));
      }
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  /**
   * Prepare HttpClient from ConnectionParams.
   *
   * @param params - ConnectionParams
   * @return - Vertx Http Client
   */
  private static HttpClient getHttpClient(ConnectionParams params) {
    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(params.getTimeout());
    options.setIdleTimeout(params.getTimeout());
    return Vertx.currentContext().owner().createHttpClient(options);
  }

  private static int getCode(AsyncResult<WrappedResponse> asyncResult) {
    return asyncResult.result().getCode();
  }

  private static boolean isSuccess(AsyncResult<WrappedResponse> asyncResult) {
    return isCode(asyncResult, HTTP_OK)
           || isCode(asyncResult, HTTP_CREATED)
           || isCode(asyncResult, HTTP_NO_CONTENT);
  }

  private static boolean isCode(AsyncResult<WrappedResponse> asyncResult, HttpStatus status) {
    return getCode(asyncResult) == status.toInt();
  }

  private static Handler<AsyncResult<HttpResponse<Buffer>>> handleResponse(Promise<WrappedResponse> promise) {
    return ar -> {
      if (ar.succeeded()) {
        WrappedResponse wr = new WrappedResponse(ar.result().statusCode(), ar.result().bodyAsString(), ar.result());
        promise.complete(wr);
      } else {
        promise.fail(ar.cause());
      }
    };
  }

  public static class WrappedResponse {
    private final int code;
    private final String body;
    private final HttpResponse<Buffer> response;
    private JsonObject json;

    WrappedResponse(int code, String body,
                    HttpResponse<Buffer> response) {
      this.code = code;
      this.body = body;
      this.response = response;
      try {
        json = new JsonObject(body);
      } catch (Exception e) {
        json = null;
      }
    }

    public int getCode() {
      return code;
    }

    public String getBody() {
      return body;
    }

    public HttpResponse<Buffer> getResponse() {
      return response;
    }

    public JsonObject getJson() {
      return json;
    }
  }
}
