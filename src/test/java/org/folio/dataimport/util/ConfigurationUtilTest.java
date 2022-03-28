package org.folio.dataimport.util;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.RestVerticle;
import org.folio.util.PercentCodec;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.folio.dataimport.util.RestUtil.OKAPI_TENANT_HEADER;
import static org.folio.dataimport.util.RestUtil.OKAPI_URL_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class ConfigurationUtilTest {

  private String code = "data.import.storage.type";
  private String value = "LOCAL_STORAGE";
  private JsonObject config = new JsonObject().put("totalRecords", 1)
    .put("configs", new JsonArray().add(new JsonObject()
      .put("module", "DATA_IMPORT")
      .put("code", code)
      .put("value", "LOCAL_STORAGE")
    ));

  private static final String TENANT_ID = "diku";
  private static final String TOKEN = "token";
  private static final String CONFIGURATION_URI =
      "/configurations/entries?query="
          + PercentCodec.encodeAsString("module==DATA_IMPORT AND ( code==\"data.import.storage.type\")")
          + "&offset=0&limit=3&";

  private OkapiConnectionParams params;

  @Rule
  public WireMockRule mockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @Before
  public void setUp(TestContext context) {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL_HEADER, "http://localhost:" + mockServer.port());
    okapiHeaders.put(OKAPI_TENANT_HEADER, TENANT_ID);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TOKEN, TOKEN);
    params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());
  }

  @Test
  public void shouldReturnFailedFutureWhenInvalidUrl() {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL_HEADER, "localhost:");
    OkapiConnectionParams params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());

    ConfigurationUtil.getPropertyByCode("", params).onComplete(stringAsyncResult -> {
      assertTrue(stringAsyncResult.failed());
    });
  }

  @Test
  public void shouldReturnPropertyByCode(TestContext context) throws UnsupportedEncodingException {
    Async async = context.async();

    WireMock.stubFor(WireMock.get(CONFIGURATION_URI).willReturn(WireMock.okJson(config.toString())));

    Future<String> future = ConfigurationUtil.getPropertyByCode(code, params);
    future.onComplete(stringAsyncResult -> {
      assertTrue(stringAsyncResult.succeeded());
      assertEquals(value, stringAsyncResult.result());
      async.complete();
    });
  }

  @Test
  public void shouldFailFutureIfErrorResponse(TestContext context) throws UnsupportedEncodingException {
    Async async = context.async();

    WireMock.stubFor(WireMock.get(CONFIGURATION_URI).willReturn(WireMock.serverError()));

    Future<String> future = ConfigurationUtil.getPropertyByCode(code, params);
    future.onComplete(stringAsyncResult -> {
      assertTrue(stringAsyncResult.failed());
      assertTrue(stringAsyncResult.cause().getMessage().contains("Expected status code 200, got '500'"));
      async.complete();
    });
  }

  @Test
  public void shouldFailFutureIfNoConfigInResponse(TestContext context) throws UnsupportedEncodingException {
    Async async = context.async();

    WireMock.stubFor(WireMock.get(CONFIGURATION_URI)
        .willReturn(WireMock.okJson(new JsonObject().put("totalRecords", 0).toString())));

    Future<String> future = ConfigurationUtil.getPropertyByCode(code, params);
    future.onComplete(stringAsyncResult -> {
      assertTrue(stringAsyncResult.failed());
      assertTrue(stringAsyncResult.cause().getMessage().contains("No config values was found"));
      async.complete();
    });
  }
}
