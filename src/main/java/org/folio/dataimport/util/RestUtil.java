package org.folio.dataimport.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.HttpStatus;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.util.Map;

import static org.folio.HttpStatus.HTTP_CREATED;
import static org.folio.HttpStatus.HTTP_INTERNAL_SERVER_ERROR;
import static org.folio.HttpStatus.HTTP_NOT_FOUND;
import static org.folio.HttpStatus.HTTP_NO_CONTENT;
import static org.folio.HttpStatus.HTTP_OK;

/**
 * Util class with static method for sending http request
 */
public final class RestUtil {

  public static final String OKAPI_TENANT_HEADER = "x-okapi-tenant";
  public static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  private static final Logger LOGGER = LoggerFactory.getLogger(RestUtil.class);
  private static final String STATUS_CODE_IS_NOT_SUCCESS_MSG = "Response HTTP code is not equals 200, 201, 204. Response code: {}";

  public static class WrappedResponse {
    private int code;
    private String body;
    private JsonObject json;
    private HttpClientResponse response;

    WrappedResponse(int code, String body,
                    HttpClientResponse response) {
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

    public HttpClientResponse getResponse() {
      return response;
    }

    public JsonObject getJson() {
      return json;
    }
  }

  private RestUtil() {
  }

  /**
   * Create http request
   *
   * @param url     - url for http request
   * @param method  - http method
   * @param payload - body of request
   * @return - async http response
   */
  public static <T> Future<WrappedResponse> doRequest(OkapiConnectionParams params, String url,
                                                      HttpMethod method, T payload) {
    Promise<WrappedResponse> promise = Promise.promise();
    try {
      MultiMap headers = params.getHeaders();
      String requestUrl = params.getOkapiUrl() + url;
      HttpClientRequest request = getHttpClient(params).requestAbs(method, requestUrl);
      if (headers != null) {
        headers.add("Content-type", "application/json")
          .add("Accept", "application/json, text/plain");
        for (Map.Entry entry : headers.entries()) {
          request.putHeader((String) entry.getKey(), (String) entry.getValue());
        }
      }
      request.exceptionHandler(promise::fail);
      request.handler(req -> req.bodyHandler(buf -> {
        WrappedResponse wr = new WrappedResponse(req.statusCode(), buf.toString(), req);
        promise.complete(wr);
      }));
      if (method == HttpMethod.PUT || method == HttpMethod.POST) {
        request.end(new ObjectMapper().writeValueAsString(payload));
      } else {
        request.end();
      }
      return promise.future();
    } catch (Exception e) {
      promise.fail(e);
      return promise.future();
    }
  }

  /**
   * Prepare HttpClient from OkapiConnection params
   *
   * @param params - Okapi connection params
   * @return - Vertx Http Client
   */
  private static HttpClient getHttpClient(OkapiConnectionParams params) {
    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(params.getTimeout());
    options.setIdleTimeout(params.getTimeout());
    return Vertx.currentContext().owner().createHttpClient(options);
  }

  /**
   * Validate http response and fail future if necessary
   *
   * @param asyncResult - http response callback
   * @param promise     - future of callback
   * @return - boolean value is response ok
   */
  public static boolean validateAsyncResult(AsyncResult<WrappedResponse> asyncResult, Promise promise) {
    boolean result = false;
    if (asyncResult.failed()) {
      LOGGER.error("Error during HTTP request: {}", asyncResult.cause());
      promise.fail(asyncResult.cause());
    } else if (asyncResult.result() == null) {
      LOGGER.error("Error during get response");
      promise.fail(new BadRequestException());
    } else if (isCode(asyncResult, HTTP_NOT_FOUND)) {
      LOGGER.error(STATUS_CODE_IS_NOT_SUCCESS_MSG, getCode(asyncResult));
      promise.fail(new NotFoundException());
    } else if (isCode(asyncResult, HTTP_INTERNAL_SERVER_ERROR)) {
      LOGGER.error(STATUS_CODE_IS_NOT_SUCCESS_MSG, getCode(asyncResult));
      promise.fail(new InternalServerErrorException());
    } else if (isSuccess(asyncResult)) {
      result = true;
    } else {
      LOGGER.error(STATUS_CODE_IS_NOT_SUCCESS_MSG, getCode(asyncResult));
      promise.fail(new BadRequestException());
    }
    return result;
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
}
