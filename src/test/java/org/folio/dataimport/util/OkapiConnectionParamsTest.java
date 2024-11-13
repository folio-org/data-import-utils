package org.folio.dataimport.util;

import io.vertx.core.Vertx;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.folio.dataimport.util.RestUtil.OKAPI_TENANT_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_TOKEN_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_URL_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OkapiConnectionParamsTest {

  @Test
  public void testCreateSystemUserConnectionParams_WithSystemUserDisabled() {
    System.setProperty("SYSTEM_USER_ENABLED", "false");

    Map<String, String> headersMap = new HashMap<>();
    headersMap.put(OKAPI_URL_HEADER, "http://localhost");
    headersMap.put(OKAPI_TENANT_HEADER, "diku");
    headersMap.put(OKAPI_TOKEN_HEADER, "dummy_token");

    var mockVertx = Mockito.mock(Vertx.class);

    var params = OkapiConnectionParams.createSystemUserConnectionParams(headersMap, mockVertx);

    assertEquals("http://localhost", params.getOkapiUrl());
    assertEquals("diku", params.getTenantId());
    assertEquals(params.getToken(), "");

    System.clearProperty("SYSTEM_USER_ENABLED");
  }

  @Test
  public void testCreateSystemUserConnectionParams_WithSystemUserEnabled() {
    System.setProperty("SYSTEM_USER_ENABLED", "true");

    Map<String, String> headersMap = new HashMap<>();
    headersMap.put(OKAPI_URL_HEADER, "http://localhost");
    headersMap.put(OKAPI_TENANT_HEADER, "diku");
    headersMap.put(OKAPI_TOKEN_HEADER, "dummy_token");

    var mockVertx = Mockito.mock(Vertx.class);

    var params = OkapiConnectionParams.createSystemUserConnectionParams(headersMap, mockVertx);

    assertEquals("http://localhost", params.getOkapiUrl());
    assertEquals("diku", params.getTenantId());
    assertEquals("dummy_token", params.getToken());

    System.clearProperty("SYSTEM_USER_ENABLED");
  }

}
