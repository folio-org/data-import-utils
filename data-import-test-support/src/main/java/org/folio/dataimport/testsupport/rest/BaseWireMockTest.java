package org.folio.dataimport.testsupport.rest;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Provides a per-class WireMock server for stubbing HTTP calls made by the module under test.
 *
 * <p>Available stub helpers:
 * <ul>
 *   <li>{@link #mockServerUrl()} — the WireMock base URL to forward {@code X-Okapi-Url} to</li>
 *   <li>{@code stubGetJson} — stub a {@code GET} returning JSON (200)</li>
 *   <li>{@code stubPostJson} — stub a {@code POST} returning JSON (201 by default)</li>
 *   <li>{@code stubPutJson} — stub a {@code PUT} returning JSON (200)</li>
 *   <li>{@code stubPatchJson} — stub a {@code PATCH} returning JSON (200)</li>
 *   <li>{@code stubDelete} — stub a {@code DELETE} returning 204 No Content</li>
 *   <li>{@code stubDeleteJson} — stub a {@code DELETE} returning JSON (200)</li>
 * </ul>
 *
 * <p>Every method has a two-arg form (URL pattern + body) that uses the default status code, and a
 * three-arg form (URL pattern + status + body) for cases that need a non-default code, e.g. an
 * error response.
 */
@SuppressWarnings("java:S1118")
public abstract class BaseWireMockTest {

  @RegisterExtension
  protected static final WireMockExtension WIRE_MOCK = WireMockExtension.newInstance()
    .options(wireMockConfig().notifier(new Slf4jNotifier(true)))
    .build();

  /**
   * Returns the base URL of the per-class WireMock server used to stub calls the module under
   * test makes to other modules (e.g. mod-users).
   *
   * @return the WireMock base URL
   */
  protected static String mockServerUrl() {
    return WIRE_MOCK.baseUrl();
  }

  // ── GET ──────────────────────────────────────────────────────────────────────

  /**
   * Stubs a {@code GET} on the shared WireMock server to return {@code 200} with the given JSON.
   *
   * @param urlPattern       URL/URL-pattern to match, e.g. {@code "/users/.*"}
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubGetJson(String urlPattern, String jsonResponseBody) {
    WIRE_MOCK.stubFor(WireMock.get(WireMock.urlMatching(urlPattern))
      .willReturn(WireMock.okJson(jsonResponseBody)));
  }

  /**
   * Stubs a {@code GET} on the shared WireMock server to return the given status and JSON body.
   *
   * @param urlPattern       URL/URL-pattern to match
   * @param status           HTTP status code to return
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubGetJson(String urlPattern, int status, String jsonResponseBody) {
    WIRE_MOCK.stubFor(WireMock.get(WireMock.urlMatching(urlPattern))
      .willReturn(jsonResponse(status, jsonResponseBody)));
  }

  // ── POST ─────────────────────────────────────────────────────────────────────

  /**
   * Stubs a {@code POST} on the shared WireMock server to return {@code 201} with the given JSON.
   *
   * @param urlPattern       URL/URL-pattern to match
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubPostJson(String urlPattern, String jsonResponseBody) {
    stubPostJson(urlPattern, 201, jsonResponseBody);
  }

  /**
   * Stubs a {@code POST} on the shared WireMock server to return the given status and JSON body.
   *
   * @param urlPattern       URL/URL-pattern to match
   * @param status           HTTP status code to return
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubPostJson(String urlPattern, int status, String jsonResponseBody) {
    WIRE_MOCK.stubFor(WireMock.post(WireMock.urlMatching(urlPattern))
      .willReturn(jsonResponse(status, jsonResponseBody)));
  }

  // ── PUT ──────────────────────────────────────────────────────────────────────

  /**
   * Stubs a {@code PUT} on the shared WireMock server to return {@code 200} with the given JSON.
   *
   * @param urlPattern       URL/URL-pattern to match
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubPutJson(String urlPattern, String jsonResponseBody) {
    stubPutJson(urlPattern, 200, jsonResponseBody);
  }

  /**
   * Stubs a {@code PUT} on the shared WireMock server to return the given status and JSON body.
   *
   * @param urlPattern       URL/URL-pattern to match
   * @param status           HTTP status code to return
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubPutJson(String urlPattern, int status, String jsonResponseBody) {
    WIRE_MOCK.stubFor(WireMock.put(WireMock.urlMatching(urlPattern))
      .willReturn(jsonResponse(status, jsonResponseBody)));
  }

  // ── PATCH ────────────────────────────────────────────────────────────────────

  /**
   * Stubs a {@code PATCH} on the shared WireMock server to return {@code 200} with the given JSON.
   *
   * @param urlPattern       URL/URL-pattern to match
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubPatchJson(String urlPattern, String jsonResponseBody) {
    stubPatchJson(urlPattern, 200, jsonResponseBody);
  }

  /**
   * Stubs a {@code PATCH} on the shared WireMock server to return the given status and JSON body.
   *
   * @param urlPattern       URL/URL-pattern to match
   * @param status           HTTP status code to return
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubPatchJson(String urlPattern, int status, String jsonResponseBody) {
    WIRE_MOCK.stubFor(WireMock.patch(WireMock.urlMatching(urlPattern))
      .willReturn(jsonResponse(status, jsonResponseBody)));
  }

  // ── DELETE ───────────────────────────────────────────────────────────────────

  /**
   * Stubs a {@code DELETE} on the shared WireMock server to return {@code 204 No Content}.
   *
   * @param urlPattern URL/URL-pattern to match
   */
  protected static void stubDelete(String urlPattern) {
    WIRE_MOCK.stubFor(WireMock.delete(WireMock.urlMatching(urlPattern))
      .willReturn(WireMock.aResponse().withStatus(204)));
  }

  /**
   * Stubs a {@code DELETE} on the shared WireMock server to return {@code 200} with the given
   * JSON. Use when the stubbed service returns a body on deletion.
   *
   * @param urlPattern       URL/URL-pattern to match
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubDeleteJson(String urlPattern, String jsonResponseBody) {
    stubDeleteJson(urlPattern, 200, jsonResponseBody);
  }

  /**
   * Stubs a {@code DELETE} on the shared WireMock server to return the given status and JSON body.
   *
   * @param urlPattern       URL/URL-pattern to match
   * @param status           HTTP status code to return
   * @param jsonResponseBody JSON body returned with the response
   */
  protected static void stubDeleteJson(String urlPattern, int status, String jsonResponseBody) {
    WIRE_MOCK.stubFor(WireMock.delete(WireMock.urlMatching(urlPattern))
      .willReturn(jsonResponse(status, jsonResponseBody)));
  }

  // ─────────────────────────────────────────────────────────────────────────────

  private static ResponseDefinitionBuilder jsonResponse(int status, String body) {
    return WireMock.aResponse()
      .withStatus(status)
      .withHeader("Content-Type", "application/json")
      .withBody(body);
  }
}
