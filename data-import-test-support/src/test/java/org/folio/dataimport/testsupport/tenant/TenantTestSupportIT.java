package org.folio.dataimport.testsupport.tenant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.vertx.core.Vertx;
import java.util.concurrent.ExecutionException;
import org.folio.rest.jaxrs.model.TenantJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TenantTestSupportIT {

  @RegisterExtension
  static final WireMockExtension WIRE_MOCK = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  private Vertx vertx;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
  }

  @AfterEach
  void tearDown() throws Exception {
    vertx.close().toCompletionStage().toCompletableFuture().get(10, SECONDS);
  }

  @DisplayName("should return completed TenantJob when Tenant API responds with 201 and a job id")
  @Test
  void shouldReturnCompletedTenantJob_whenTenantApiResponds201WithJobId() throws Exception {
    // arrange
    WIRE_MOCK.stubFor(post(urlPathEqualTo("/_/tenant"))
      .willReturn(aResponse()
        .withStatus(201)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":\"test-job-id\"}")));
    WIRE_MOCK.stubFor(get(urlPathMatching("/_/tenant/.*"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":\"test-job-id\"}")));

    // act
    TenantJob job = TenantTestSupport
      .enableTenant(vertx, WIRE_MOCK.baseUrl(), "diku", null, "mod-test-1.0.0")
      .toCompletionStage().toCompletableFuture().get(30, SECONDS);

    // assert
    assertThat(job.getId()).isEqualTo("test-job-id");
  }

  @DisplayName("should return completed TenantJob when enableTenant is called with TenantAttributes")
  @Test
  void shouldReturnCompletedTenantJob_whenCalledWithTenantAttributes() throws Exception {
    // arrange
    WIRE_MOCK.stubFor(post(urlPathEqualTo("/_/tenant"))
      .willReturn(aResponse()
        .withStatus(201)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":\"attr-job-id\"}")));
    WIRE_MOCK.stubFor(get(urlPathMatching("/_/tenant/.*"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":\"attr-job-id\"}")));

    // act
    TenantJob job = TenantTestSupport
      .enableTenant(vertx, WIRE_MOCK.baseUrl(), "diku", null,
        new org.folio.rest.jaxrs.model.TenantAttributes().withModuleTo("mod-test-1.0.0"))
      .toCompletionStage().toCompletableFuture().get(30, SECONDS);

    // assert
    assertThat(job.getId()).isEqualTo("attr-job-id");
  }

  @DisplayName("should fail the future when Tenant API responds with non-201 status")
  @Test
  void shouldFailFuture_whenTenantApiRespondsWithNon201Status() {
    // arrange
    WIRE_MOCK.stubFor(post(urlPathEqualTo("/_/tenant"))
      .willReturn(aResponse().withStatus(500)));

    // act
    var completable = TenantTestSupport
      .enableTenant(vertx, WIRE_MOCK.baseUrl(), "diku", null, "mod-test-1.0.0")
      .toCompletionStage().toCompletableFuture();

    // assert
    assertThatThrownBy(() -> completable.get(10, SECONDS))
      .isInstanceOf(ExecutionException.class)
      .hasMessageContaining("500");
  }
}
