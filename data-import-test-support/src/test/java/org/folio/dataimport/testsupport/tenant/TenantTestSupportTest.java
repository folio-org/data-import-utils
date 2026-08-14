package org.folio.dataimport.testsupport.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.rest.jaxrs.model.Parameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TenantTestSupportTest {

  @DisplayName("should return two parameters keyed by loadReference and loadSample")
  @Test
  void shouldReturnTwoParametersKeyedByLoadReferenceAndLoadSample() {
    // act
    List<Parameter> params = TenantTestSupport.dataLoadingParameters(true, false);

    // assert
    assertThat(params).hasSize(2);
    assertThat(params.get(0).getKey()).isEqualTo("loadReference");
    assertThat(params.get(1).getKey()).isEqualTo("loadSample");
  }

  @DisplayName("should encode loadReference and loadSample flag values as strings")
  @ParameterizedTest(name = "loadReference={0}, loadSample={1}")
  @CsvSource({"true,true", "false,false", "true,false", "false,true"})
  void shouldEncodeDataLoadingFlagsAsStrings(boolean loadReference, boolean loadSample) {
    // act
    List<Parameter> params = TenantTestSupport.dataLoadingParameters(loadReference, loadSample);

    // assert
    assertThat(params.get(0).getValue()).isEqualTo(Boolean.toString(loadReference));
    assertThat(params.get(1).getValue()).isEqualTo(Boolean.toString(loadSample));
  }
}
