package com.sequenceiq.cloudbreak.cloud.aws.view;

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
                map, 0L);

        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);

        Assert.assertTrue(actual.isKmsCustom());
    }
}
