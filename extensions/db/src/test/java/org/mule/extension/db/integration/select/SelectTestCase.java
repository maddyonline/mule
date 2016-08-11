/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.select;

import static org.mule.extension.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.extension.db.integration.TestRecordUtil.getAllPlanetRecords;
import static org.mule.extension.db.integration.TestRecordUtil.getEarthRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getMarsRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getVenusRecord;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.api.message.MuleMessage;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectTestCase extends AbstractDbIntegrationTestCase {

  public SelectTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-config.xml"};
  }

  @Test
  public void select() throws Exception {
    MuleMessage response = flowRunner("select").run().getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Test
  public void fixedParam() throws Exception {
    MuleMessage response = flowRunner("fixedParam").run().getMessage();
    assertMessageContains(response, getMarsRecord());
  }

  @Test
  public void expressionAndFixedParamMixed() throws Exception {
    MuleMessage response = flowRunner("expressionAndFixedParamMixed").run().getMessage();
    assertMessageContains(response, getEarthRecord());
  }

  @Test
  public void dynamicQuery() throws Exception {
    MuleMessage response = flowRunner("dynamicQuery").run().getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Test
  public void maxRows() throws Exception {
    MuleMessage response = flowRunner("selectMaxRows").run().getMessage();
    assertMessageContains(response, getVenusRecord(), getEarthRecord());
  }

  @Test
  public void limitsStreamedRows() throws Exception {
    MuleMessage response = flowRunner("selectMaxStreamedRows").run().getMessage();
    assertMessageContains(response, getVenusRecord(), getEarthRecord());
  }

}
