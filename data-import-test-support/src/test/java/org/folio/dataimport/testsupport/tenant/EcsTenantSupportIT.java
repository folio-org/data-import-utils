package org.folio.dataimport.testsupport.tenant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.vertx.core.Vertx;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class EcsTenantSupportIT {

  @RegisterExtension
  static final WireMockExtension WIRE_MOCK = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  private Vertx vertx;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
    WIRE_MOCK.stubFor(post(urlPathEqualTo("/_/tenant"))
      .willReturn(aResponse()
        .withStatus(201)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":\"ecs-job-id\"}")));
    WIRE_MOCK.stubFor(get(urlPathMatching("/_/tenant/.*"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":\"ecs-job-id\"}")));
  }

  @AfterEach
  void tearDown() throws Exception {
    vertx.close().toCompletionStage().toCompletableFuture().get(10, SECONDS);
  }

  @DisplayName("should enable central and member tenants without error")
  @Test
  void shouldEnableCentralAndMemberTenants_withoutError() {
    // act / assert
    assertThatNoException().isThrownBy(() ->
      EcsTenantSupport.enableConsortium(
          vertx, WIRE_MOCK.baseUrl(), null, "mod-test-1.0.0",
          "central", List.of("member1", "member2"))
        .toCompletionStage().toCompletableFuture().get(30, SECONDS));
  }

  @DisplayName("should post to tenant endpoint once per tenant when enabling consortium")
  @Test
  void shouldPostToTenantEndpointOncePerTenant_whenEnablingConsortium() throws Exception {
    // arrange
    List<String> members = List.of("member1", "member2");

    // act
    EcsTenantSupport.enableConsortium(
        vertx, WIRE_MOCK.baseUrl(), null, "mod-test-1.0.0", "central", members)
      .toCompletionStage().toCompletableFuture().get(30, SECONDS);

    // assert — 1 central + 2 members = 3 POST calls total
    WIRE_MOCK.verify(3, postRequestedFor(urlPathEqualTo("/_/tenant")));
  }
}
