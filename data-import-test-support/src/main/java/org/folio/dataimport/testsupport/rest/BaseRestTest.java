package org.folio.dataimport.testsupport.rest;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.folio.dataimport.testsupport.kafka.KafkaExtension;
import org.folio.dataimport.testsupport.postgres.PostgresExtension;
import org.folio.dataimport.testsupport.tenant.TenantTestSupport;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Base class for raml-module-builder integration tests that need a running module.
 *
 * <p>Boots shared PostgreSQL and Kafka containers, deploys the standard raml-module-builder
 * {@link RestVerticle} on a random free port and runs the Tenant API so the module schema is
 * created before the first test. The deployment logic is identical for every raml-module-builder
 * module, so subclasses only provide the module id through {@link #getModuleName()}; the deployed
 * module is then reachable through {@link #connectionUrl} on {@link #port}.
 *
 * <p>Also starts a per-class WireMock server (reachable via {@link #mockServerUrl()} and stubbed
 * through {@link #stubGetJson(String, String)}) and pre-builds a RestAssured {@link #spec} that
 * points at the deployed module, carries the tenant/token headers and forwards the Okapi URL
 * header to the WireMock server, so other modules called by the module under test can be stubbed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseRestTest {

  @RegisterExtension
  protected static final PostgresExtension POSTGRES = new PostgresExtension();

  @RegisterExtension
  protected static final KafkaExtension KAFKA = new KafkaExtension();

  @RegisterExtension
  protected static final WireMockExtension WIRE_MOCK = WireMockExtension.newInstance()
    .options(wireMockConfig().notifier(new Slf4jNotifier(true)))
    .build();

  private static final int STARTUP_TIMEOUT_SECONDS = 60;
  private static final String DEFAULT_TENANT = "diku";

  protected Vertx vertx;
  protected int port;
  protected String connectionUrl;
  protected RequestSpecification spec;

  /**
   * Returns the module id used as the {@code moduleTo} value for the Tenant API,
   * e.g. {@code mod-data-import-1.0.0}.
   *
   * @return the target module id
   */
  protected abstract String getModuleName();

  /**
   * Returns the tenant enabled before the tests run. Defaults to {@code diku}.
   *
   * @return the tenant identifier
   */
  protected String getTenantId() {
    return DEFAULT_TENANT;
  }

  /**
   * Returns the Okapi token used for the Tenant API call. Defaults to {@code null}.
   *
   * @return the Okapi token, or {@code null}
   */
  protected String getToken() {
    return null;
  }

  /**
   * Returns the attributes posted to the Tenant API. Defaults to enabling {@link #getModuleName()}.
   *
   * @return the tenant attributes
   */
  protected TenantAttributes getTenantAttributes() {
    return new TenantAttributes().withModuleTo(getModuleName());
  }

  /**
   * Returns extra headers merged into the pre-built RestAssured {@link #spec}, in addition to the
   * tenant, token and {@code X-Okapi-Url} headers set by default. Defaults to none. Override to add
   * e.g. an {@code X-Okapi-UserId} header expected by the module under test.
   *
   * @return the extra headers, keyed by header name
   */
  protected Map<String, String> getExtraSpecHeaders() {
    return Map.of();
  }

  /**
   * Returns the base URL of the per-class WireMock server used to stub calls the module under
   * test makes to other modules (e.g. mod-users).
   *
   * @return the WireMock base URL
   */
  protected static String mockServerUrl() {
    return WIRE_MOCK.baseUrl();
  }

  /**
   * Stubs a {@code GET} request on the shared WireMock server to return the given JSON body.
   *
   * @param urlPattern       the request URL/URL pattern to match, e.g. {@code "/users?query=id==.*"}
   * @param jsonResponseBody the JSON response body to return with a {@code 200} status
   */
  protected static void stubGetJson(String urlPattern, String jsonResponseBody) {
    WIRE_MOCK.stubFor(WireMock.get(WireMock.urlMatching(urlPattern))
      .willReturn(WireMock.okJson(jsonResponseBody)));
  }

  /**
   * Returns a pre-configured RestAssured request builder, spec-ed with {@link #spec}.
   *
   * @return a new request specification ready to chain a call on
   */
  protected RequestSpecification given() {
    return RestAssured.given().spec(spec);
  }

  /**
   * Posts {@code body} to {@code path}, asserts the response status and returns the deserialized
   * response body.
   *
   * @param path           the request path
   * @param body           the request body
   * @param expectedStatus the expected HTTP status code
   * @param responseType   the type to deserialize the response body as, or {@code null} if the
   *                       response body should be ignored
   * @param <T>            the response entity type
   * @return the deserialized response body, or {@code null} if {@code responseType} is {@code null}
   */
  protected <T> T postEntity(String path, Object body, int expectedStatus, Class<T> responseType) {
    var response = given().body(body).when().log().all(true).post(path).then().statusCode(expectedStatus).extract();
    return responseType == null ? null : response.as(responseType);
  }

  /**
   * Puts {@code body} to {@code path}, asserts the response status and returns the deserialized
   * response body.
   *
   * @param path           the request path
   * @param body           the request body
   * @param expectedStatus the expected HTTP status code
   * @param responseType   the type to deserialize the response body as, or {@code null} if the
   *                       response body should be ignored
   * @param <T>            the response entity type
   * @return the deserialized response body, or {@code null} if {@code responseType} is {@code null}
   */
  protected <T> T putEntity(String path, Object body, int expectedStatus, Class<T> responseType) {
    var response = given().body(body).when().log().all(true).put(path).then().statusCode(expectedStatus).extract();
    return responseType == null ? null : response.as(responseType);
  }

  /**
   * Gets {@code path}, asserts the response status and returns the deserialized response body.
   *
   * @param path           the request path
   * @param expectedStatus the expected HTTP status code
   * @param responseType   the type to deserialize the response body as
   * @param <T>            the response entity type
   * @return the deserialized response body
   */
  protected <T> T getEntity(String path, int expectedStatus, Class<T> responseType) {
    return given().when().log().all(true).get(path).then().statusCode(expectedStatus).extract().as(responseType);
  }

  /**
   * Deletes {@code path} and asserts the response status.
   *
   * @param path           the request path
   * @param expectedStatus the expected HTTP status code
   */
  protected void deleteEntity(String path, int expectedStatus) {
    given().when().log().all(true).delete(path).then().statusCode(expectedStatus);
  }

  /**
   * Posts {@code body} to {@code path} and returns the response for further assertions/extraction,
   * e.g. {@code postRequest(path, body).statusCode(201).body("name", is("foo"))}.
   *
   * @param path the request path
   * @param body the request body
   * @return the validatable response
   */
  protected ValidatableResponse postRequest(String path, Object body) {
    return given().body(body).when().log().all(true).post(path).then();
  }

  /**
   * Posts {@code body} to {@code path} with the given query parameters and returns the response
   * for further assertions/extraction.
   *
   * @param path        the request path
   * @param body        the request body
   * @param queryParams the query parameters, keyed by parameter name
   * @return the validatable response
   */
  protected ValidatableResponse postRequest(String path, Object body, Map<String, ?> queryParams) {
    return given().queryParams(queryParams).body(body).when().log().all(true).post(path).then();
  }

  /**
   * Puts {@code body} to {@code path} and returns the response for further assertions/extraction.
   *
   * @param path the request path
   * @param body the request body
   * @return the validatable response
   */
  protected ValidatableResponse putRequest(String path, Object body) {
    return given().body(body).when().log().all(true).put(path).then();
  }

  /**
   * Puts {@code body} to {@code path} with the given query parameters and returns the response
   * for further assertions/extraction.
   *
   * @param path        the request path
   * @param body        the request body
   * @param queryParams the query parameters, keyed by parameter name
   * @return the validatable response
   */
  protected ValidatableResponse putRequest(String path, Object body, Map<String, ?> queryParams) {
    return given().queryParams(queryParams).body(body).when().log().all(true).put(path).then();
  }

  /**
   * Gets {@code path} and returns the response for further assertions/extraction.
   *
   * @param path the request path
   * @return the validatable response
   */
  protected ValidatableResponse getRequest(String path) {
    return given().when().log().all(true).get(path).then();
  }

  /**
   * Gets {@code path} with the given query parameters and returns the response for further
   * assertions/extraction, e.g. {@code getRequest(path, Map.of("query", cql)).statusCode(200)}.
   *
   * @param path        the request path
   * @param queryParams the query parameters, keyed by parameter name
   * @return the validatable response
   */
  protected ValidatableResponse getRequest(String path, Map<String, ?> queryParams) {
    return given().queryParams(queryParams).when().log().all(true).get(path).then();
  }

  /**
   * Deletes {@code path} and returns the response for further assertions/extraction.
   *
   * @param path the request path
   * @return the validatable response
   */
  protected ValidatableResponse deleteRequest(String path) {
    return given().when().log().all(true).delete(path).then();
  }

  /**
   * Deletes {@code path} with the given query parameters and returns the response for further
   * assertions/extraction.
   *
   * @param path        the request path
   * @param queryParams the query parameters, keyed by parameter name
   * @return the validatable response
   */
  protected ValidatableResponse deleteRequest(String path, Map<String, ?> queryParams) {
    return given().queryParams(queryParams).when().log().all(true).delete(path).then();
  }

  @BeforeAll
  void deployRestVerticle() {
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();
    connectionUrl = "http://localhost:" + port;

    var options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
    await(vertx.deployVerticle(RestVerticle.class.getName(), options));
    await(TenantTestSupport.enableTenant(vertx, connectionUrl, getTenantId(), getToken(), getTenantAttributes()));
    spec = buildRequestSpec();
  }

  @AfterAll
  void undeployRestVerticle() {
    if (vertx != null) {
      await(vertx.close());
    }
  }

  private RequestSpecification buildRequestSpec() {
    var specBuilder = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .setBaseUri(connectionUrl)
      .addHeader(XOkapiHeaders.TENANT, getTenantId())
      .addHeader(XOkapiHeaders.URL, mockServerUrl());
    if (getToken() != null) {
      specBuilder.addHeader(XOkapiHeaders.TOKEN, getToken());
    }
    getExtraSpecHeaders().forEach(specBuilder::addHeader);
    return specBuilder.build();
  }

  private static <T> T await(Future<T> future) {
    try {
      return future.toCompletionStage().toCompletableFuture().get(STARTUP_TIMEOUT_SECONDS, SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while awaiting async test setup", e);
    } catch (ExecutionException | TimeoutException e) {
      throw new IllegalStateException("Async test setup failed", e);
    }
  }
}
