package com.sequenceiq.it.cloudbreak.recovery;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.amazonaws.regions.Regions;

public class AwsManualRecoveryTest extends AbstractManualRecoveryTest {
    @BeforeMethod
    @Parameters("region")
    public void setCloudProviderParameters(@Optional Regions region) {
        getCloudProviderParams().put("cloudProvider", "AWS");
        getCloudProviderParams().put("region", region.getName());
    }
}
