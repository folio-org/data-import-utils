package org.folio.dataimport.testsupport.postgres;

import io.vertx.core.Vertx;
import org.folio.rest.persist.PostgresClient;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

/**
 * JUnit 5 extension that boots a single shared PostgreSQL Testcontainer for the whole test run.
 *
 * <p>Register it as a {@code static} field annotated with {@code @RegisterExtension}; the container
 * is started once before the first test and stopped automatically when the run finishes. Because
 * raml-module-builder keeps the Postgres tester in JVM-wide static state, a single container is
 * started per JVM no matter how many test classes register the extension.
 */
public class PostgresExtension implements BeforeAllCallback {

  private static final Namespace NAMESPACE = Namespace.create(PostgresExtension.class);
  private static final String RESOURCE_KEY = "postgres-container";

  private final String dockerImageName;

  /**
   * Creates an extension backed by the default PostgreSQL image.
   */
  public PostgresExtension() {
    this(null);
  }

  /**
   * Creates an extension backed by a custom PostgreSQL image.
   *
   * @param dockerImageName the Postgres Docker image, e.g. {@code postgres:16-alpine}
   */
  public PostgresExtension(String dockerImageName) {
    this.dockerImageName = dockerImageName;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    var store = context.getRoot().getStore(NAMESPACE);
    store.computeIfAbsent(RESOURCE_KEY, key -> new PostgresResource(dockerImageName));
  }

  /**
   * Returns a client bound to the shared (system) schema of the running container.
   *
   * @param vertx the Vert.x instance
   * @return a system-scoped {@link PostgresClient}
   */
  public PostgresClient getClient(Vertx vertx) {
    return PostgresTestSupport.getClient(vertx);
  }

  /**
   * Returns a client bound to the schema of the given tenant.
   *
   * @param vertx    the Vert.x instance
   * @param tenantId the tenant identifier
   * @return a tenant-scoped {@link PostgresClient}
   */
  public PostgresClient getClient(Vertx vertx, String tenantId) {
    return PostgresTestSupport.getClient(vertx, tenantId);
  }

  private static final class PostgresResource implements CloseableResource {

    PostgresResource(String dockerImageName) {
      if (dockerImageName == null) {
        PostgresTestSupport.start();
      } else {
        PostgresTestSupport.start(dockerImageName);
      }
    }

    @Override
    public void close() {
      PostgresTestSupport.stop();
    }
  }
}
