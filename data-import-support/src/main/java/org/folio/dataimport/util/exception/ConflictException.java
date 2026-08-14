package org.folio.dataimport.util.exception;

/**
 * A runtime exception indicating a request conflict with current server state.
 */
public class ConflictException extends RuntimeException {

  /**
   * Construct a new "conflict" exception.
   *
   * @param message the detail message (which is saved for later retrieval
   *                by the {@link #getMessage()} method).
   */
  public ConflictException(String message) {
    super(message);
  }
}
