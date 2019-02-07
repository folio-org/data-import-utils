package org.folio.dataimport.util;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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

  static final String TENANT_ID = "diku";
  static Vertx vertx;
  private static int PORT = NetworkUtils.nextFreePort();
  private static String BASE_URL = "http://localhost:";
  private static String OKAPI_URL = BASE_URL + PORT;
  private static final String TOKEN = "token";

  @Rule
  public WireMockRule mockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true)));

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    Async async = context.async();
    vertx = Vertx.vertx();

    TenantClient tenantClient = new TenantClient(OKAPI_URL, "diku", "dummy-token");
    DeploymentOptions restVerticleDeploymentOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", PORT));
    vertx.deployVerticle(RestVerticle.class.getName(), restVerticleDeploymentOptions, res -> {
      try {
        tenantClient.postTenant(null, res2 -> async.complete());
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      async.complete();
    }));
  }

  @Before
  public void setUp(TestContext testContext) {
//    WireMock.get("/configurations/entries?query="
//      + URLEncoder.encode("module==DATA_IMPORT AND ( code==\"data.import.storage.type\")", "UTF-8")
//      + "&offset=0&limit=3&").willReturn(WireMock.okJson(config.toString()));
    try {
    WireMock.stubFor(WireMock.get(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
      .willReturn(WireMock.okJson(config.toString())));
    } catch (Exception ignored) {
    }
  }

  @Test
  public void getPropertyByCode() {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL_HEADER, "http://localhost:" + mockServer.port());
    okapiHeaders.put(OKAPI_TENANT_HEADER, TENANT_ID);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TOKEN, TOKEN);
    OkapiConnectionParams params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());

    ConfigurationUtil.getPropertyByCode(code, params).setHandler(stringAsyncResult -> {
      assertTrue(stringAsyncResult.succeeded());
      assertEquals(value, stringAsyncResult.result());
    });
  }
}
