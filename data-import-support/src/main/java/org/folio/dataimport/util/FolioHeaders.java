package org.folio.dataimport.util;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.folio.okapi.common.XOkapiHeaders;

/**
 * Fluent builder for the Okapi headers commonly required by FOLIO integration tests.
 *
 * <p>Collects the tenant, token, URL and (for Enhanced Consortia Support) user identifier headers
 * into an immutable map that can be passed to a REST client or attached to a Kafka record.
 */
public final class FolioHeaders {

  private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

  private FolioHeaders() { }

  public static FolioHeaders from(Map<String, String> headers) {
    var headersMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    headersMap.putAll(headers);
    return new FolioHeaders()
      .token(headersMap.get(XOkapiHeaders.TOKEN))
      .connectionUrl(headersMap.get(XOkapiHeaders.URL))
      .tenant(headersMap.get(XOkapiHeaders.TENANT))
      .userId(headersMap.get(XOkapiHeaders.USER_ID))
      .requestId(headersMap.get(XOkapiHeaders.REQUEST_ID));
  }

  /**
   * Creates an empty builder.
   *
   * @return a new builder instance
   */
  public static FolioHeaders builder() {
    return new FolioHeaders();
  }

  public FolioHeaders connectionUrl(String url) {
    if (url != null) {
      headers.put(XOkapiHeaders.URL, url);
    }
    return this;
  }

  public FolioHeaders tenant(String tenantId) {
    if (tenantId != null) {
      headers.put(XOkapiHeaders.TENANT, tenantId);
    }
    return this;
  }

  public FolioHeaders token(String token) {
    if (token != null) {
      headers.put(XOkapiHeaders.TOKEN, token);
    }
    return this;
  }

  public FolioHeaders userId(String userId) {
    if (userId != null) {
      headers.put(XOkapiHeaders.USER_ID, userId);
    }
    return this;
  }

  public FolioHeaders requestId(String requestId) {
    if (requestId != null) {
      headers.put(XOkapiHeaders.REQUEST_ID, requestId);
    }
    return this;
  }

  public Optional<String> getUserId() {
    return Optional.ofNullable(headers.get(XOkapiHeaders.USER_ID));
  }

  public Optional<String> getRequestId() {
    return Optional.ofNullable(headers.get(XOkapiHeaders.REQUEST_ID));
  }

  public Optional<String> getTenantId() {
    return Optional.ofNullable(headers.get(XOkapiHeaders.TENANT));
  }

  public Optional<String> getConnectionUrl() {
    return Optional.ofNullable(headers.get(XOkapiHeaders.URL));
  }

  public Optional<String> getToken() {
    return Optional.ofNullable(headers.get(XOkapiHeaders.TOKEN));
  }

  /**
   * Builds an immutable copy of the collected headers.
   *
   * @return the header map
   */
  public Map<String, String> buildMap() {
    return Map.copyOf(headers);
  }
}
