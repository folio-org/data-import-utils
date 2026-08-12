package org.folio.dataimport.testsupport.tenant;

import java.util.LinkedHashMap;
import java.util.Map;
import org.folio.okapi.common.XOkapiHeaders;

/**
 * Fluent builder for the Okapi headers commonly required by FOLIO integration tests.
 *
 * <p>Collects the tenant, token, URL and (for Enhanced Consortia Support) user identifier headers
 * into an immutable map that can be passed to a REST client or attached to a Kafka record.
 */
public final class FolioHeaders {

  private final Map<String, String> headers = new LinkedHashMap<>();

  private FolioHeaders() {
  }

  /**
   * Creates an empty builder.
   *
   * @return a new builder instance
   */
  public static FolioHeaders builder() {
    return new FolioHeaders();
  }

  public FolioHeaders url(String url) {
    headers.put(XOkapiHeaders.URL, url);
    return this;
  }

  public FolioHeaders tenant(String tenantId) {
    headers.put(XOkapiHeaders.TENANT, tenantId);
    return this;
  }

  public FolioHeaders token(String token) {
    headers.put(XOkapiHeaders.TOKEN, token);
    return this;
  }

  public FolioHeaders userId(String userId) {
    headers.put(XOkapiHeaders.USER_ID, userId);
    return this;
  }

  public FolioHeaders requestId(String requestId) {
    headers.put(XOkapiHeaders.REQUEST_ID, requestId);
    return this;
  }

  /**
   * Builds an immutable copy of the collected headers.
   *
   * @return the header map
   */
  public Map<String, String> build() {
    return Map.copyOf(headers);
  }
}
