/*
 *
 * MariaDB Client for Java
 *
 * Copyright (c) 2012-2014 Monty Program Ab.
 * Copyright (c) 2015-2020 MariaDB Corporation Ab.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to Monty Program Ab info@montyprogram.com.
 *
 * This particular MariaDB Client for Java file is work
 * derived from a Drizzle-JDBC. Drizzle-JDBC file which is covered by subject to
 * the following copyright and notice provisions:
 *
 * Copyright (c) 2009-2011, Marcus Eriksson
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of the driver nor the names of its contributors may not be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS  AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */

package org.mariadb.jdbc;

import static org.junit.Assert.*;

import java.sql.*;
import org.junit.*;

public class CallStatementTest extends BaseTest {

  @BeforeClass()
  public static void initClass() throws SQLException {
    // error MXS-3929 for maxscale 6.2.0
    Assume.assumeTrue(
        !sharedConnection.getMetaData().getDatabaseProductVersion().contains("maxScale-6.2.0"));

    drop();
    try (Statement stmt = sharedConnection.createStatement()) {
      stmt.execute("CREATE PROCEDURE useParameterName(a int) begin select a; end");
      stmt.execute("CREATE PROCEDURE useWrongParameterName(a int) begin select a; end");
      stmt.execute("CREATE PROCEDURE multiResultSets() BEGIN  SELECT 1; SELECT 2; END");
      stmt.execute("CREATE PROCEDURE inoutParam(INOUT p1 INT) begin set p1 = p1 + 1; end\n");
      stmt.execute("CREATE PROCEDURE testGetProcedures(INOUT p1 INT) begin set p1 = p1 + 1; end\n");
      stmt.execute("CREATE PROCEDURE withStrangeParameter(IN a DECIMAL(10,2)) begin select a; end");
      stmt.execute(
          "CREATE PROCEDURE TEST_SP1() BEGIN\n"
              + "SELECT @Something := 'Something';\n"
              + "SIGNAL SQLSTATE '70100'\n"
              + "SET MESSAGE_TEXT = 'Test error from SP'; \n"
              + "END");
      stmt.execute("CREATE PROCEDURE testCallWithFetchSize()\nBEGIN\nSELECT 1;SELECT 2;\nEND");
      stmt.execute("CREATE PROCEDURE prepareWithNoParameters()\nbegin\n    SELECT 'mike';end\n");
      stmt.execute("CREATE PROCEDURE testMetaCatalog(x int, out y int)\nBEGIN\nSELECT 1;end\n");
      stmt.execute("CREATE PROCEDURE callabletest1()\nBEGIN\nSELECT 1;end\n");
      stmt.execute("CREATE PROCEDURE stmtSimple(IN p1 INT, IN p2 INT) begin SELECT p1 + p2; end\n");
      stmt.execute(
          "CREATE PROCEDURE prepareStmtSimple(IN p1 INT, IN p2 INT) begin SELECT p1 + p2; end\n");
      stmt.execute(
          "CREATE FUNCTION stmtSimpleFunction(a float, b bigint, c int) RETURNS INT NO SQL\nBEGIN\nRETURN a;\nEND");
      stmt.execute(
          "CREATE PROCEDURE prepareStmtWithOutParameter(x int, INOUT y int)\nBEGIN\nSELECT 1;end\n");
      stmt.execute("CREATE PROCEDURE testMetaWildcard(x int, out y int)\nBEGIN\nSELECT 1;end\n");

      stmt.execute("FLUSH TABLES");
    }
  }

  @AfterClass
  public static void drop() throws SQLException {
    try (Statement stmt = sharedConnection.createStatement()) {
      stmt.execute("DROP PROCEDURE IF EXISTS useParameterName");
      stmt.execute("DROP PROCEDURE IF EXISTS useWrongParameterName");
      stmt.execute("DROP PROCEDURE IF EXISTS multiResultSets");
      stmt.execute("DROP PROCEDURE IF EXISTS inoutParam");
      stmt.execute("DROP PROCEDURE IF EXISTS testGetProcedures");
      stmt.execute("DROP PROCEDURE IF EXISTS withStrangeParameter");
      stmt.execute("DROP PROCEDURE IF EXISTS TEST_SP1");
      stmt.execute("DROP PROCEDURE IF EXISTS testCallWithFetchSize");
      stmt.execute("DROP PROCEDURE IF EXISTS prepareWithNoParameters");
      stmt.execute("DROP PROCEDURE IF EXISTS testMetaCatalog");
      stmt.execute("DROP PROCEDURE IF EXISTS callabletest1");
      stmt.execute("DROP PROCEDURE IF EXISTS stmtSimple");
      stmt.execute("DROP PROCEDURE IF EXISTS prepareStmtSimple");
      stmt.execute("DROP FUNCTION IF EXISTS stmtSimpleFunction");
      stmt.execute("DROP PROCEDURE IF EXISTS prepareStmtWithOutParameter");
      stmt.execute("DROP PROCEDURE IF EXISTS testMetaWildcard");
    }
  }

