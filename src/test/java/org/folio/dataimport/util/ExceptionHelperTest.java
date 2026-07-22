package org.folio.dataimport.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import org.folio.dataimport.util.exception.ConflictException;
import org.junit.jupiter.api.Test;

class ExceptionHelperTest {

  @Test
  void shouldReturnBadRequestResponse() {
    var response = ExceptionHelper.mapExceptionToResponse(new BadRequestException("Bad request message"));
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Bad request message", response.getEntity().toString());
  }

  @Test
  void shouldReturnNotFoundResponse() {
    var response = ExceptionHelper.mapExceptionToResponse(new NotFoundException("Not found message"));
    assertNotNull(response);
    assertEquals(404, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Not found message", response.getEntity().toString());
  }

  @Test
  void shouldReturnConflictResponse() {
    var response = ExceptionHelper.mapExceptionToResponse(new ConflictException("Conflict message"));
    assertNotNull(response);
    assertEquals(409, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Conflict message", response.getEntity().toString());
  }

  @Test
  void shouldReturnInternalServerErrorResponse() {
    var response = ExceptionHelper.mapExceptionToResponse(new InternalServerErrorException("Internal server error"));
    assertNotNull(response);
    assertEquals(500, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertTrue(response.getEntity().toString().contains("Internal Server Error"));
  }
}
