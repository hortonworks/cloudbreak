package com.sequenceiq.cloudbreak.clusterdefinition.validation;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;

public class StackServiceComponentDescriptorsTest {

    @Test
    public void testStackServiceComponentDescriptorsTestWhenInitialize() {
        Map<String, StackServiceComponentDescriptor> stackServiceComponentDescriptorsMap = Maps.newHashMap();
        StackServiceComponentDescriptor namenode = new StackServiceComponentDescriptor("namenode", "1+", 1, 4);
        stackServiceComponentDescriptorsMap.put("master", namenode);

        StackServiceComponentDescriptors stackServiceComponentDescriptors = new StackServiceComponentDescriptors(stackServiceComponentDescriptorsMap);

        StackServiceComponentDescriptor master = stackServiceComponentDescriptors.get("master");
        Assert.assertEquals(namenode, stackServiceComponentDescriptors.get("master"));
        Assert.assertNotNull(master.isMaster());
        Assert.assertNotNull(master.getName());
        Assert.assertNotNull(master.getMaxCardinality());
        Assert.assertNotNull(master.getMinCardinality());
        Assert.assertNotNull(master.getCategory());
        Assert.assertNull(stackServiceComponentDescriptors.get("master1"));
    }

}