  @Before
  public void checkSp() throws SQLException {
    requireMinimumVersion(5, 0);
  }

  @Test
  public void stmtSimple() throws SQLException {
    ResultSet rs = sharedConnection.createStatement().executeQuery("{call stmtSimple(2,2)}");
    assertTrue(rs.next());
    int result = rs.getInt(1);
    assertEquals(result, 4);
  }

  @Test
  public void prepareStmtSimple() throws SQLException {
    PreparedStatement preparedStatement =
        sharedConnection.prepareStatement("{call prepareStmtSimple(?,?)}");
    preparedStatement.setInt(1, 2);
    preparedStatement.setInt(2, 2);
    ResultSet rs = preparedStatement.executeQuery();
    assertTrue(rs.next());
    int result = rs.getInt(1);
    assertEquals(result, 4);
  }

  @Test
  public void stmtSimpleFunction() throws SQLException {
    try {
      sharedConnection.createStatement().execute("{call stmtSimpleFunction(2,2,2)}");
      fail("call mustn't work for function, use SELECT <function>");
    } catch (SQLSyntaxErrorException sqle) {
      assertTrue(
          "error : " + sqle.getMessage(),
          sqle.getMessage().contains("stmtSimpleFunction does not exist"));
    }
  }

  @Test
  public void prepareStmtSimpleFunction() throws SQLException {
    try {
      PreparedStatement preparedStatement =
          sharedConnection.prepareStatement("{call stmtSimpleFunction(?,?,?)}");
      preparedStatement.setInt(1, 2);
      preparedStatement.setInt(2, 2);
      preparedStatement.setInt(3, 2);
      preparedStatement.execute();
      fail("call mustn't work for function, use SELECT <function>");
    } catch (SQLSyntaxErrorException sqle) {
      assertTrue(
          "error : " + sqle.getMessage(),
          sqle.getMessage().contains("stmtSimpleFunction does not exist"));
    }
  }

  @Test
  public void functionParameterByNameError() throws SQLException {
    try (CallableStatement call =
        sharedConnection.prepareCall("{? = call stmtSimpleFunction(?,?,?)}")) {
      call.setInt("a", 8);
      call.setInt(3, 2);
      call.setInt(4, 2);
      call.execute();

      try {
        call.getString("b");
        fail("must have thrown exception");
      } catch (SQLDataException ee) {
        assertTrue(ee.getMessage().contains("No such column"));
      }

      try {
        call.getString("d");
        fail("must have thrown exception");
      } catch (SQLSyntaxErrorException ee) {
        assertTrue(ee.getMessage().contains("there is no parameter with the name d"));
      }
      assertEquals(8, call.getInt(1));
    }
  }

  @Test
  public void prepareStmtWithOutParameter() throws SQLException {
    Assume.assumeTrue(sharedUsePrepare());
    PreparedStatement preparedStatement =
        sharedConnection.prepareStatement("{call prepareStmtWithOutParameter(?,?)}");
    preparedStatement.setInt(1, 2);
    preparedStatement.setInt(2, 3);
    assertTrue(preparedStatement.execute());
  }

  @Test
  public void prepareBatchMultiResultSets() throws Exception {
    PreparedStatement stmt = sharedConnection.prepareStatement("{call multiResultSets()}");
    stmt.addBatch();
    stmt.addBatch();
    try {
      stmt.executeBatch();
    } catch (SQLException e) {
      assertTrue(
          e.getMessage().contains("Select command are not permitted via executeBatch() command"));
    }
  }

  @Test
  public void stmtMultiResultSets() throws Exception {
    Statement stmt = sharedConnection.createStatement();
    stmt.execute("{call multiResultSets()}");
    ResultSet rs = stmt.getResultSet();
    assertTrue(rs.next());
    assertEquals(1, rs.getInt(1));
    assertFalse(rs.next());
    assertTrue(stmt.getMoreResults());
    rs = stmt.getResultSet();
    assertTrue(rs.next());
    assertEquals(2, rs.getInt(1));
    assertFalse(rs.next());
    assertFalse(stmt.getMoreResults());
  }

