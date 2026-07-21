package org.folio.dataimport.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DaoUtilTest {

  @Test
  void shouldConstructCriteria() {
    var idField = "id";
    var id = "000000000000000";
    var criteria = DaoUtil.constructCriteria(idField, id);
    var expectedString = String.format("(jsonb->>%s) = '%s'", idField, id);
    assertNotNull(criteria);
    assertEquals(expectedString, criteria.toString());
  }
}
