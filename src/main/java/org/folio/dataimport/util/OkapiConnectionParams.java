package org.folio.dataimport.util;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;

import java.util.Map;

import static org.folio.dataimport.util.RestUtil.OKAPI_TENANT_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_TOKEN_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_URL_HEADER;

/**
 * Wrapper class for Okapi connection params
 */
public final class OkapiConnectionParams {

  private static final int DEF_TIMEOUT = 30000;
  private final String okapiUrl;
  private final String tenantId;
  private final String token;
  @Deprecated
  private final Vertx vertx;
  private final Integer timeout;
  private MultiMap headers = new HeadersMultiMap();

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

  @Deprecated
  public Vertx getVertx() {
    //TODO: in all places where you need Vertx instance use Vertx.currentContext().owner()
    //Do not ever use Vertx.vertx(); it create a completely new Vertx instance
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
