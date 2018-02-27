package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.VmType;
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
import java.util.HashSet;
import java.util.Set;

public class VmTypeTests extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmTypeTests.class);

    private String errorMessage = "";

    private CloudProvider cloudProvider;

    private String regionDefVirtualMachine = "";

    private String azDefVirtualMachine = "";

    private int regionVirtualMachinesNumber;

    private int azVirtualMachinesNumber;

    private Set<VmTypeJson> regionVirtualMachines = new HashSet<VmTypeJson>();

    private Set<VmTypeJson> azVirtualMachines = new HashSet<VmTypeJson>();

    public VmTypeTests() {
    }

    public VmTypeTests(CloudProvider cp, TestParameter tp) {
        this.cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters({"provider"})
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("Provider: {} VM Type test setup has been started.", provider);
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

    @Test(priority = 0, groups = {"vmtypes"})
    public void testCountVMTypesForCredentialInDefaultRegion() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform()
                .concat(" credential is created"));
        given(VmType.request()
                .withPlatform(cloudProvider.getPlatform())
                .withRegion(cloudProvider.region())
                .withAvailibilityZone(cloudProvider.availibilityZone()), cloudProvider.getPlatform()
                .concat(" vm type request"));
        when(VmType.getPlatformVmTypes(), " vm types are requested to "
                .concat(cloudProvider.getPlatform())
                .concat(" credential and ")
                .concat(cloudProvider.region())
                .concat(" region."));
        then(VmType.assertThis(
                (vmtype, t) -> {
                    if (cloudProvider.getPlatform().matches("AZURE")) {
                        regionVirtualMachinesNumber = vmtype.getResponse().getVmTypes().get(cloudProvider.region())
                                .getVirtualMachines()
                                .size();

                        LOGGER.info("Number of {} Virtual Machines in {} region ::: {}",
                                cloudProvider.getPlatform(),
                                cloudProvider.region(),
                                regionVirtualMachinesNumber);
                        Assert.assertTrue(regionVirtualMachinesNumber > 1,
                                "Number of Virtual Machines should be present in response for default region!");
                    } else {
                        azVirtualMachinesNumber = vmtype.getResponse().getVmTypes().get(cloudProvider.availibilityZone())
                                .getVirtualMachines()
                                .size();

                        LOGGER.info("Number of {} Virtual Machines in {} availibility zone ::: {}",
                                cloudProvider.getPlatform(),
                                cloudProvider.availibilityZone(),
                                azVirtualMachinesNumber);
                        Assert.assertTrue(azVirtualMachinesNumber > 1,
                                "Number of Virtual Machines should be present in response for default availibility zone!");
                    }
                }), "Virtual Machines should be present in response."
        );
    }

    @Test(priority = 1, groups = {"vmtypes"})
    public void testListVMTypesForCredentialInDefaultRegion() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform()
                .concat(" credential is created"));
        given(VmType.request()
                .withPlatform(cloudProvider.getPlatform())
                .withRegion(cloudProvider.region())
                .withAvailibilityZone(cloudProvider.availibilityZone()), cloudProvider.getPlatform()
                .concat(" vm type request"));
        when(VmType.getPlatformVmTypes(), " vm types are requested to "
                .concat(cloudProvider.getPlatform())
                .concat(" credential and ")
                .concat(cloudProvider.region())
                .concat(" region."));
        then(VmType.assertThis(
                (vmtype, t) -> {
                    if (cloudProvider.getPlatform().matches("AZURE")) {
                        regionVirtualMachines = vmtype.getResponse().getVmTypes()
                                .get(cloudProvider.region())
                                .getVirtualMachines();

                        Assert.assertFalse(regionVirtualMachines.isEmpty(),
                                "Virtual Machines should be present in response for default region!");
                    } else {
                        azVirtualMachines = vmtype.getResponse().getVmTypes()
                                .get(cloudProvider.availibilityZone())
                                .getVirtualMachines();

                        Assert.assertFalse(azVirtualMachines.isEmpty(),
                                "Virtual Machines should be present in response for default availibility zone!");
                    }
                }), "Virtual Machines should be present in response."
        );
    }

    @Test(priority = 2, groups = {"vmtypes"})
    public void testDefaultVMTypeForCredentialInDefaultRegion() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform()
                .concat(" credential is created"));
        given(VmType.request()
                .withPlatform(cloudProvider.getPlatform())
                .withRegion(cloudProvider.region())
                .withAvailibilityZone(cloudProvider.availibilityZone()), cloudProvider.getPlatform()
                .concat(" vm type request"));
        when(VmType.getPlatformVmTypes(), " vm types are requested to "
                .concat(cloudProvider.getPlatform())
                .concat(" credential and ")
                .concat(cloudProvider.region())
                .concat(" region."));
        then(VmType.assertThis(
                (vmtype, t) -> {
                    switch (cloudProvider.getPlatform()) {
                        case "AZURE":
                            regionDefVirtualMachine = vmtype.getResponse().getVmTypes()
                                    .get(cloudProvider.region())
                                    .getDefaultVirtualMachine()
                                    .getValue();

                            LOGGER.info("{} Default Virtual Machine in {} region is ::: {}",
                                    cloudProvider.getPlatform(),
                                    cloudProvider.region(),
                                    regionDefVirtualMachine);
                            Assert.assertNotNull(regionDefVirtualMachine,
                                    "Default Virtual Machine should be present in response!");
                            break;
                        case "OPENSTACK":
                            LOGGER.info("{} Default Virtual Machine is not supported for OpenStack.",
                                    cloudProvider.getPlatform());
                            Assert.assertNull(vmtype.getResponse().getVmTypes()
                                    .get(cloudProvider.availibilityZone())
                                    .getDefaultVirtualMachine(),
                                    "Default Virtual Machine should not be present in response!");
                            break;
                        default:
                            azDefVirtualMachine = vmtype.getResponse().getVmTypes()
                                    .get(cloudProvider.availibilityZone())
                                    .getDefaultVirtualMachine()
                                    .getValue();

                            LOGGER.info("{} Default Virtual Machine in {} availibility zone is ::: {}",
                                    cloudProvider.getPlatform(),
                                    cloudProvider.availibilityZone(),
                                    azDefVirtualMachine);
                            Assert.assertNotNull(azDefVirtualMachine,
                                    "Default Virtual Machine should be present in response!");
                            break;
                    }
                }), "Default Virtual Machine should be present in response."
        );
    }
}