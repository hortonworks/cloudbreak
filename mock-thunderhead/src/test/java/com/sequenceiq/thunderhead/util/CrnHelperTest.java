package com.sequenceiq.thunderhead.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.thunderhead.model.AltusKey;

class CrnHelperTest {

    @Test
    void extractCrnFromAuthHeader() {
        String result = CrnHelper.extractCrnFromAuthHeader("eyJhY2Nlc3Nfa2V5X2lkIjoiWTNKdU9tTmtjRHBwWVcwNmRYTXRkMlZ6ZEMweE9tTnNi"
                + "M1ZrWlhKaE9uVnpaWEk2ZFhObGNnPT0iLCJhdXRoX21ldGhvZCI6ImVkMjU1MTl2MSJ9.SIGNATURE_NOT_HANDLED");
        assertEquals("crn:cdp:iam:us-west-1:cloudera:user:user", result);
    }

    @Test
    void generateAltusApiKeyTest() {
        AltusKey k = CrnHelper.generateAltusApiKey("cloudera", "user");
        assertEquals("Y3JuOmNkcDppYW06dXMtd2VzdC0xOmNsb3VkZXJhOnVzZXI6dXNlcg==", k.getAccessKeyId());
    }
}
