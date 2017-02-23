package com.sequenceiq.it.cloudbreak.recovery;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

public class AwsManualRecoveryTest extends AbstractManualRecoveryTest {
    @BeforeMethod
    @Parameters("region")
    public void setCloudProviderParameters(@Optional String region) {
        getCloudProviderParams().put("cloudProvider", "AWS");
        getCloudProviderParams().put("region", region);
    }
}
