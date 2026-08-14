package org.folio.dataimport.util;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import io.vertx.core.Promise;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dataimport.util.exception.ConflictException;
import org.folio.rest.tools.utils.ValidationHelper;

public final class ExceptionHelper {

  private static final Logger LOGGER = LogManager.getLogger();

  private ExceptionHelper() {
  }

  public static Response mapExceptionToResponse(Throwable throwable) {
    Response knownExceptionResponse = mapKnownException(throwable);
    if (knownExceptionResponse != null) {
      return knownExceptionResponse;
    }
    return mapValidationOrInternalError(throwable);
  }

  private static Response mapKnownException(Throwable throwable) {
    if (throwable instanceof BadRequestException) {
      return buildPlainTextResponse(BAD_REQUEST, throwable.getMessage());
    }
    if (throwable instanceof NotFoundException) {
      return buildPlainTextResponse(NOT_FOUND, throwable.getMessage());
    }
    if (throwable instanceof ConflictException) {
      return buildPlainTextResponse(CONFLICT, throwable.getMessage());
    }
    return null;
  }

  private static Response mapValidationOrInternalError(Throwable throwable) {
    Promise<Response> validationFuture = Promise.promise();
    ValidationHelper.handleError(throwable, validationFuture::handle);
    if (validationFuture.future().isComplete()) {
      Response response = validationFuture.future().result();
      if (response.getStatus() == INTERNAL_SERVER_ERROR.getStatusCode()) {
        LOGGER.error(throwable.getMessage(), throwable);
      }
      return response;
    }
    LOGGER.error(throwable.getMessage(), throwable);
    return buildPlainTextResponse(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getReasonPhrase());
  }

  private static Response buildPlainTextResponse(Status status, String message) {
    return Response.status(status.getStatusCode())
      .type(MediaType.TEXT_PLAIN)
      .entity(message)
      .build();
  }
}
