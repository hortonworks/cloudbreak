package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Region;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import java.util.Collection;

public class RegionProviderSpecTests extends CloudbreakTest {

    private static final String AZURE_CRED_NAME = "autotesting-regions-spec-azure";

    private static final String AWS_CRED_NAME = "autotesting-regions-spec-aws";

    private static final String GCP_CRED_NAME = "autotesting-regions-spec-gcp";

    private static final String OS_CRED_NAME = "autotesting-regions-spec-os";

    private static final Logger LOGGER = LoggerFactory.getLogger(RegionProviderSpecTests.class);

    public RegionProviderSpecTests() {
    }

    public RegionProviderSpecTests(TestParameter tp) {
        setTestParameter(tp);
    }

    private void getSupportedAvailabilityZones(CloudProvider provider, String credentialName) throws Exception {
        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(credentialName), provider.getPlatform() + " credential is created");
        given(Region.request(), provider.getPlatform() + " region request");
        when(Region.getPlatformRegionsWithRetry(Integer.parseInt(getTestParameter().get("retryQuantity"))), "Regions are requested to " + provider.getPlatform()
                + " credential");
        then(Region.assertThis(
                (region, t) -> {
                    Collection<String> availibilityZones = region.getRegionV4Response().getAvailabilityZones()
                            .get(provider.region());

                    availibilityZones.forEach(zone ->
                            LOGGER.info("{} Default Region's Availibility Zone is ::: {}",
                                    provider.getPlatform(), zone));
                    Assert.assertFalse(availibilityZones.isEmpty());
                }), "Default Region Availibility Zones should be part of the response."
        );
    }

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {AZURE_CRED_NAME, AWS_CRED_NAME, GCP_CRED_NAME, OS_CRED_NAME};

        for (String aNameArray : nameArray) {
            LOGGER.info("Delete credential: \'{}\'", aNameArray.toLowerCase().trim());
            try {
                given(CloudbreakClient.created());
                given(Credential.request()
                        .withName(aNameArray));
                when(Credential.delete());
            } catch (ForbiddenException | BadRequestException e) {
                String exceptionMessage = e.getResponse().readEntity(String.class);
                String errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("Clean Up Exception message ::: {}", errorMessage);
            }
        }
    }

    @Test(priority = 1, groups = "regions")
    private void testAzureAvailabilityZoneSupport() throws Exception {
        AzureCloudProvider provider = new AzureCloudProvider(getTestParameter());

        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(AZURE_CRED_NAME), "Azure credential is created");
        given(Region.request(), "Azure region request");
        when(Region.getPlatformRegionsWithRetry(Integer.parseInt(getTestParameter().get("retryQuantity"))), "Regions are requested to Azure credential");
        then(Region.assertThis(
                (region, t) -> {
                    Collection<String> availibilityZones = region.getRegionV4Response().getAvailabilityZones()
                            .get(provider.region());

                    LOGGER.info("Azure Default Region's Availibility Zone is not supported.");
                    Assert.assertTrue(availibilityZones.isEmpty());
                }), "Azure Default Region Availibility Zones should not be part of the response."
        );
    }

    @Test(priority = 2, groups = "regions")
    public void testAWSListAvailabilityZonesForDefaultRegion() throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        getSupportedAvailabilityZones(provider, AWS_CRED_NAME);
    }

    @Test(priority = 3, groups = "regions")
    public void testGCPListAvailabilityZonesForDefaultRegion() throws Exception {
        GcpCloudProvider provider = new GcpCloudProvider(getTestParameter());

        getSupportedAvailabilityZones(provider, GCP_CRED_NAME);
    }

    @Test(priority = 4, groups = "regions")
    public void testOSListAvailabilityZonesForDefaultRegion() throws Exception {
        OpenstackCloudProvider provider = new OpenstackCloudProvider(getTestParameter());

        getSupportedAvailabilityZones(provider, OS_CRED_NAME);
    }
}
