package org.folio.dataimport.util;

import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.cql.CQLWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DaoUtil.class})
public class DaoUtilTest {

  private final String tableName = "table_name";
  private final String query = "status any \\\"\" + COMMITTED + \" \" + ERROR + \" \\\"";

  @Before
  public void setUp() throws Exception {
    CQL2PgJSON cql2PgJSON = mock(CQL2PgJSON.class);
    whenNew(CQL2PgJSON.class)
      .withArguments(tableName + ".jsonb")
      .thenReturn(cql2PgJSON);
  }

  @Test
  public void shouldCreateCqlWrapperWithNoLimitOrOffset() throws Exception {
    CQLWrapper cqlWrapperNoLimitNoOffset = DaoUtil.getCQLWrapper(tableName, query);
    assertNotNull(cqlWrapperNoLimitNoOffset);
    assertEquals(query, cqlWrapperNoLimitNoOffset.getQuery());
    assertFalse(cqlWrapperNoLimitNoOffset.toString().contains("LIMIT"));
    assertFalse(cqlWrapperNoLimitNoOffset.toString().contains("OFFSET"));
  }

  @Test
  public void shouldCreateCqlWrapperWithLimitAndOffset() throws Exception {
    CQLWrapper cqlWrapper = DaoUtil.getCQLWrapper(tableName, query, 20, 0);
    assertNotNull(cqlWrapper);
    assertEquals(query, cqlWrapper.getQuery());
    assertTrue(cqlWrapper.toString().contains("LIMIT 20 OFFSET 0"));
  }

  @Test
  public void shouldConstructCriteria() {
    String id = "000000000000000";
    Criteria criteria = DaoUtil.constructCriteria("id", id);
    assertNotNull(criteria);
    assertEquals("id", criteria.getField().get(0));
    assertEquals("=", criteria.getOperation());
    assertEquals(id, criteria.getValue());
  }
}
