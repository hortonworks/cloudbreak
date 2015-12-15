package com.sequenceiq.cloudbreak.shell.support;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderObjectValueMap;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry;

public class TableRendererTest {


    @Test
    @Ignore
    public void testRenderObjectValueMap() {
        Map<String, Object> testValues = new HashMap<>();
        testValues.put("master1", new InstanceGroupEntry(10L, 10, "hostgroup"));
        testValues.put("master2", new InstanceGroupEntry(10L, 10, "hostgroup"));
        testValues.put("master3", new InstanceGroupEntry(10L, 10, "hostgroup"));
        testValues.put("master4", new InstanceGroupEntry(10L, 10, "hostgroup"));

        String instanceGroup = renderObjectValueMap(testValues, "instanceGroup");
        System.out.println(instanceGroup);
    }
}