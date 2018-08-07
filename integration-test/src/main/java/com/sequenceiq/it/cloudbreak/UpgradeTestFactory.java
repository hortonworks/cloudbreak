package com.sequenceiq.it.cloudbreak;

import java.util.Arrays;

import org.testng.annotations.Factory;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider;

public class UpgradeTestFactory extends CloudbreakTest {
    @Factory
    @Parameters("providers")
    public Object[] clusterTestFactory(@Optional(GcpCloudProvider.GCP) String providers) {
        CloudProvider[] cloudProviders = CloudProviderHelper.providersFactory(providers, getTestParameter());
        Object[] results = Arrays.stream(cloudProviders)
                .map(provider -> new UpgradeTests(provider, getTestParameter()))
                .toArray();
        return results;
    }
}
