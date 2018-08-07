package com.sequenceiq.it.cloudbreak;

import java.util.Arrays;

import org.testng.annotations.Factory;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class ClusterTestFactory extends CloudbreakTest {
    @Factory
    @Parameters("providers")
    public Object[] clusterTestFactory(@Optional(OpenstackCloudProvider.OPENSTACK) String providers) {
        CloudProvider[] cloudProviders = CloudProviderHelper.providersFactory(providers, getTestParameter());
        Object[] results = Arrays.stream(cloudProviders)
                .map(provider -> new ClusterTestsSimple(provider, getTestParameter()))
                .toArray();
        return results;
    }

}
