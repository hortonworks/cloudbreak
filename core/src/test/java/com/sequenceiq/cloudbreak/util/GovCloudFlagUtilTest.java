package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.workspace.model.User;

class GovCloudFlagUtilTest {

    @Test
    void testNullObject() {
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(null));
    }

    @Test
    void testBooleanObject() {
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(false));
        assertTrue(GovCloudFlagUtil.extractGovCloudFlag(true));
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(Boolean.FALSE));
        assertTrue(GovCloudFlagUtil.extractGovCloudFlag(Boolean.TRUE));
    }

    @Test
    void testStringObject() {
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag("false"));
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag("anythingelse"));
        assertTrue(GovCloudFlagUtil.extractGovCloudFlag("true"));
    }

    @Test
    void testDifferentObject() {
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(new User()));
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(Credential.builder().build()));
    }

}
