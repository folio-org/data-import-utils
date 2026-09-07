package org.folio.dataimport.util.marc;

/**
 * Signals that a MARC record's content could not be read, parsed, mutated, or written back.
 * Internal "this failed" signal used by the throwing cores in {@link MarcRecordEditor}, which callers
 * that want a boolean/no-throw contract catch as a plain {@link Exception}.
 */
public class MarcContentException extends RuntimeException {

  public MarcContentException(String message) {
    super(message);
  }

  public MarcContentException(String message, Throwable cause) {
    super(message, cause);
  }
}
