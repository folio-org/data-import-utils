package org.folio.dataimport.util;

import org.folio.rest.persist.Criteria.Criteria;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DaoUtilTest {

  @Test
  public void shouldConstructCriteria() {
    String id = "000000000000000";
    String expectedQuery = "(jsonb->>id) = '" + id + "'";
    Criteria criteria = DaoUtil.constructCriteria("id", id);
    assertNotNull(criteria);
    assertEquals(expectedQuery, criteria.toString());
  }
}
