package org.folio.dataimport.testsupport.rest;

import static org.folio.dataimport.testsupport.vertx.VertxTestUtil.await;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.folio.dataimport.testsupport.tenant.TenantTestSupport;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Deploys one raml-module-builder {@link RestVerticle} per JVM, keyed by module id, instead of
 * once per test class. The first test class for a given module id deploys it and enables its
 * tenant; every later test class for the same module id reuses the already-running verticle and,
 * if it declares a tenant not yet enabled on it, enables just that tenant. The JUnit root store
 * closes each deployment automatically when the whole test run finishes, the same lifecycle
 * {@link org.folio.dataimport.testsupport.postgres.PostgresExtension} and
 * {@link org.folio.dataimport.testsupport.kafka.KafkaExtension} use for their containers.
 *
 * <p>{@link BaseRestTest} builds on this for RestAssured-based tests. Call it directly from a
 * module's own base test class when that module uses a different HTTP client.
 */
public final class SharedRestVerticleSupport {

  private static final Namespace NAMESPACE = Namespace.create(SharedRestVerticleSupport.class);

  private SharedRestVerticleSupport() {
  }

  /**
   * Returns the shared verticle deployment for {@code moduleId}, deploying it first if this is
   * the first test class asking for it. Equivalent to {@code getOrCreate(context, moduleId, true)}.
   *
   * @param context   the extension context of the calling test class (its root is used as the
   *                  JVM-wide store, so any test class's context works)
   * @param moduleId  the target module id, e.g. {@code mod-inventory-storage-1.0.0}; also the
   *                  cache key, so all test classes passing the same value share one deployment
   * @return the shared, already-deployed verticle
   */
  public static SharedRestVerticle getOrCreate(ExtensionContext context, String moduleId) {
    return getOrCreate(context, moduleId, true);
  }

  /**
   * Returns the verticle deployment for {@code moduleId}, deploying it first if none exists yet
   * at the requested scope.
   *
   * @param context   the extension context of the calling test class
   * @param moduleId  the target module id, e.g. {@code mod-inventory-storage-1.0.0}; also the
   *                  cache key, so test classes passing the same value at the same scope share
   *                  one deployment
   * @param shared    when {@code true}, the deployment lives in {@code context}'s root store, so
   *                  every test class in the JVM that asks for the same {@code moduleId} reuses
   *                  it, and it is closed only when the whole run finishes. When {@code false},
   *                  the deployment lives in {@code context}'s own store, so it is private to the
   *                  calling test class and closed as soon as that class's tests finish - the same
   *                  "one verticle per test class" behavior {@code BaseRestTest} had before shared
   *                  deployments were introduced.
   * @return the already-deployed verticle
   */
  public static SharedRestVerticle getOrCreate(ExtensionContext context, String moduleId, boolean shared) {
    var storeContext = shared ? context.getRoot() : context;
    var store = storeContext.getStore(NAMESPACE);
    return store.computeIfAbsent(moduleId, key -> new SharedRestVerticle(), SharedRestVerticle.class);
  }

  /**
   * A single shared verticle deployment: its {@link Vertx}, port, connection URL, and the set of
   * tenants already enabled on it.
   */
  public static final class SharedRestVerticle implements AutoCloseable {

    private final Vertx vertx = Vertx.vertx();
    private final int port = NetworkUtils.nextFreePort();
    private final String connectionUrl = "http://localhost:" + port;
    private final Set<String> enabledTenants = ConcurrentHashMap.newKeySet();

    private SharedRestVerticle() {
      var options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
      await(vertx.deployVerticle(RestVerticle.class.getName(), options));
    }

    public Vertx getVertx() {
      return vertx;
    }

    public int getPort() {
      return port;
    }

    public String getConnectionUrl() {
      return connectionUrl;
    }

    /**
     * Enables {@code tenantId} on this verticle if it has not already been enabled here by an
     * earlier test class.
     *
     * @param tenantId   the tenant identifier
     * @param token      the Okapi token, may be {@code null}
     * @param attributes the tenant attributes to post if the tenant is not yet enabled
     */
    public synchronized void enableTenantIfAbsent(String tenantId, String token, TenantAttributes attributes) {
      if (enabledTenants.add(tenantId)) {
        await(TenantTestSupport.enableTenant(vertx, connectionUrl, tenantId, token, attributes));
      }
    }

    @Override
    public void close() {
      await(vertx.close());
    }
  }
}
