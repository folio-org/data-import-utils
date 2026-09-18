package org.folio.dataimport.testsupport.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

class ExtensionContextParameterResolverTest {

  private final ExtensionContextParameterResolver resolver = new ExtensionContextParameterResolver();

  @DisplayName("should support a parameter typed as ExtensionContext")
  @Test
  void shouldSupportParameter_whenParameterTypeIsExtensionContext() throws NoSuchMethodException {
    // arrange
    var parameterContext = parameterContextFor("withExtensionContext", ExtensionContext.class);

    // act
    boolean supported = resolver.supportsParameter(parameterContext, noopExtensionContext());

    // assert
    assertThat(supported).isTrue();
  }

  @DisplayName("should not support a parameter typed as something other than ExtensionContext")
  @Test
  void shouldNotSupportParameter_whenParameterTypeIsNotExtensionContext() throws NoSuchMethodException {
    // arrange
    var parameterContext = parameterContextFor("withString", String.class);

    // act
    boolean supported = resolver.supportsParameter(parameterContext, noopExtensionContext());

    // assert
    assertThat(supported).isFalse();
  }

  @DisplayName("should resolve the parameter to the exact extension context instance it was given")
  @Test
  void shouldResolveParameter_toGivenExtensionContextInstance() throws NoSuchMethodException {
    // arrange
    var parameterContext = parameterContextFor("withExtensionContext", ExtensionContext.class);
    var context = noopExtensionContext();

    // act
    Object resolved = resolver.resolveParameter(parameterContext, context);

    // assert
    assertThat(resolved).isSameAs(context);
  }

  private static ParameterContext parameterContextFor(String methodName, Class<?> paramType)
      throws NoSuchMethodException {
    Method method = MethodHolder.class.getDeclaredMethod(methodName, paramType);
    Parameter parameter = method.getParameters()[0];
    return new FakeParameterContext(parameter);
  }

  /**
   * A stand-in {@link ExtensionContext} for tests that only need a distinguishable instance to
   * pass through {@link ExtensionContextParameterResolver}, never one whose methods are actually
   * invoked.
   */
  private static ExtensionContext noopExtensionContext() {
    return (ExtensionContext) Proxy.newProxyInstance(
      ExtensionContextParameterResolverTest.class.getClassLoader(),
      new Class<?>[] {ExtensionContext.class},
      (proxy, method, args) -> {
        throw new UnsupportedOperationException("not needed by this test");
      });
  }

  @SuppressWarnings("unused")
  private static final class MethodHolder {

    @SuppressWarnings("java:S1172")
    void withExtensionContext(ExtensionContext context) {
      // for test support only
    }

    @SuppressWarnings("java:S1172")
    void withString(String value) {
      // for test support only
    }
  }

  private record FakeParameterContext(Parameter parameter) implements ParameterContext {
    @Override
    public @NonNull Parameter getParameter() {
      return parameter;
    }

    @Override
    public int getIndex() {
      return 0;
    }

    @Override
    public @NonNull Optional<Object> getTarget() {
      return Optional.empty();
    }
  }
}
