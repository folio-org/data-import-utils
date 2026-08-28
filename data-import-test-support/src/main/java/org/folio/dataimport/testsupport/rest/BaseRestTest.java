package org.folio.dataimport.testsupport.rest;

import static org.folio.dataimport.testsupport.vertx.VertxTestUtil.await;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import org.folio.dataimport.testsupport.kafka.KafkaExtension;
import org.folio.dataimport.testsupport.postgres.PostgresExtension;
import org.folio.dataimport.testsupport.tenant.TenantTestSupport;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Base class for raml-module-builder integration tests that need a running module.
 *
 * <p>Boots shared PostgreSQL and Kafka containers, deploys the standard raml-module-builder
 * {@link RestVerticle} on a random free port and runs the Tenant API so the module schema is
 * created before the first test. The deployment logic is identical for every raml-module-builder
 * module, so subclasses only provide the module id through {@link #getModuleName()}; the deployed
 * module is then reachable through {@link #connectionUrl} on {@link #port}.
 *
 * <p>WireMock support (stubbing other modules) and RestAssured HTTP helpers are inherited from
 * {@link BaseRestAssuredTest} and {@link BaseWireMockTest} respectively.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseRestTest extends BaseRestAssuredTest {

  @RegisterExtension
  protected static final PostgresExtension POSTGRES = new PostgresExtension();

  @RegisterExtension
  protected static final KafkaExtension KAFKA = new KafkaExtension();

  private static final String DEFAULT_TENANT = "diku";

  protected Vertx vertx;
  protected int port;
  protected String connectionUrl;

  /**
   * Returns the module id used as the {@code moduleTo} value for the Tenant API,
   * e.g. {@code mod-data-import-1.0.0}.
   *
   * @return the target module id
   */
  protected abstract String getModuleName();

  /**
   * Returns the tenant enabled before the tests run. Defaults to {@code diku}.
   *
   * @return the tenant identifier
   */
  protected String getTenantId() {
    return DEFAULT_TENANT;
  }

  /**
   * Returns the Okapi token used for the Tenant API call. Defaults to {@code null}.
   *
   * @return the Okapi token, or {@code null}
   */
  protected String getToken() {
    return null;
  }

  /**
   * Returns the attributes posted to the Tenant API. Defaults to enabling {@link #getModuleName()}.
   *
   * @return the tenant attributes
   */
  protected TenantAttributes getTenantAttributes() {
    return new TenantAttributes().withModuleTo(getModuleName());
  }

  /**
   * Returns extra headers merged into the pre-built RestAssured {@link #spec}, in addition to the
   * tenant, token and {@code X-Okapi-Url} headers set by default. Defaults to none. Override to add
   * e.g. an {@code X-Okapi-UserId} header expected by the module under test.
   *
   * @return the extra headers, keyed by header name
   */
  protected Map<String, String> getExtraSpecHeaders() {
    return Map.of();
  }

  /**
   * Builds a spec with the module base URI and the standard Okapi headers (tenant, URL, and
   * optionally token). Override {@link #getExtraSpecHeaders()} to inject additional headers rather
   * than overriding this method.
   */
  @Override
  protected RequestSpecification buildSpec() {
    var specBuilder = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .setBaseUri(connectionUrl)
      .addHeader(XOkapiHeaders.TENANT, getTenantId())
      .addHeader(XOkapiHeaders.URL, mockServerUrl());
    if (getToken() != null) {
      specBuilder.addHeader(XOkapiHeaders.TOKEN, getToken());
    }
    getExtraSpecHeaders().forEach(specBuilder::addHeader);
    return specBuilder.build();
  }

  @BeforeAll
  void deployRestVerticle() {
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();
    connectionUrl = "http://localhost:" + port;

    var options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
    await(vertx.deployVerticle(RestVerticle.class.getName(), options));
    await(TenantTestSupport.enableTenant(vertx, connectionUrl, getTenantId(), getToken(), getTenantAttributes()));
    spec = buildSpec();
  }

  @AfterAll
  void undeployRestVerticle() {
    if (vertx != null) {
      await(vertx.close());
    }
  }
}
