package org.folio.dataimport.testsupport.rest;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import java.util.Map;
import org.folio.dataimport.testsupport.kafka.KafkaExtension;
import org.folio.dataimport.testsupport.postgres.PostgresExtension;
import org.folio.dataimport.testsupport.rest.SharedRestVerticleSupport.SharedRestVerticle;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Base class for raml-module-builder integration tests that need a running module.
 *
 * <p>Boots shared PostgreSQL and Kafka containers and deploys one shared raml-module-builder
 * {@link RestVerticle} for the whole JVM, keyed by {@link #getModuleName()}. The first test class
 * for a given module deploys it and enables its tenant; every later test class for the same module
 * reuses the already-running verticle and, if it declares a tenant not yet enabled on it, enables
 * just that tenant. This avoids paying the deploy-and-provision cost once per test class. Subclasses
 * provide the module id through {@link #getModuleName()}; the deployed module is then reachable
 * through {@link #connectionUrl} on {@link #port}.
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
   * e.g. {@code mod-data-import-1.0.0}. Also the key under which the shared verticle for this
   * module is cached, so all test classes returning the same value share one deployment.
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
  void deployRestVerticle(ExtensionContext context) {
    SharedRestVerticle shared = SharedRestVerticleSupport.getOrCreate(context, getModuleName());
    vertx = shared.getVertx();
    port = shared.getPort();
    connectionUrl = shared.getConnectionUrl();
    shared.enableTenantIfAbsent(getTenantId(), getToken(), getTenantAttributes());
    spec = buildSpec();
  }
}
