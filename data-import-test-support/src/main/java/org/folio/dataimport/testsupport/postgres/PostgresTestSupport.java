package org.folio.dataimport.testsupport.postgres;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;

/**
 * Shared bootstrap for a PostgreSQL Testcontainer used by DataImport integration tests.
 *
 * <p>Registers a {@link PostgresTesterContainer} with raml-module-builder's {@link PostgresClient}
 * so that every client obtained during a test run is backed by the same container instance. Start
 * the container once (typically from a JUnit {@code @BeforeAll}) and stop it when the suite ends.
 */
public final class PostgresTestSupport {

  private static final Logger LOGGER = LogManager.getLogger();

  private static boolean started;

  private PostgresTestSupport() {
  }

  /**
   * Starts the shared PostgreSQL container using the default image and registers it with
   * {@link PostgresClient}. Repeated invocations are ignored.
   */
  public static synchronized void start() {
    if (started) {
      return;
    }
    LOGGER.info("start:: Starting shared PostgreSQL test container");
    PostgresClient.setPostgresTester(new PostgresTesterContainer());
    started = true;
  }

  /**
   * Starts the shared PostgreSQL container using a custom image. Repeated invocations are ignored.
   *
   * @param dockerImageName the Postgres Docker image, e.g. {@code postgres:16-alpine}
   */
  public static synchronized void start(String dockerImageName) {
    if (started) {
      return;
    }
    LOGGER.info("start:: Starting shared PostgreSQL test container, image: {}", dockerImageName);
    PostgresClient.setPostgresTester(new PostgresTesterContainer(dockerImageName));
    started = true;
  }

  /**
   * Returns a client bound to the shared (system) schema of the running container.
   *
   * @param vertx the Vert.x instance
   * @return a system-scoped {@link PostgresClient}
   */
  public static PostgresClient getClient(Vertx vertx) {
    return PostgresClient.getInstance(vertx);
  }

  /**
   * Returns a client bound to the schema of the given tenant.
   *
   * @param vertx    the Vert.x instance
   * @param tenantId the tenant identifier
   * @return a tenant-scoped {@link PostgresClient}
   */
  public static PostgresClient getClient(Vertx vertx, String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }

  /**
   * Deletes all rows from the given table in the named tenant's schema.
   *
   * @param tableName the unqualified table name
   * @param vertx     the Vert.x instance
   * @param tenantId  the tenant whose schema to clear
   * @return a {@link Future} that completes when the delete finishes
   */
  public static Future<Void> clearTable(String tableName, Vertx vertx, String tenantId) {
    return getClient(vertx, tenantId)
      .delete(tableName, new Criterion())
      .mapEmpty();
  }

  /**
   * Stops the shared PostgreSQL container and resets the bootstrap state so it can be started again.
   */
  public static synchronized void stop() {
    if (!started) {
      return;
    }
    LOGGER.info("stop:: Stopping shared PostgreSQL test container");
    PostgresClient.stopPostgresTester();
    started = false;
  }
}
