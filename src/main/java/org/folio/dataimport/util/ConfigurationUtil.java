package org.folio.dataimport.util;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import org.folio.rest.client.ConfigurationsClient;

import static java.lang.String.format;

/**
 * Util for loading configuration values from mod-configuration
 */
public final class ConfigurationUtil {

  private static final String MODULE_CODE = "DATA_IMPORT";

  private ConfigurationUtil() {
  }

  /**
   * Load property value from mod-config by code
   *
   * @param code - property code
   * @return a list of user fields to use for search
   */
  public static Future<String> getPropertyByCode(String code, OkapiConnectionParams params) {
    Promise<String> promise = Promise.promise();
    String okapiURL = params.getOkapiUrl();
    String tenant = params.getTenantId();
    String token = params.getToken();
    try {
      ConfigurationsClient configurationsClient = new ConfigurationsClient(okapiURL, tenant, token);
      StringBuilder query = new StringBuilder("module==")
        .append(MODULE_CODE)
        .append(" AND ( code==\"")
        .append(code)
        .append("\")");
      configurationsClient.getConfigurationsEntries(query.toString(), 0, 3, null, null, response -> {
        if (response.result().statusCode() != 200) {
          promise.fail(format("Expected status code 200, got '%s' : %s", response.result().statusCode(), response.result().bodyAsString()));
          return;
        }
        JsonObject entries = response.result().bodyAsJsonObject();
        Integer total = entries.getInteger("totalRecords");
        if (total != null && total > 0) {
          promise.complete(
            entries.getJsonArray("configs")
              .getJsonObject(0)
              .getString("value"));
        } else {
          promise.fail("No config values was found");
        }
      });
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

}
