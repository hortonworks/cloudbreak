package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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

    @Test
    public void ofCreatesFully() {
        Arn result = Arn.of(String.format("%s:%s:%s:%s:%s:%s", PREFIX, PARTITION, SERVICE, REGION, ACCOUNTID, RESOURCE));
        assertEquals(PREFIX, result.getPrefix());
        assertEquals(PARTITION, result.getPartition());
        assertEquals(SERVICE, result.getService());
        assertEquals(REGION, result.getRegion());
        assertEquals(ACCOUNTID, result.getAccountId());
        assertEquals(RESOURCE, result.getResource());
    }

    @Test
    public void ofCreatesFullyExtraResource() {
        Arn result = Arn.of(String.format("%s:%s:%s:%s:%s:%s", PREFIX, PARTITION, SERVICE, REGION, ACCOUNTID, RESOURCE_EXTRA));
        assertEquals(PREFIX, result.getPrefix());
        assertEquals(PARTITION, result.getPartition());
        assertEquals(SERVICE, result.getService());
        assertEquals(REGION, result.getRegion());
        assertEquals(ACCOUNTID, result.getAccountId());
        assertEquals(RESOURCE_EXTRA, result.getResource());
    }

    @Test
    public void ofWithEmptyPartsReturnsEmptyStrings() {
        Arn result = Arn.of(String.format("%s:%s:%s:::%s", PREFIX, PARTITION, SERVICE, RESOURCE_EXTRA));
        assertEquals(PREFIX, result.getPrefix());
        assertEquals(PARTITION, result.getPartition());
        assertEquals(SERVICE, result.getService());
        assertEquals("", result.getRegion());
        assertEquals("", result.getAccountId());
        assertEquals(RESOURCE_EXTRA, result.getResource());
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
        Exception exception = assertThrows(IllegalArgumentException.class, () -> Arn.of(arn));
        assertEquals(message, exception.getMessage());
    }
}
