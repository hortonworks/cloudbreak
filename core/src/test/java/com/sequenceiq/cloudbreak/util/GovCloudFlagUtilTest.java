package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.workspace.model.User;

public class GovCloudFlagUtilTest {

    @Test
    public void testNullObject() {
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(null));
    }

    @Test
    public void testBooleanObject() {
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(false));
        assertTrue(GovCloudFlagUtil.extractGovCloudFlag(true));
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(Boolean.FALSE));
        assertTrue(GovCloudFlagUtil.extractGovCloudFlag(Boolean.TRUE));
    }

    @Test
    public void testStringObject() {
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag("false"));
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag("anythingelse"));
        assertTrue(GovCloudFlagUtil.extractGovCloudFlag("true"));
    }

    @Test
    public void testDifferentObject() {
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(new User()));
        assertFalse(GovCloudFlagUtil.extractGovCloudFlag(Credential.builder().build()));
    }

}
