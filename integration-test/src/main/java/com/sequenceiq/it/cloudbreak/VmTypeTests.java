package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.VmType;
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
import java.util.Set;

public class VmTypeTests extends CloudbreakTest {

    private static final String AZURE_CRED_NAME = "autotesting-vmtypes-azure";

    private static final String AWS_CRED_NAME = "autotesting-vmtypes-aws";

    private static final String GCP_CRED_NAME = "autotesting-vmtypes-gcp";

    private static final String OS_CRED_NAME = "autotesting-vmtypes-os";

    private static final Logger LOGGER = LoggerFactory.getLogger(VmTypeTests.class);

    public VmTypeTests() {
    }

    public VmTypeTests(TestParameter tp) {
        setTestParameter(tp);
    }

    private void countVMTypesForCredentialInDefaultAvailabilityZone(CloudProvider provider, String credentialName) throws Exception {
        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(credentialName), provider.getPlatform() + " credential is created");
        given(VmType.request()
                .withPlatform(provider.getPlatform())
                .withRegion(provider.region())
                .withAvailabilityZone(provider.availabilityZone()), provider.getPlatform()
                + " vm type request");
        when(VmType.getPlatformVmTypes(), String.join(" ", "vm types are requested to",
                provider.getPlatform(), "credential and", provider.region(), "region."));
        then(VmType.assertThis(
                (vmtype, t) -> {
                    int azVirtualMachinesNumber = vmtype.getResponse().getVmTypes().get(provider.availabilityZone())
                            .getVirtualMachines()
                            .size();

                    LOGGER.info("Number of {} Virtual Machines in {} availibility zone ::: {}",
                            provider.getPlatform(),
                            provider.availabilityZone(),
                            azVirtualMachinesNumber);
                    Assert.assertTrue(azVirtualMachinesNumber > 0,
                            "Number of Virtual Machines should be present in response"
                                    + " for default availibility zone!");
                }), "Virtual Machines should be part of the response."
        );
    }

    private void listVMTypesForCredentialInDefaultAvailabilityZone(CloudProvider provider, String credentialName) throws Exception {
        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(credentialName), provider.getPlatform() + " credential is created");
        given(VmType.request()
                .withPlatform(provider.getPlatform())
                .withRegion(provider.region())
                .withAvailabilityZone(provider.availabilityZone()), provider.getPlatform()
                + " vm type request");
        when(VmType.getPlatformVmTypes(), String.join(" ", "vm types are requested to",
                provider.getPlatform(), "credential and", provider.region(), "region."));
        then(VmType.assertThis(
                (vmtype, t) -> {
                    Set<VmTypeV4Response> azVirtualMachines = vmtype.getResponse().getVmTypes()
                            .get(provider.availabilityZone())
                            .getVirtualMachines();

                    Assert.assertFalse(azVirtualMachines.isEmpty(),
                                "Virtual Machines should be present in response"
                                        + " for default availibility zone!");
                }), "Virtual Machines should be part of the response."
        );
    }

    private void getDefaultVMTypeForCredentialInDefaultAvailabilityZone(CloudProvider provider, String credentialName) throws Exception {
        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(credentialName), provider.getPlatform() + " credential is created");
        given(VmType.request()
                .withPlatform(provider.getPlatform())
                .withRegion(provider.region())
                .withAvailabilityZone(provider.availabilityZone()), provider.getPlatform()
                + " vm type request");
        when(VmType.getPlatformVmTypes(), String.join(" ", "vm types are requested to",
                provider.getPlatform(), "credential and", provider.region(), "region."));
        then(VmType.assertThis(
                (vmtype, t) -> {
                    String azDefVirtualMachine = vmtype.getResponse().getVmTypes()
                            .get(provider.availabilityZone())
                            .getDefaultVirtualMachine()
                            .getValue();

                    LOGGER.info("{} Default Virtual Machine in {} availibility zone is ::: {}",
                            provider.getPlatform(),
                            provider.availabilityZone(),
                            azDefVirtualMachine);
                    Assert.assertNotNull(azDefVirtualMachine,
                            "Default Virtual Machine should be present in response"
                                    + " for default availibility zone!");
                }), "Default Virtual Machine should be part of the response."
        );
    }

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {AZURE_CRED_NAME, AWS_CRED_NAME, GCP_CRED_NAME, OS_CRED_NAME};

        for (String aNameArray : nameArray) {
            LOGGER.info("Delete credential: {}", aNameArray.toLowerCase().trim());

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

    @Test(priority = 1, groups = "vmtypes")
    public void testCountAzureVMTypesForCredentialInDefaultRegion() throws Exception {
        AzureCloudProvider provider = new AzureCloudProvider(getTestParameter());

        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(AZURE_CRED_NAME), "Azure credential is created");
        given(VmType.request()
                .withPlatform(provider.getPlatform())
                .withRegion(provider.region())
                .withAvailabilityZone(provider.availabilityZone()), "Azure vm type request");
        when(VmType.getPlatformVmTypes(), "vm types are requested to Azure credential and "
                + provider.region() + " region.");
        then(VmType.assertThis(
                (vmtype, t) -> {
                    int regionVirtualMachinesCount = vmtype.getResponse().getVmTypes().get(provider.region())
                                .getVirtualMachines()
                                .size();

                        LOGGER.info("Number of Azure Virtual Machines in {} region ::: {}",
                                provider.region(),
                                regionVirtualMachinesCount);
                        Assert.assertTrue(regionVirtualMachinesCount > 0,
                                "Number of Azure Virtual Machines should be present in response"
                                        + " for default region!");
                }), "Azure Virtual Machines should be part of the response."
        );
    }

    @Test(priority = 2, groups = "vmtypes")
    public void testCountAWSVMTypesForCredentialInDefaultAvailabilityZone() throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        countVMTypesForCredentialInDefaultAvailabilityZone(provider, AWS_CRED_NAME);
    }

    @Test(priority = 3, groups = "vmtypes")
    public void testCountGCPVMTypesForCredentialInDefaultAvailabilityZone() throws Exception {
        GcpCloudProvider provider = new GcpCloudProvider(getTestParameter());

        countVMTypesForCredentialInDefaultAvailabilityZone(provider, GCP_CRED_NAME);
    }

    @Test(priority = 4, groups = "vmtypes")
    public void testCountOSVMTypesForCredentialInDefaultAvailabilityZone() throws Exception {
        OpenstackCloudProvider provider = new OpenstackCloudProvider(getTestParameter());

        countVMTypesForCredentialInDefaultAvailabilityZone(provider, OS_CRED_NAME);
    }

    @Test(priority = 5, groups = "vmtypes")
    public void testListAzureVMTypesForCredentialInDefaultRegion() throws Exception {
        AzureCloudProvider provider = new AzureCloudProvider(getTestParameter());

        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(AZURE_CRED_NAME), "Azure credential is created");
        given(VmType.request()
                .withPlatform(provider.getPlatform())
                .withRegion(provider.region())
                .withAvailabilityZone(provider.availabilityZone()), "Azure vm type request");
        when(VmType.getPlatformVmTypes(), "vm types are requested to Azure credential and "
                + provider.region() + " region.");
        then(VmType.assertThis(
                (vmtype, t) -> {
                    Set<VmTypeV4Response> regionVirtualMachines = vmtype.getResponse().getVmTypes()
                            .get(provider.region())
                            .getVirtualMachines();

                    Assert.assertFalse(regionVirtualMachines.isEmpty(),
                            "Azure Virtual Machines should be present in response for default region!");
                }), "Virtual Machines should be part of the response."
        );
    }

    @Test(priority = 6, groups = "vmtypes")
    public void testListAWSVMTypesForCredentialInDefaultAvailabilityZone() throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        listVMTypesForCredentialInDefaultAvailabilityZone(provider, AWS_CRED_NAME);
    }

    @Test(priority = 7, groups = "vmtypes")
    public void testListGCPVMTypesForCredentialInDefaultAvailabilityZone() throws Exception {
        GcpCloudProvider provider = new GcpCloudProvider(getTestParameter());

        listVMTypesForCredentialInDefaultAvailabilityZone(provider, GCP_CRED_NAME);
    }

    @Test(priority = 8, groups = "vmtypes")
    public void testListOSVMTypesForCredentialInDefaultAvailabilityZone() throws Exception {
        OpenstackCloudProvider provider = new OpenstackCloudProvider(getTestParameter());

        listVMTypesForCredentialInDefaultAvailabilityZone(provider, OS_CRED_NAME);
    }

    @Test(priority = 9, groups = "vmtypes")
    public void testAzureDefaultVMTypeForCredentialInDefaultRegion() throws Exception {
        AzureCloudProvider provider = new AzureCloudProvider(getTestParameter());

        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(AZURE_CRED_NAME), "Azure credential is created");
        given(VmType.request()
                .withPlatform(provider.getPlatform())
                .withRegion(provider.region())
                .withAvailabilityZone(provider.availabilityZone()), "Azure vm type request");
        when(VmType.getPlatformVmTypes(), "vm types are requested to Azure credential and "
                + provider.region() + " region.");
        then(VmType.assertThis(
                (vmtype, t) -> {
                    String regionDefVirtualMachine = vmtype.getResponse().getVmTypes().get(provider.region())
                            .getDefaultVirtualMachine()
                            .getValue();

                    LOGGER.info("Azure Default Virtual Machine in {} region is ::: {}", provider.region(),
                            regionDefVirtualMachine);
                    Assert.assertNotNull(regionDefVirtualMachine,
                            "Azure Default Virtual Machine should be present in response for default region!");
                }), "Default Virtual Machine should be part of the response."
        );
    }

    @Test(priority = 10, groups = "vmtypes")
    public void testOSDefaultVMTypeSupport() throws Exception {
        OpenstackCloudProvider provider = new OpenstackCloudProvider(getTestParameter());

        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(OS_CRED_NAME), "OpenStack credential is created");
        given(VmType.request()
                .withPlatform(provider.getPlatform())
                .withRegion(provider.region())
                .withAvailabilityZone(provider.availabilityZone()), "OpenStack vm type request");
        when(VmType.getPlatformVmTypes(), "vm types are requested to OpenStack credential and "
                + provider.region() + " region.");
        then(VmType.assertThis(
                (vmtype, t) -> {
                    LOGGER.info("OpenStack Default Virtual Machine is not supported for OpenStack.");
                    Assert.assertNull(vmtype.getResponse().getVmTypes().get(provider.availabilityZone())
                                    .getDefaultVirtualMachine(),
                            "OpenStack Default Virtual Machine should not be present in response!");
                }), "OpenStack Default Virtual Machine should not be part of the response."
        );
    }

    @Test(priority = 11, groups = "vmtypes")
    public void testAWSDefaultVMTypeForCredentialInDefaultAvailabilityZone() throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        getDefaultVMTypeForCredentialInDefaultAvailabilityZone(provider, AWS_CRED_NAME);
    }

    @Test(priority = 12, groups = "vmtypes")
    public void testGCPDefaultVMTypeForCredentialInDefaultAvailabilityZone() throws Exception {
        GcpCloudProvider provider = new GcpCloudProvider(getTestParameter());

        getDefaultVMTypeForCredentialInDefaultAvailabilityZone(provider, GCP_CRED_NAME);
    }
}