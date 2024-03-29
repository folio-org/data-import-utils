package org.folio.dataimport.util;

import org.folio.dataimport.util.exception.ConflictException;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExceptionHelperTest {

  @Test
  public void shouldReturnBadRequestResponse() {
    Response response = ExceptionHelper.mapExceptionToResponse(new BadRequestException("Bad request message"));
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Bad request message", response.getEntity().toString());
  }

  @Test
  public void shouldReturnNotFoundResponse() {
    Response response = ExceptionHelper.mapExceptionToResponse(new NotFoundException("Not found message"));
    assertNotNull(response);
    assertEquals(404, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Not found message", response.getEntity().toString());
  }

  @Test
  public void shouldReturnConflictResponse() {
    Response response = ExceptionHelper.mapExceptionToResponse(new ConflictException("Conflict message"));
    assertNotNull(response);
    assertEquals(409, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Conflict message", response.getEntity().toString());
  }

  @Test
  public void shouldReturnInternalServerErrorResponse() {
    Response response = ExceptionHelper.mapExceptionToResponse(new InternalServerErrorException("Internal server error message"));
    assertNotNull(response);
    assertEquals(500, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertTrue(response.getEntity().toString().contains("Internal Server Error"));
  }

}
