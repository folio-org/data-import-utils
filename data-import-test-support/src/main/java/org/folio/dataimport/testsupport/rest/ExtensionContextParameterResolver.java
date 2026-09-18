package org.folio.dataimport.testsupport.rest;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Resolves {@link ExtensionContext}-typed parameters on {@code @BeforeAll}/{@code @BeforeEach}/
 * {@code @Test} methods.
 *
 * <p>JUnit Jupiter injects {@code ExtensionContext} into extension SPI callbacks (e.g.
 * {@code BeforeAllCallback}) automatically, but has no built-in resolver for it as a plain method
 * parameter. {@link BaseRestTest#deployRestVerticle(ExtensionContext)} needs the context to share
 * one {@link SharedRestVerticleSupport.SharedRestVerticle} deployment across test classes via the
 * JUnit root store, so this extension fills that gap.
 */
final class ExtensionContextParameterResolver implements ParameterResolver {

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == ExtensionContext.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext;
  }
}
