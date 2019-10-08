package org.folio.dataimport.util;

import org.folio.rest.persist.Criteria.Criteria;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DaoUtilTest {

  @Test
  public void shouldConstructCriteria() {
    String idField = "id";
    String id = "000000000000000";
    Criteria criteria = DaoUtil.constructCriteria(idField, id);
    String expectedString = String.format("(jsonb->>%s) = '%s'", idField, id);
    assertNotNull(criteria);
    assertEquals(expectedString, criteria.toString());
  }
}
