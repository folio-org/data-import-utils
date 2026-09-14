package org.folio.dataimport.testsupport.tenant;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;

/**
 * Shared helpers to enable and configure FOLIO tenants for DataImport integration tests.
 *
 * <p>Wraps raml-module-builder's {@link TenantClient} to post {@link TenantAttributes} to the
 * {@code /_/tenant} endpoint and wait for the asynchronous tenant job to finish, returning the
 * completed {@link TenantJob} so tests can assert on its outcome.
 */
public final class TenantTestSupport {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final int DEFAULT_JOB_TIMEOUT_MS = 60_000;

  private TenantTestSupport() {
  }

  /**
   * Enables a tenant for the given target module and waits for the job to complete.
   *
   * @param vertx    the Vert.x instance
   * @param okapiUrl the Okapi/module base URL
   * @param tenantId the tenant identifier
   * @param token    the Okapi token, may be {@code null}
   * @param moduleTo the target module id, e.g. {@code mod-data-import-1.0.0}
   * @return a future completed with the finished tenant job
   */
  public static Future<TenantJob> enableTenant(Vertx vertx, String okapiUrl, String tenantId,
                                               String token, String moduleTo) {
    return enableTenant(vertx, okapiUrl, tenantId, token,
      new TenantAttributes().withModuleTo(moduleTo));
  }

  /**
   * Enables a tenant using fully specified attributes and waits for the job to complete.
   *
   * @param vertx      the Vert.x instance
   * @param okapiUrl   the Okapi/module base URL
   * @param tenantId   the tenant identifier
   * @param token      the Okapi token, may be {@code null}
   * @param attributes the tenant attributes to post
   * @return a future completed with the finished tenant job
   */
  public static Future<TenantJob> enableTenant(Vertx vertx, String okapiUrl, String tenantId,
                                               String token, TenantAttributes attributes) {
    return enableTenant(WebClient.create(vertx), okapiUrl, tenantId, token, attributes);
  }

  /**
   * Enables a tenant using fully specified attributes and waits for the job to complete, posting
   * through the given {@code webClient} rather than a bare, freshly-created one.
   *
   * <p>Use this overload when the tenant-install request needs headers or other behavior a caller
   * configures on its own {@link WebClient} (e.g. an interceptor adding {@code X-Okapi-Url}/
   * {@code X-Okapi-Url-to} for a module that consults both) instead of duplicating this method's
   * request/job-polling logic just to get such a client wired in.
   *
   * @param webClient  the (already configured) client to post the tenant request through
   * @param okapiUrl   the Okapi/module base URL
   * @param tenantId   the tenant identifier
   * @param token      the Okapi token, may be {@code null}
   * @param attributes the tenant attributes to post
   * @return a future completed with the finished tenant job
   */
  public static Future<TenantJob> enableTenant(WebClient webClient, String okapiUrl, String tenantId,
                                               String token, TenantAttributes attributes) {
    LOGGER.info("enableTenant:: Enabling tenant {} against {}", tenantId, okapiUrl);
    var client = new TenantClient(okapiUrl, tenantId, token, webClient);
    return client.postTenant(attributes).compose(response -> awaitJob(client, response));
  }

  /**
   * Builds the reference/sample data loading parameters accepted by the Tenant API.
   *
   * @param loadReference whether reference data should be loaded
   * @param loadSample    whether sample data should be loaded
   * @return the parameter list to attach to {@link TenantAttributes}
   */
  public static List<Parameter> dataLoadingParameters(boolean loadReference, boolean loadSample) {
    return List.of(
      new Parameter().withKey("loadReference").withValue(Boolean.toString(loadReference)),
      new Parameter().withKey("loadSample").withValue(Boolean.toString(loadSample)));
  }

  private static Future<TenantJob> awaitJob(TenantClient client, HttpResponse<Buffer> response) {
    if (response.statusCode() != 201) {
      return Future.failedFuture("Unexpected tenant registration status: " + response.statusCode());
    }
    var job = response.bodyAsJson(TenantJob.class);
    return client.getTenantByOperationId(job.getId(), DEFAULT_JOB_TIMEOUT_MS)
      .map(jobResponse -> jobResponse.bodyAsJson(TenantJob.class));
  }
}
