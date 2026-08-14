package org.folio.dataimport.util;

import static org.folio.dataimport.util.RestUtil.SYSTEM_USER_ENV_VAR;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.TOKEN;
import static org.folio.okapi.common.XOkapiHeaders.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ConnectionParamsTest {

  private static final String TEST_URL = "http://localhost";
  private static final String TEST_TENANT = "diku";
  private static final String TEST_TOKEN = "dummy_token";

  @Test
  void testCreateSystemUserConnectionParams_WithSystemUserDisabled() {
    disableSystemUser();

    var headersMap = Map.of(
      URL, TEST_URL,
      TENANT, TEST_TENANT,
      TOKEN, TEST_TOKEN
    );

    var params = ConnectionParams.createSystemUserConnectionParams(headersMap);

    assertEquals(TEST_URL, params.getConnectionUrl());
    assertEquals(TEST_TENANT, params.getTenantId());
    assertEquals("", params.getToken());

    clearSystemUserProperty();
  }

  @Test
  void testCreateSystemUserConnectionParams_WithSystemUserEnabled() {
    enableSystemUser();

    var headersMap = Map.of(
      URL, TEST_URL,
      TENANT, TEST_TENANT,
      TOKEN, TEST_TOKEN
    );

    var params = ConnectionParams.createSystemUserConnectionParams(headersMap);

    assertEquals(TEST_URL, params.getConnectionUrl());
    assertEquals(TEST_TENANT, params.getTenantId());
    assertEquals(TEST_TOKEN, params.getToken());

    clearSystemUserProperty();
  }

  @Test
  void testConstructor_headerLookupIsCaseInsensitive() {
    // Use lower-case header keys to verify field extraction is case-insensitive
    var headersMap = Map.of(
      URL.toLowerCase(), TEST_URL,
      TENANT.toLowerCase(), TEST_TENANT,
      TOKEN.toLowerCase(), TEST_TOKEN
    );

    var params = new ConnectionParams(headersMap);

    assertEquals(TEST_URL, params.getConnectionUrl());
    assertEquals(TEST_TENANT, params.getTenantId());
    assertEquals(TEST_TOKEN, params.getToken());
  }

  @Test
  void testGetHeaders_lookupIsCaseInsensitiveRegardlessOfInputKeyCase() {
    var headersMap = Map.of(
      URL, TEST_URL,
      TENANT, TEST_TENANT,
      TOKEN, TEST_TOKEN
    );

    var params = new ConnectionParams(headersMap);

    var headers = params.getHeaders();
    assertEquals(TEST_URL, headers.get(URL.toLowerCase()));
    assertEquals(TEST_TENANT, headers.get(TENANT.toUpperCase()));
    assertEquals(TEST_TOKEN, headers.get(TOKEN.toLowerCase()));
  }

  @Test
  void testCreateSystemUserConnectionParams_WithSystemUserDisabled_CaseInsensitiveTokenKey() {
    disableSystemUser();

    // Use a lower-case token header key to verify removal is case-insensitive
    var headersMap = Map.of(
      URL.toLowerCase(), TEST_URL,
      TENANT.toLowerCase(), TEST_TENANT,
      TOKEN.toLowerCase(), TEST_TOKEN
    );

    var params = ConnectionParams.createSystemUserConnectionParams(headersMap);

    assertEquals(TEST_URL, params.getConnectionUrl());
    assertEquals(TEST_TENANT, params.getTenantId());
    assertEquals("", params.getToken());
    assertNull(params.getHeaders().get(TOKEN.toUpperCase()));

    clearSystemUserProperty();
  }

  private void enableSystemUser() {
    System.setProperty(SYSTEM_USER_ENV_VAR, "true");
  }

  private void clearSystemUserProperty() {
    System.clearProperty(SYSTEM_USER_ENV_VAR);
  }

  private void disableSystemUser() {
    System.setProperty(SYSTEM_USER_ENV_VAR, "false");
  }
}
