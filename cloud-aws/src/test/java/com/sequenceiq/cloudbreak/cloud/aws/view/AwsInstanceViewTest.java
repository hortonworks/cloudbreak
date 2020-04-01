package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class AwsInstanceViewTest {

    @Test
    public void testTemplateParameters() {
        Map<String, Object> map = new HashMap<>();
        map.put("encrypted", true);
        map.put("type", "CUSTOM");

        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                map, 0L, "imageId");

        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);

        Assert.assertTrue(actual.isKmsCustom());
    }

    @Test
    public void testOnDemand() {
        Map<String, Object> map = new HashMap<>();
        map.put("spotPercentage", 30);
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                map, 0L, "imageId");
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertEquals(70, actual.getOnDemandPercentage());
    }

    @Test
    public void testOnDemandMissingPercentage() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                Map.of(), 0L, "imageId");
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertEquals(100, actual.getOnDemandPercentage());
    }
}
