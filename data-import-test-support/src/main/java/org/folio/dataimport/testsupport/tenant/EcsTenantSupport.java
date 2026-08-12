package org.folio.dataimport.testsupport.tenant;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shared helpers for enabling Enhanced Consortia Support (ECS) tenants in integration tests.
 *
 * <p>In an ECS setup a single central tenant and one or more member tenants must each be enabled
 * independently. This helper enables the central tenant first and then all member tenants, reusing
 * {@link TenantTestSupport} for the individual Tenant API calls.
 */
public final class EcsTenantSupport {

  private static final Logger LOGGER = LogManager.getLogger();

  private EcsTenantSupport() {
  }

  /**
   * Enables the central tenant followed by every member tenant of a consortium.
   *
   * @param vertx    the Vert.x instance
   * @param okapiUrl the Okapi/module base URL
   * @param token    the Okapi token, may be {@code null}
   * @param moduleTo the target module id
   * @param central  the central tenant identifier
   * @param members  the member tenant identifiers
   * @return a future completed when every tenant has been enabled
   */
  public static Future<Void> enableConsortium(Vertx vertx, String okapiUrl, String token,
                                              String moduleTo, String central, List<String> members) {
    LOGGER.info("enableConsortium:: Enabling central tenant {} and {} member(s)",
      central, members.size());
    return TenantTestSupport.enableTenant(vertx, okapiUrl, central, token, moduleTo)
      .compose(job -> enableMembers(vertx, okapiUrl, token, moduleTo, members));
  }

  private static Future<Void> enableMembers(Vertx vertx, String okapiUrl, String token,
                                            String moduleTo, List<String> members) {
    var futures = members.stream()
      .map(member -> TenantTestSupport.enableTenant(vertx, okapiUrl, member, token, moduleTo))
      .toList();
    return Future.all(futures).mapEmpty();
  }
}
