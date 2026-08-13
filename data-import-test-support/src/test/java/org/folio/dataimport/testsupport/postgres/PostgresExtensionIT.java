package org.folio.dataimport.testsupport.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import org.folio.rest.persist.PostgresClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PostgresExtensionIT {

  @RegisterExtension
  static final PostgresExtension POSTGRES = new PostgresExtension();

  private Vertx vertx;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
  }

  @AfterEach
  void tearDown() {
    vertx.close();
  }

  @DisplayName("should provide a non-null system PostgresClient when extension is active")
  @Test
  void shouldProvideSystemPostgresClient_whenExtensionIsActive() {
    // act
    PostgresClient client = POSTGRES.getClient(vertx);

    // assert
    assertThat(client).isNotNull();
  }

  @DisplayName("should provide a non-null tenant-scoped PostgresClient when extension is active")
  @Test
  void shouldProvideTenantScopedPostgresClient_whenExtensionIsActive() {
    // act
    PostgresClient client = POSTGRES.getClient(vertx, "test-tenant");

    // assert
    assertThat(client).isNotNull();
  }
}
