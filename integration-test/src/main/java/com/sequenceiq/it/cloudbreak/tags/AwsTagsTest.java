package com.sequenceiq.it.cloudbreak.tags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.amazonaws.regions.Regions;


public class AwsTagsTest extends AbstractTagTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsTagsTest.class);

    @BeforeMethod
    @Parameters({"region"})
    @Test
    public void checkAwsTags(Regions region) {
        getCloudProviderParams().put("cloudProvider", "AWS");
        getCloudProviderParams().put("region", region.getName());
    }
}