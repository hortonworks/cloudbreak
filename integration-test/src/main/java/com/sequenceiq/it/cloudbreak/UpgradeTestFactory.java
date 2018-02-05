package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.util.stream.IntStream;

public class UpgradeTestFactory extends CloudbreakTest {
    @Factory
    @Parameters({"providers"})
    public Object[] clusterTestFactory(@Optional(GcpCloudProvider.GCP) String providers) {
        CloudProvider[] cloudProviders = CloudProviderHelper.providerFactory(providers, getTestParameter());
        Object[] results = new Object[cloudProviders.length];
        IntStream.range(0, results.length).forEach(index -> results[index] = new UpgradeTests(cloudProviders[index],
                getTestParameter()));
        return results;
    }
}
