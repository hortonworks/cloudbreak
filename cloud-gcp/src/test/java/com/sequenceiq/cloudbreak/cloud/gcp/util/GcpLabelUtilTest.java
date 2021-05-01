package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

class GcpLabelUtilTest {
    private static final String ENVIRONMENT_CRN = "Cloudera-Environment-Resource-Name";

    private static final String CREATOR_CRN = "Cloudera-Creator-Resource-Name";

    private static final String RESOURCE_CRN = "Cloudera-Resource-Name";

    private GcpLabelUtil gcpLabelUtil = new GcpLabelUtil();

    @Test
    void createLabelsFromTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(CREATOR_CRN, "crn:altus:timbuk2:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:05ca1026-c028-466b-8943-b04f765fa3f6");
        tags.put(RESOURCE_CRN, "crn:cdp:freeipa:us-west-2:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:freeipa:8111d534-8c7e-4a68-a8ba-7ebb389a3a20");
        tags.put(ENVIRONMENT_CRN, "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:12474ddc-6e44-4f4c-806a-b197ef12cbb8");
        CloudStack cloudStack = new CloudStack(new HashSet<>(), null, null, new HashMap<>(), tags, null, null, null, null, null);

        Map<String, String> result = gcpLabelUtil.createLabelsFromTags(cloudStack);

        assertEquals(3L, result.size());
        assertEquals("12474ddc-6e44-4f4c-806a-b197ef12cbb8", result.get(ENVIRONMENT_CRN.toLowerCase()));
        assertEquals("5d7-b645-7ccf9edbb73d-user-05ca1026-c028-466b-8943-b04f765fa3f6", result.get(CREATOR_CRN.toLowerCase()));
        assertEquals("-b645-7ccf9edbb73d-freeipa-8111d534-8c7e-4a68-a8ba-7ebb389a3a20", result.get(RESOURCE_CRN.toLowerCase()));
    }

    @Test
    void transformValueWhenItsLengthLessThan63Chars() {
        String originalValue = "gpc-test-value-which-is-short";

        String result = gcpLabelUtil.transformLabelKeyOrValue(originalValue);

        assertEquals(originalValue, result);
    }

    @Test
    void transformValueWhenItsLengthMoreThan63Chars() {
        String originalValue = "gcp-dev-cloudbreak-gcp-test-9257bdae358342cca05e674b3893563-387";

        String result = gcpLabelUtil.transformLabelKeyOrValue(originalValue);

        assertEquals(GcpLabelUtil.GCP_MAX_TAG_LEN, result.length());
        assertEquals(originalValue.substring(originalValue.length() - GcpLabelUtil.GCP_MAX_TAG_LEN), result);
    }

    @Test
    void transformValueWhenItsLengthMoreThan63CharsAndItIsACrn() {
        String originalValue = "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:12474ddc-6e44-4f4c-806a-b197ef12cbb8";

        String result = gcpLabelUtil.transformLabelKeyOrValue(originalValue);

        assertTrue(result.length() < GcpLabelUtil.GCP_MAX_TAG_LEN);
        assertEquals("12474ddc-6e44-4f4c-806a-b197ef12cbb8", result);
    }
}