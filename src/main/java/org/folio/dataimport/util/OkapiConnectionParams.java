package org.folio.dataimport.util;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;

import java.util.Map;

import static org.folio.dataimport.util.RestUtil.OKAPI_TENANT_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_TOKEN_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_URL_HEADER;

/**
 * Wrapper class for Okapi connection params
 */
public final class OkapiConnectionParams {

  private static final int DEF_TIMEOUT = 2000;
  private String okapiUrl;
  private String tenantId;
  private String token;
  private Vertx vertx;
  private Integer timeout;
  private MultiMap headers = new VertxHttpHeaders();

  public OkapiConnectionParams(Map<String, String> okapiHeaders, Vertx vertx, Integer timeout) {
    this.okapiUrl = okapiHeaders.getOrDefault(OKAPI_URL_HEADER, "localhost");
    this.tenantId = okapiHeaders.getOrDefault(OKAPI_TENANT_HEADER, "");
    this.token = okapiHeaders.getOrDefault(OKAPI_TOKEN_HEADER, "dummy");
    this.vertx = vertx;
    this.timeout = timeout != null ? timeout : DEF_TIMEOUT;
    this.headers.addAll(okapiHeaders);
  }

  public OkapiConnectionParams(Map<String, String> okapiHeaders, Vertx vertx) {
    this(okapiHeaders, vertx, null);
  }

  public String getOkapiUrl() {
    return okapiUrl;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getToken() {
    return token;
  }

  public Vertx getVertx() {
    return vertx;
  }

  public int getTimeout() {
    return timeout;
  }

  public MultiMap getHeaders() {
    return headers;
  }

  public void setHeaders(MultiMap headers) {
    this.headers = headers;
  }
}
