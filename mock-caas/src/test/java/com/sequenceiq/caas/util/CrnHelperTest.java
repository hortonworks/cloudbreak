package com.sequenceiq.caas.util;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.caas.model.AltusKey;

public class CrnHelperTest {

    @Test
    public void extractCrnFromAuthHeader() {
        String result = CrnHelper.extractCrnFromAuthHeader("eyJhY2Nlc3Nfa2V5X2lkIjoiWTNKdU9tRnNkSFZ6T21saGJUcDFjeTEzWlhOMExURTZZMnh2ZFdSbGNtRT"
                + "ZkWE5sY2pwMWMyVnkiLCJhdXRoX21ldGhvZCI6ImVkMjU1MTl2MSJ9.SIGNATURE_NOT_HANDLED");
        Assert.assertEquals("crn:altus:iam:us-west-1:cloudera:user:user", result);
    }

    @Test
    public void generateAltusApiKeyTest() {
        AltusKey k = CrnHelper.generateAltusApiKey("cloudera", "user");
        Assert.assertEquals("Y3JuOmFsdHVzOmlhbTp1cy13ZXN0LTE6Y2xvdWRlcmE6dXNlcjp1c2Vy", k.getAccessKeyId());
    }
}