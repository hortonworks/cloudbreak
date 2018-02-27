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
import java.util.Collection;

public class RegionTests extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegionTests.class);

    private String errorMessage = "";

    private CloudProvider cloudProvider;

    public RegionTests() {
    }

    public RegionTests(CloudProvider cp, TestParameter tp) {
        this.cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters({ "provider" })
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("Provider: {} Region test setup has been started.", provider);
        if (this.cloudProvider != null) {
            LOGGER.info("{} provider already set - running from factory test", this.cloudProvider);
            return;
        }
        this.cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter())[0];
    }

    @AfterTest
    public void cleanUp() throws Exception {
        LOGGER.info("Delete credential with name: {}", cloudProvider.getCredentialName());

        try {
            given(CloudbreakClient.isCreated());
            given(Credential.request()
                    .withName(cloudProvider.getCredentialName()));
            when(Credential.delete());
        } catch (ForbiddenException | BadRequestException e) {
            String exceptionMessage = e.getResponse().readEntity(String.class);
            this.errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(":") + 1);
            LOGGER.info("Clean Up Exception message ::: {}", this.errorMessage);
        }
    }

    @Test(priority = 0, groups = { "regions" })
    public void testDefaultRegionForCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform()
                .concat(" credential is created"));
        given(Region.request(), cloudProvider.getPlatform()
                .concat(" region request"));
        when(Region.getPlatformRegions(), "Default region is requested to "
                .concat(cloudProvider.getPlatform())
                .concat(" credential"));
        then(Region.assertThis(
                (region, t) -> {
                    LOGGER.info("{} Default Region is ::: {}",
                            cloudProvider.getPlatform(),
                            region.getRegionResponse().getDefaultRegion());
                    Assert.assertTrue(region.getRegionResponse().getDefaultRegion().contains(cloudProvider.region()),
                            "[" + cloudProvider.region() + "] region is not the default one!");
                }), "[" + cloudProvider.region() + "] region should be the default one."
        );
    }

    @Test(priority = 1, groups = { "regions" })
    public void testListRegionsForCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform()
                .concat(" credential is created"));
        given(Region.request(), cloudProvider.getPlatform()
                .concat(" region request"));
        when(Region.getPlatformRegions(), "Regions are requested to "
                .concat(cloudProvider.getPlatform())
                .concat(" credential"));
        then(Region.assertThis(
                (region, t) -> {
                    LOGGER.info("{} number of Regions ::: {}",
                            cloudProvider.getPlatform(),
                            Integer.toString(region.getRegionResponse().getRegions().size()));
                    Assert.assertNotNull(region.getRegionResponse().getRegions().size());
                }), "Regions should be part of the response."
        );
    }

    @Test(priority = 2, groups = { "regions" })
    public void testListDisplayNamesForCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform()
                .concat(" credential is created"));
        given(Region.request(), cloudProvider.getPlatform()
                .concat(" region request"));
        when(Region.getPlatformRegions(), "Regions are requested to "
                .concat(cloudProvider.getPlatform())
                .concat(" credential"));
        then(Region.assertThis(
                (region, t) -> {
                    LOGGER.info("{} number of Display Names ::: {}",
                            cloudProvider.getPlatform(),
                            Integer.toString(region.getRegionResponse().getDisplayNames().size()));
                    Assert.assertNotNull(region.getRegionResponse().getDisplayNames().size());
                }), "Display Names should be part of the response."
        );
    }

    @Test(priority = 3, groups = { "regions" })
    public void testListAvailabilityZonesForCredential() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform()
                .concat(" credential is created"));
        given(Region.request(), cloudProvider.getPlatform()
                .concat(" region request"));
        when(Region.getPlatformRegions(), "Regions are requested to "
                .concat(cloudProvider.getPlatform())
                .concat(" credential"));
        then(Region.assertThis(
                (region, t) -> {
                    LOGGER.info("{} number of Availibility Zones ::: {}",
                            cloudProvider.getPlatform(),
                            Integer.toString(region.getRegionResponse().getAvailabilityZones().size()));
                    Assert.assertNotNull(region.getRegionResponse().getAvailabilityZones().size());
                }), "Availibility Zones should be part of the response."
        );
    }

    @Test(priority = 4, groups = { "regions" })
    public void testListAvailabilityZonesForDefaultRegion() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform()
                .concat(" credential is created"));
        given(Region.request(), cloudProvider.getPlatform()
                .concat(" region request"));
        when(Region.getPlatformRegions(), "Regions are requested to "
                .concat(cloudProvider.getPlatform())
                .concat(" credential"));
        then(Region.assertThis(
                (region, t) -> {
                    Collection<String> availibilityZones = region.getRegionResponse().getAvailabilityZones()
                            .get(cloudProvider.region());

                    if (cloudProvider.getPlatform().equalsIgnoreCase("AZURE")) {
                        LOGGER.info("{} Default Region's Availibility Zone is not supported.",
                                cloudProvider.getPlatform());
                        Assert.assertTrue(availibilityZones.isEmpty());
                    } else {
                        availibilityZones.forEach(zone ->
                            LOGGER.info("{} Default Region's Availibility Zone is ::: {}",
                                    cloudProvider.getPlatform(), zone));
                        Assert.assertFalse(availibilityZones.isEmpty());
                    }
                }), "Default Region Availibility Zones should be part of the response."
        );
    }
}
