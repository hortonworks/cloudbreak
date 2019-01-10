package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Region;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

public class RegionTests extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegionTests.class);

    private CloudProvider cloudProvider;

    public RegionTests() {
    }

    public RegionTests(CloudProvider cp, TestParameter tp) {
        cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("Provider: {} Region test setup has been started.", provider);
        if (cloudProvider == null) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        } else {
            LOGGER.info("cloud provider already set - running from factory test");
        }
    }

    @AfterTest
    public void cleanUp() throws Exception {
        LOGGER.info("Delete credential with name: {}", cloudProvider.getCredentialName());

        try {
            given(CloudbreakClient.created());
            given(Credential.request()
                    .withName(cloudProvider.getCredentialName()));
            when(Credential.delete());
        } catch (ForbiddenException | BadRequestException e) {
            String exceptionMessage = e.getResponse().readEntity(String.class);
            String errorMessage;

            errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
            LOGGER.info("Clean Up Exception message ::: {}", errorMessage);
        }
    }

    @Test(priority = 1, groups = "regions")
    public void testDefaultRegionForCredential() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform() + " credential is created");
        given(Region.request(), cloudProvider.getPlatform() + " region request");
        when(Region.getPlatformRegionsWithRetry(Integer.parseInt(getTestParameter().get("retryQuantity"))), "Default region is requested to "
                + cloudProvider.getPlatform() + " credential");
        then(Region.assertThis(
                (region, t) -> {
                    LOGGER.info("{} Default Region is ::: {}",
                            cloudProvider.getPlatform(),
                            region.getRegionV4Response().getDefaultRegion());
                    Assert.assertTrue(region.getRegionV4Response().getDefaultRegion().contains(cloudProvider.region()),
                            '[' + cloudProvider.region() + "] region is not the default one!");
                }), '[' + cloudProvider.region() + "] region should be the default one."
        );
    }

    @Test(priority = 2, groups = "regions")
    public void testListRegionsForCredential() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform() + " credential is created");
        given(Region.request(), cloudProvider.getPlatform() + " region request");
        when(Region.getPlatformRegionsWithRetry(Integer.parseInt(getTestParameter().get("retryQuantity"))), "Regions are requested to "
                + cloudProvider.getPlatform() + " credential");
        then(Region.assertThis(
                (region, t) -> {
                    int regionsCount = region.getRegionV4Response().getRegions().size();

                    LOGGER.info("{} number of Regions ::: {}",
                            cloudProvider.getPlatform(),
                            regionsCount);
                    Assert.assertTrue(regionsCount > 0);
                }), "Regions should be part of the response."
        );
    }

    @Test(priority = 3, groups = "regions")
    public void testListDisplayNamesForCredential() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform() + " credential is created");
        given(Region.request(), cloudProvider.getPlatform() + " region request");
        when(Region.getPlatformRegionsWithRetry(Integer.parseInt(getTestParameter().get("retryQuantity"))), "Regions are requested to "
                + cloudProvider.getPlatform() + " credential");
        then(Region.assertThis(
                (region, t) -> {
                    int displayNamesCount = region.getRegionV4Response().getDisplayNames().size();

                    LOGGER.info("{} number of Display Names ::: {}",
                            cloudProvider.getPlatform(),
                            displayNamesCount);
                    Assert.assertTrue(displayNamesCount > 0);
                }), "Display Names should be part of the response."
        );
    }

    @Test(priority = 4, groups = "regions")
    public void testListAvailabilityZonesForCredential() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform() + " credential is created");
        given(Region.request(), cloudProvider.getPlatform() + " region request");
        when(Region.getPlatformRegionsWithRetry(Integer.parseInt(getTestParameter().get("retryQuantity"))), "Regions are requested to "
                + cloudProvider.getPlatform() + " credential");
        then(Region.assertThis(
                (region, t) -> {
                    int availibilityZonesCount = region.getRegionV4Response().getAvailabilityZones().size();

                    LOGGER.info("{} number of Availibility Zones ::: {}",
                            cloudProvider.getPlatform(),
                            availibilityZonesCount);
                    Assert.assertTrue(availibilityZonesCount > 0);
                }), "Availibility Zones should be part of the response."
        );
    }
}
