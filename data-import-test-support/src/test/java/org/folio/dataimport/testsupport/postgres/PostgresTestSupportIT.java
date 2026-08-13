package org.folio.dataimport.testsupport.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.vertx.core.Vertx;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.persist.PostgresClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostgresTestSupportIT {

  @BeforeEach
  void ensureStopped() {
    PostgresTestSupport.stop();
  }

  @AfterEach
  void cleanup() {
    PostgresTestSupport.stop();
  }

  @DisplayName("should provide a non-null system PostgresClient after start")
  @Test
  void shouldProvideSystemPostgresClient_afterStart() {
    // arrange
    PostgresTestSupport.start();
    var vertx = Vertx.vertx();

    // act
    PostgresClient client = PostgresTestSupport.getClient(vertx);

    // assert
    assertThat(client).isNotNull();
    vertx.close();
  }

  @DisplayName("should provide a tenant-scoped PostgresClient after start")
  @Test
  void shouldProvideTenantScopedPostgresClient_afterStart() {
    // arrange
    PostgresTestSupport.start();
    var vertx = Vertx.vertx();

    // act
    PostgresClient client = PostgresTestSupport.getClient(vertx, "test-tenant");

    // assert
    assertThat(client).isNotNull();
    vertx.close();
  }

  @DisplayName("should not throw when start is called a second time")
  @Test
  void shouldNotThrow_whenStartCalledTwice() {
    assertThatNoException().isThrownBy(() -> {
      PostgresTestSupport.start();
      PostgresTestSupport.start();
    });
  }

  @DisplayName("should not throw when stop is called while already stopped")
  @Test
  void shouldNotThrow_whenStopCalledWhileAlreadyStopped() {
    assertThatNoException().isThrownBy(PostgresTestSupport::stop);
  }

  @DisplayName("should start with a custom docker image without throwing")
  @Test
  void shouldStartWithCustomDockerImage_withoutThrowing() {
    assertThatNoException().isThrownBy(
      () -> PostgresTestSupport.start(PostgresTesterContainer.DEFAULT_IMAGE_NAME));
  }
}
