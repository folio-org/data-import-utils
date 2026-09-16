package org.folio.dataimport.testsupport.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Runs real, throwaway test classes through the Jupiter engine (via {@link EngineTestKit}) to
 * verify {@link SharedRestVerticleSupport#getOrCreate(ExtensionContext, String, boolean)}'s scoping
 * contract end to end, the same way {@code deployRestVerticle} exercises it in {@link BaseRestTest}
 * (excluded from coverage since it needs a live Postgres/Kafka stack that this module doesn't have).
 */
class SharedRestVerticleSupportIT {

  @DisplayName("should reuse the same verticle across test classes when shared")
  @Test
  void shouldReuseSameVerticle_whenSharedAcrossTestClasses() {
    // act
    EngineTestKit.engine("junit-jupiter")
      .selectors(selectClass(SharedFirst.class), selectClass(SharedSecond.class))
      .execute()
      .testEvents()
      .assertStatistics(stats -> stats.succeeded(2).failed(0));

    // assert
    assertThat(SharedSecond.PORT).isEqualTo(SharedFirst.PORT);
  }

  @DisplayName("should deploy a separate verticle per test class when not shared")
  @Test
  void shouldDeploySeparateVerticles_whenNotShared() {
    // act
    EngineTestKit.engine("junit-jupiter")
      .selectors(selectClass(PrivateFirst.class), selectClass(PrivateSecond.class))
      .execute()
      .testEvents()
      .assertStatistics(stats -> stats.succeeded(2).failed(0));

    // assert
    assertThat(PrivateSecond.PORT).isNotEqualTo(PrivateFirst.PORT);
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @ExtendWith(ExtensionContextParameterResolver.class)
  static class SharedFirst {
    static volatile int PORT;

    @BeforeAll
    void deploy(ExtensionContext context) {
      PORT = SharedRestVerticleSupport.getOrCreate(context, "mod-shared-it-test", true).getPort();
    }

    @Test
    void noop() {
      assertTrue(true);
    }
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @ExtendWith(ExtensionContextParameterResolver.class)
  static class SharedSecond {
    static volatile int PORT;

    @BeforeAll
    void deploy(ExtensionContext context) {
      PORT = SharedRestVerticleSupport.getOrCreate(context, "mod-shared-it-test", true).getPort();
    }

    @Test
    void noop() {
      assertTrue(true);
    }
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @ExtendWith(ExtensionContextParameterResolver.class)
  static class PrivateFirst {
    static volatile int PORT;

    @BeforeAll
    void deploy(ExtensionContext context) {
      PORT = SharedRestVerticleSupport.getOrCreate(context, "mod-private-it-test", false).getPort();
    }

    @Test
    void noop() {
      assertTrue(true);
    }
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @ExtendWith(ExtensionContextParameterResolver.class)
  static class PrivateSecond {
    static volatile int PORT;

    @BeforeAll
    void deploy(ExtensionContext context) {
      PORT = SharedRestVerticleSupport.getOrCreate(context, "mod-private-it-test", false).getPort();
    }

    @Test
    void noop() {
      assertTrue(true);
    }
  }
}
