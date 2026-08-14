package org.folio.dataimport.util;

import static org.folio.dataimport.util.RestUtil.isSystemUserEnabled;

import java.util.Map;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.XOkapiHeaders;

/**
 * Wrapper class for Okapi connection params.
 */
public final class ConnectionParams {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final int DEF_TIMEOUT = 30000;
  private final String connectionUrl;
  private final String tenantId;
  private final String token;
  private final Integer timeout;
  private Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

  public ConnectionParams(Map<String, String> headers, Integer timeout) {
    this.headers.putAll(headers);
    this.connectionUrl = this.headers.get(XOkapiHeaders.URL);
    this.tenantId = this.headers.get(XOkapiHeaders.TENANT);
    this.token = this.headers.getOrDefault(XOkapiHeaders.TOKEN, "");
    this.timeout = timeout != null ? timeout : DEF_TIMEOUT;
  }

  public ConnectionParams(Map<String, String> headers) {
    this(headers, null);
  }

  public static ConnectionParams createSystemUserConnectionParams(Map<String, String> headers) {
    Map<String, String> headersMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headersMap.putAll(headers);
    if (isSystemUserEnabled()) {
      var tenant = headers.get(XOkapiHeaders.TENANT);
      LOGGER.trace(
        "createSystemUserConnectionParams:: Creating connection params without token for system user, tenant: {}",
        tenant);
      headersMap.remove(XOkapiHeaders.TOKEN);
    }
    return new ConnectionParams(headersMap);
  }

  public String getConnectionUrl() {
    return connectionUrl;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getToken() {
    return token;
  }

  public int getTimeout() {
    return timeout;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }
}
