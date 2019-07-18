package com.sequenceiq.cloudbreak.cloud.aws.util;

import org.junit.Assert;
import org.junit.Test;

public class ArnTest {

    private static final String PARTITION = "partition";

    private static final String SERVICE = "service";

    private static final String REGION = "region";

    private static final String ACCOUNTID = "account";

    private static final String RESOURCE = "resource";

    @Test
    public void ofCreatesFully() {
        Arn result = Arn.of(String.format("%s:%s:%s:%s:%s", PARTITION, SERVICE, REGION, ACCOUNTID, RESOURCE));
        Assert.assertEquals(PARTITION, result.getPartition());
        Assert.assertEquals(SERVICE, result.getService());
        Assert.assertEquals(REGION, result.getRegion());
        Assert.assertEquals(ACCOUNTID, result.getAccountId());
        Assert.assertEquals(RESOURCE, result.getResource());
    }

    @Test
    public void ofWithEmptyPartsReturnsEmptyStrings() {
        Arn result = Arn.of("::::");
        Assert.assertEquals("", result.getPartition());
        Assert.assertEquals("", result.getService());
        Assert.assertEquals("", result.getRegion());
        Assert.assertEquals("", result.getAccountId());
        Assert.assertEquals("", result.getResource());
    }
}
