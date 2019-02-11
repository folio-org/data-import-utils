package org.folio.dataimport.util;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.client.ConfigurationsClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.folio.dataimport.util.RestUtil.OKAPI_URL_HEADER;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationUtil.class })
public class ConfigurationUtilTest {

  @Test
  public void shouldReturnFailedFutureWhenInvalidUrl() {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL_HEADER, "localhost:");
    OkapiConnectionParams params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());

    ConfigurationUtil.getPropertyByCode("", params).setHandler(stringAsyncResult -> {
      assertTrue(stringAsyncResult.failed());
      assertTrue(stringAsyncResult.cause().getMessage().contains("Could not parse okapiURL"));
    });
  }

  @Test
  public void shouldReturnFailedFutureWhenExceptionIsThrown() throws Exception {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL_HEADER, "http://localhost:");
    OkapiConnectionParams params = new OkapiConnectionParams(okapiHeaders, Vertx.vertx());

    ConfigurationsClient client = spy(ConfigurationsClient.class);
    whenNew(ConfigurationsClient.class)
      .withAnyArguments()
      .thenReturn(client);

    doThrow(new UnsupportedEncodingException())
      .when(client)
      .getEntries(anyString(), anyInt(), anyInt(), isNull(), isNull(), any(Handler.class));

    ConfigurationUtil.getPropertyByCode("", params).setHandler(stringAsyncResult -> {
      assertTrue(stringAsyncResult.failed());
      assertTrue(stringAsyncResult.cause() instanceof UnsupportedEncodingException);
    });
  }
}