  @Test
  public void prepareStmtMultiResultSets() throws Exception {
    PreparedStatement stmt = sharedConnection.prepareStatement("{call multiResultSets()}");
    stmt.execute();
    ResultSet rs = stmt.getResultSet();
    assertTrue(rs.next());
    assertEquals(1, rs.getInt(1));
    assertFalse(rs.next());
    assertTrue(stmt.getMoreResults());
    rs = stmt.getResultSet();
    assertTrue(rs.next());
    assertEquals(2, rs.getInt(1));
    assertFalse(rs.next());
    assertFalse(stmt.getMoreResults());
  }

  @Test
  public void stmtInoutParam() throws SQLException {
    try (Connection connection = setConnection("&dumpQueriesOnException")) {
      // to ensure not having too many connection error for maxscale
      connection.isValid(1);
    }
    try (Connection connection = setConnection("&dumpQueriesOnException")) {
      Statement stmt = connection.createStatement();
      stmt.execute("{call inOutParam(1)}");
      fail("must fail : statement cannot be use when there is out parameter");
    } catch (SQLSyntaxErrorException e) {
      assertTrue(
          e.getMessage().contains("OUT or INOUT argument 1 for routine")
              && e.getMessage()
                  .contains("is not a variable or NEW pseudo-variable in BEFORE trigger")
              && e.getMessage().contains("Query is: call inOutParam(1)"));
    }
  }

  @Test
  public void prepareStmtInoutParam() throws SQLException {
    Assume.assumeTrue(sharedUsePrepare());
    // must work, but out parameter isn't accessible
    PreparedStatement preparedStatement = sharedConnection.prepareStatement("{call inOutParam(?)}");
    preparedStatement.setInt(1, 1);
    preparedStatement.execute();
  }

  @Test
  public void getProcedures() throws SQLException {
    ResultSet rs = sharedConnection.getMetaData().getProcedures(null, null, "testGetProc%");
    ResultSetMetaData md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) {
      md.getColumnLabel(i);
    }

    while (rs.next()) {
      for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
        rs.getObject(i);
      }
    }
  }

  @Test
  public void meta() throws Exception {
    ResultSet rs = sharedConnection.getMetaData().getProcedures(null, null, "callabletest1");
    if (rs.next()) {
      assertTrue("callabletest1".equals(rs.getString(3)));
    } else {
      fail();
    }
  }

  @Test
  public void testMetaWildcard() throws Exception {
    ResultSet rs =
        sharedConnection.getMetaData().getProcedureColumns(null, null, "testMetaWildcard%", "%");
    if (rs.next()) {
      assertEquals("testMetaWildcard", rs.getString(3));
      assertEquals("x", rs.getString(4));

      assertTrue(rs.next());
      assertEquals("testMetaWildcard", rs.getString(3));
      assertEquals("y", rs.getString(4));
      assertFalse(rs.next());
    }
  }

  @Test
  public void testMetaCatalog() throws Exception {
    ResultSet rs =
        sharedConnection
            .getMetaData()
            .getProcedures(sharedConnection.getCatalog(), null, "testMetaCatalog");
    assertTrue(rs.next());
    assertTrue("testMetaCatalog".equals(rs.getString(3)));
    assertFalse(rs.next());

    // test with bad catalog
    rs = sharedConnection.getMetaData().getProcedures("yahoooo", null, "testMetaCatalog");
    assertFalse(rs.next());

    // test without catalog
    rs = sharedConnection.getMetaData().getProcedures(null, null, "testMetaCatalog");
    assertTrue(rs.next());
    assertTrue("testMetaCatalog".equals(rs.getString(3)));
    assertFalse(rs.next());
  }

  @Test
  public void prepareWithNoParameters() throws SQLException {
    PreparedStatement preparedStatement =
        sharedConnection.prepareStatement("{call prepareWithNoParameters()}");
    ResultSet rs = preparedStatement.executeQuery();
    assertTrue(rs.next());
    assertEquals("mike", rs.getString(1));
  }

  @Test
  public void testCallWithFetchSize() throws SQLException {
    try (Statement statement = sharedConnection.createStatement()) {
      statement.setFetchSize(1);
      try (ResultSet resultSet = statement.executeQuery("CALL testCallWithFetchSize()")) {
        int rowCount = 0;
        while (resultSet.next()) {
          rowCount++;
        }
        assertEquals(1, rowCount);
      }
      statement.execute("SELECT 1");
    }
  }
}
