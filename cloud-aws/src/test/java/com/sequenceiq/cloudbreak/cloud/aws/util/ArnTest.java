package com.sequenceiq.cloudbreak.cloud.aws.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ArnTest {

    private static final String PREFIX = "arn";

    private static final String PARTITION = "partition";

    private static final String SERVICE = "service";

    private static final String REGION = "region";

    private static final String ACCOUNTID = "account";

    private static final String RESOURCE = "resource";

    private static final String RESOURCE_EXTRA = "resource/sub:subsub/item";

    private static final String ARN_MUST_NOT_BE_EMPTY = "ARN must not be empty.";

    private static final String ARN_HAS_INVALID_FORMAT = "ARN has invalid format.";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void ofCreatesFully() {
        Arn result = Arn.of(String.format("%s:%s:%s:%s:%s:%s", PREFIX, PARTITION, SERVICE, REGION, ACCOUNTID, RESOURCE));
        Assert.assertEquals(PREFIX, result.getPrefix());
        Assert.assertEquals(PARTITION, result.getPartition());
        Assert.assertEquals(SERVICE, result.getService());
        Assert.assertEquals(REGION, result.getRegion());
        Assert.assertEquals(ACCOUNTID, result.getAccountId());
        Assert.assertEquals(RESOURCE, result.getResource());
    }

    @Test
    public void ofCreatesFullyExtraResource() {
        Arn result = Arn.of(String.format("%s:%s:%s:%s:%s:%s", PREFIX, PARTITION, SERVICE, REGION, ACCOUNTID, RESOURCE_EXTRA));
        Assert.assertEquals(PREFIX, result.getPrefix());
        Assert.assertEquals(PARTITION, result.getPartition());
        Assert.assertEquals(SERVICE, result.getService());
        Assert.assertEquals(REGION, result.getRegion());
        Assert.assertEquals(ACCOUNTID, result.getAccountId());
        Assert.assertEquals(RESOURCE_EXTRA, result.getResource());
    }

    @Test
    public void ofWithEmptyPartsReturnsEmptyStrings() {
        Arn result = Arn.of(String.format("%s:%s:%s:::%s", PREFIX, PARTITION, SERVICE, RESOURCE_EXTRA));
        Assert.assertEquals(PREFIX, result.getPrefix());
        Assert.assertEquals(PARTITION, result.getPartition());
        Assert.assertEquals(SERVICE, result.getService());
        Assert.assertEquals("", result.getRegion());
        Assert.assertEquals("", result.getAccountId());
        Assert.assertEquals(RESOURCE_EXTRA, result.getResource());
    }

    @Test
    public void illegalThrownForNull() {
        expectIllegalWithMessage(null, ARN_MUST_NOT_BE_EMPTY);
    }

    @Test
    public void illegalThrownForEmpty() {
        expectIllegalWithMessage("", ARN_MUST_NOT_BE_EMPTY);
    }

    @Test
    public void illegalThrownForBlank() {
        expectIllegalWithMessage("  ", ARN_MUST_NOT_BE_EMPTY);
    }

    @Test
    public void illegalThrownForMissingParts() {
        expectIllegalWithMessage("arn", ARN_HAS_INVALID_FORMAT);
    }

    @Test
    public void illegalThrownForMissingParts2() {
        String arn = String.format("%s:%s:%s:%s:%s", PREFIX, PARTITION, SERVICE, REGION, ACCOUNTID);
        expectIllegalWithMessage(arn, ARN_HAS_INVALID_FORMAT);
    }

    private void expectIllegalWithMessage(String arn, String message) {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(message);
        Arn.of(arn);
    }
}
