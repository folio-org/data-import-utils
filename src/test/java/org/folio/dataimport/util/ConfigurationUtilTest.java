package org.folio.dataimport.util;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.ConfigurationsClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.folio.dataimport.util.RestUtil.OKAPI_TENANT_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_URL_HEADER;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationUtil.class })
public class ConfigurationUtilTest {

  private String code = "data.import.storage.type";
  private String value = "LOCAL_STORAGE";
  private JsonObject config = new JsonObject().put("totalRecords", 1)
    .put("configs", new JsonArray().add(new JsonObject()
      .put("module", "DATA_IMPORT")
      .put("code", code)
      .put("value", "LOCAL_STORAGE")
    ));

  @Test
  public void shouldReturnFailedFutureWhenInvalidUrl() throws Exception {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL_HEADER, "localhost:");
    okapiHeaders.put(OKAPI_TENANT_HEADER, "diku");
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TOKEN, "token");
    OkapiConnectionParams params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());

    ConfigurationUtil.getPropertyByCode(code, params).setHandler(stringAsyncResult -> {
      assertTrue(stringAsyncResult.failed());
      assertTrue(stringAsyncResult.cause().getMessage().contains("Could not parse okapiURL: localhost"));
    });
  }

  @Test
  public void shouldReturnFailedFutureWhenExceptionIsThrown() throws Exception {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL_HEADER, "http://localhost:");
    okapiHeaders.put(OKAPI_TENANT_HEADER, "diku");
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TOKEN, "token");
    OkapiConnectionParams params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());

    ConfigurationsClient client = spy(ConfigurationsClient.class);
    whenNew(ConfigurationsClient.class)
      .withAnyArguments()
      .thenReturn(client);

    doThrow(new UnsupportedEncodingException())
      .when(client)
      .getEntries(anyString(), anyInt(), anyInt(), isNull(), isNull(), any(Handler.class));

    ConfigurationUtil.getPropertyByCode(code, params).setHandler(stringAsyncResult -> {
      assertTrue(stringAsyncResult.failed());
      assertTrue(stringAsyncResult.cause() instanceof UnsupportedEncodingException);
    });
  }

}
