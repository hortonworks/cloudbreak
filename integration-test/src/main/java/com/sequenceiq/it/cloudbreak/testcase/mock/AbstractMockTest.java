package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;

import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class AbstractMockTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMockTest.class);

    @Inject
    private ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure;

    @Override
    protected void renewTestAndSetup(Object[] data, ITestResult testResult) {
        Log.log(LOGGER, "New test inited: %s", testResult.getMethod().getMethodName());
        getExecuteQueryToMockInfrastructure().call("/tests/new", w -> w);
    }

    public ExecuteQueryToMockInfrastructure getExecuteQueryToMockInfrastructure() {
        return executeQueryToMockInfrastructure;
    }
}
