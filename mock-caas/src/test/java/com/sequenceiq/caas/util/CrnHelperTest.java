package com.sequenceiq.caas.util;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.caas.model.AltusKey;

public class CrnHelperTest {

    @Test
    public void extractCrnFromAuthHeader() {
        String result = CrnHelper.extractCrnFromAuthHeader("eyJhY2Nlc3Nfa2V5X2lkIjoiWTNKdU9tTmtjRHBwWVcwNmRYTXRkMlZ6ZEMweE9tTnNi"
                + "M1ZrWlhKaE9uVnpaWEk2ZFhObGNnPT0iLCJhdXRoX21ldGhvZCI6ImVkMjU1MTl2MSJ9.SIGNATURE_NOT_HANDLED");
        Assert.assertEquals("crn:cdp:iam:us-west-1:cloudera:user:user", result);
    }

    @Test
    public void generateAltusApiKeyTest() {
        AltusKey k = CrnHelper.generateAltusApiKey("cloudera", "user");
        Assert.assertEquals("Y3JuOmNkcDppYW06dXMtd2VzdC0xOmNsb3VkZXJhOnVzZXI6dXNlcg==", k.getAccessKeyId());
    }
}
