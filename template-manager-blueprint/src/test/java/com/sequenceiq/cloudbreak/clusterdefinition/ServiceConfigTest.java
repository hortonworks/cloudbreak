package com.sequenceiq.cloudbreak.clusterdefinition;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ServiceConfigTest {

    @Test
    public void serviceConfigTestWhenInitialized() {
        ServiceConfig serviceConfig = new ServiceConfig("NAMENODE", Lists.newArrayList(), Maps.newHashMap(), Maps.newHashMap());

        Assert.assertNotNull(serviceConfig.getGlobalConfig());
        Assert.assertNotNull(serviceConfig.getHostGroupConfig());
        Assert.assertNotNull(serviceConfig.getRelatedServices());
        Assert.assertNotNull(serviceConfig.getServiceName());
    }

}