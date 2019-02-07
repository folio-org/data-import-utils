package org.folio.dataimport.util;

import org.apache.http.HttpStatus;
import org.folio.rest.tools.utils.ValidationHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
public class ExceptionHelperTest {

  @Test
  @PrepareForTest({ ValidationHelper.class })
  public void shouldReturnBadRequestResponse() {
    Response response = ExceptionHelper.mapExceptionToResponse(new BadRequestException("Bad request message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Bad request message", response.getEntity().toString());
  }

  @Test
  @PrepareForTest({ ValidationHelper.class })
  public void shouldReturnNotFoundResponse() {
    Response response = ExceptionHelper.mapExceptionToResponse(new NotFoundException("Not found message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Not found message", response.getEntity().toString());
  }

  @Test
  public void shouldReturnInternalServerErrorResponse() {
    Response response = ExceptionHelper.mapExceptionToResponse(new InternalServerErrorException("Internal server error message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertTrue(response.getEntity().toString().contains("Internal Server Error"));
  }

  @Test
  @PrepareForTest({ ValidationHelper.class })
  public void shouldReturnErrorResponse() {
    spy(ValidationHelper.class);
    doNothing().when(ValidationHelper.class);
    Response response = ExceptionHelper.mapExceptionToResponse(new IllegalArgumentException("Error message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), response.getEntity().toString());
  }
}
