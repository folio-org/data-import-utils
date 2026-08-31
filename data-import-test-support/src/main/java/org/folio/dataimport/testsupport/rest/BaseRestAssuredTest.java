package org.folio.dataimport.testsupport.rest;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.Map;

/**
 * Extends {@link BaseWireMockTest} with a pre-configured RestAssured request spec and HTTP
 * convenience methods.
 *
 * <p>The {@link #spec} field is populated by calling {@link #buildSpec()} — either directly in a
 * subclass {@code @BeforeAll}, or automatically by subclasses such as {@link BaseRestTest} that
 * manage their own lifecycle. Override {@link #buildSpec()} to customise the spec; the default
 * produces a JSON-content-type-only spec with no base URI or headers.
 *
 * <p>All HTTP helper methods delegate to {@link #given()}, which wraps {@link #spec}.
 */
public abstract class BaseRestAssuredTest extends BaseWireMockTest {

  protected RequestSpecification spec;

  /**
   * Builds the RestAssured {@link RequestSpecification} used by all HTTP helper methods.
   *
   * <p>The default implementation creates a spec with {@code Content-Type: application/json} only.
   * Subclasses should override this to add a base URI, authentication headers, or any other
   * request defaults needed for the module under test.
   *
   * <p>This method is called once per test class (typically from a {@code @BeforeAll} method after
   * any required infrastructure — e.g. a running server — has been started).
   *
   * @return the request specification to assign to {@link #spec}
   */
  protected RequestSpecification buildSpec() {
    return new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .build();
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
    var response = given().body(body).when().log().all(true).post(path)
      .then().log().ifValidationFails()
      .statusCode(expectedStatus).extract();
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
    var response = given().body(body).when().log().all(true).put(path)
      .then().log().ifValidationFails()
      .statusCode(expectedStatus).extract();
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
    return given().when().log().all(true).get(path)
      .then().log().ifValidationFails()
      .statusCode(expectedStatus).extract().as(responseType);
  }

  /**
   * Deletes {@code path} and asserts the response status.
   *
   * @param path           the request path
   * @param expectedStatus the expected HTTP status code
   */
  protected void deleteEntity(String path, int expectedStatus) {
    given().when().log().all(true).delete(path)
      .then().log().ifValidationFails()
      .statusCode(expectedStatus);
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
    return given().body(body).when().log().all(true).post(path)
      .then().log().ifValidationFails();
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
    return given().queryParams(queryParams).body(body).when().log().all(true).post(path)
      .then().log().ifValidationFails();
  }

  /**
   * Puts {@code body} to {@code path} and returns the response for further assertions/extraction.
   *
   * @param path the request path
   * @param body the request body
   * @return the validatable response
   */
  protected ValidatableResponse putRequest(String path, Object body) {
    return given().body(body).when().log().all(true).put(path)
      .then().log().ifValidationFails();
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
    return given().queryParams(queryParams).body(body).when().log().all(true).put(path)
      .then().log().ifValidationFails();
  }

  /**
   * Gets {@code path} and returns the response for further assertions/extraction.
   *
   * @param path the request path
   * @return the validatable response
   */
  protected ValidatableResponse getRequest(String path) {
    return given().when().log().all(true).get(path)
      .then().log().ifValidationFails();
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
    return given().queryParams(queryParams).when().log().all(true).get(path)
      .then().log().ifValidationFails();
  }

  /**
   * Deletes {@code path} and returns the response for further assertions/extraction.
   *
   * @param path the request path
   * @return the validatable response
   */
  protected ValidatableResponse deleteRequest(String path) {
    return given().when().log().all(true).delete(path)
      .then().log().ifValidationFails();
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
    return given().queryParams(queryParams).when().log().all(true).delete(path)
      .then().log().ifValidationFails();
  }
}
