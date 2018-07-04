package com.sequenceiq.it.cloudbreak;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.DiskTypes;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class DiskTypeTests extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskTypeTests.class);

    public DiskTypeTests() {
    }

    public DiskTypeTests(TestParameter tp) {
        setTestParameter(tp);
    }

    private void listDiskMappingsForProvider(CloudProvider provider) throws Exception {
        given(CloudbreakClient.isCreated());
        given(DiskTypes.request(), " disk types request");
        when(DiskTypes.get(), " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Map<String, Map<String, String>> diskMappingsProviders = disktype.getResponse().getDiskMappings();

                    for (Entry<String, Map<String, String>> diskMappingsProvider : diskMappingsProviders.entrySet()) {
                        LOGGER.debug("Provider is ::: {}", diskMappingsProvider.getKey());
                        diskMappingsProvider.getValue().forEach((diskType, mapping) -> LOGGER.debug("Disk Mapping is ::: {}", mapping));
                        if (Objects.equals(provider.getPlatform(), diskMappingsProvider.getKey())) {
                            Assert.assertFalse(diskMappingsProvider.getValue().isEmpty(), "Disk Mapping should be present in response!");
                        }
                    }
                }), "Disk Mapping should be part of the response."
        );
    }

    private void listDisplayNamesForProvider(CloudProvider provider) throws Exception {
        given(CloudbreakClient.isCreated());
        given(DiskTypes.request(), " disk types request");
        when(DiskTypes.get(), " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Map<String, Map<String, String>> displayNamesProviders = disktype.getResponse().getDisplayNames();

                    for (Entry<String, Map<String, String>> displayNamesProvider : displayNamesProviders.entrySet()) {
                        LOGGER.debug("Provider is ::: {}", displayNamesProvider.getKey());
                        displayNamesProvider.getValue().forEach((diskType, displayName) -> LOGGER.debug("Display Name is ::: {}", displayName));
                        if (Objects.equals(provider.getPlatform(), displayNamesProvider.getKey())) {
                            Assert.assertFalse(displayNamesProvider.getValue().isEmpty(), "Display Name should be present in response!");
                        }
                    }
                }), "Display Name should be part of the response."
        );
    }

    private void listDefaultDisksForProvdier(CloudProvider provider) throws Exception {
        given(CloudbreakClient.isCreated());
        given(DiskTypes.request(), " disk types request");
        when(DiskTypes.get(), " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Map<String, String> defaultDiskForProviders = disktype.getResponse().getDefaultDisks();

                    for (Entry<String, String> defaultDiskForProvider : defaultDiskForProviders.entrySet()) {
                        LOGGER.debug("The {} provider default disk is ::: {}", defaultDiskForProvider.getKey(), defaultDiskForProvider.getValue());
                        if (Objects.equals(provider.getPlatform(), defaultDiskForProvider.getKey())) {
                            Assert.assertFalse(defaultDiskForProvider.getValue().isEmpty(), "Default disk should be present in response!");
                        }
                    }
                }), "Default disk should be part of the response."
        );
    }

    @Test(priority = 1, groups = "disktypes")
    public void testListAWSDefaultDisks() throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        listDefaultDisksForProvdier(provider);
    }

    @Test(priority = 3, groups = "disktypes")
    public void testListAzureDefaultDisks() throws Exception {
        AzureCloudProvider provider = new AzureCloudProvider(getTestParameter());

        listDefaultDisksForProvdier(provider);
    }

    @Test(priority = 4, groups = "disktypes")
    public void testListGCPDefaultDisks() throws Exception {
        GcpCloudProvider provider = new GcpCloudProvider(getTestParameter());

        listDefaultDisksForProvdier(provider);
    }

    @Test(priority = 5, groups = "disktypes")
    public void testListOSDefaultDisks() throws Exception {
        OpenstackCloudProvider provider = new OpenstackCloudProvider(getTestParameter());

        listDefaultDisksForProvdier(provider);
    }

    @Test(priority = 6, groups = "disktypes")
    public void testListAWSDisplayNames() throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        listDisplayNamesForProvider(provider);
    }

    @Test(priority = 7, groups = "disktypes")
    public void testListAzureDisplayNames() throws Exception {
        AzureCloudProvider provider = new AzureCloudProvider(getTestParameter());

        listDisplayNamesForProvider(provider);
    }

    @Test(priority = 8, groups = "disktypes")
    public void testListGCPDisplayNames() throws Exception {
        GcpCloudProvider provider = new GcpCloudProvider(getTestParameter());

        listDisplayNamesForProvider(provider);
    }

    @Test(priority = 9, groups = "disktypes")
    public void testListOSDisplayNames() throws Exception {
        OpenstackCloudProvider provider = new OpenstackCloudProvider(getTestParameter());

        listDisplayNamesForProvider(provider);
    }

    @Test(priority = 10, groups = "disktypes")
    public void testListAWSDiskMappings() throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        listDiskMappingsForProvider(provider);
    }

    @Test(priority = 11, groups = "disktypes")
    public void testListAzureDiskMappings() throws Exception {
        AzureCloudProvider provider = new AzureCloudProvider(getTestParameter());

        listDiskMappingsForProvider(provider);
    }

    @Test(priority = 12, groups = "disktypes")
    public void testListGCPDiskMappings() throws Exception {
        GcpCloudProvider provider = new GcpCloudProvider(getTestParameter());

        listDiskMappingsForProvider(provider);
    }

    @Test(priority = 13, groups = "disktypes")
    public void testListOSDiskMappings() throws Exception {
        OpenstackCloudProvider provider = new OpenstackCloudProvider(getTestParameter());

        listDiskMappingsForProvider(provider);
    }

    @Test(priority = 14, groups = "disktypes")
    public void testListAWSDiskTypes() throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        given(CloudbreakClient.isCreated());
        given(DiskTypes.request().withType(provider.getPlatform()), provider.getPlatform() + " disk types request");
        when(DiskTypes.getByType(), provider.getPlatform() + " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Collection<String> diskTypes = disktype.getByFilterResponses();

                    diskTypes.forEach(diskType -> LOGGER.debug(provider.getPlatform() + " Disk Type is ::: {}", diskType));
                    Assert.assertFalse(diskTypes.isEmpty(), "Disk Type should be present in response!");
                }), provider.getPlatform() + " Disk Type should be part of the response."
        );
    }

    @Test(priority = 15, groups = "disktypes")
    public void testListAzureDiskTypes() throws Exception {
        AzureCloudProvider provider = new AzureCloudProvider(getTestParameter());

        given(CloudbreakClient.isCreated());
        given(DiskTypes.request().withType(provider.getPlatform()), provider.getPlatform() + " disk types request");
        when(DiskTypes.getByType(), provider.getPlatform() + " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Collection<String> diskTypes = disktype.getByFilterResponses();

                    diskTypes.forEach(diskType -> LOGGER.debug(provider.getPlatform() + " Disk Type is ::: {}", diskType));
                    Assert.assertFalse(diskTypes.isEmpty(), "Disk Type should be present in response!");
                }), provider.getPlatform() + " Disk Type should be part of the response."
        );
    }

    @Test(priority = 16, groups = "disktypes")
    public void testListGCPDiskTypes() throws Exception {
        GcpCloudProvider provider = new GcpCloudProvider(getTestParameter());

        given(CloudbreakClient.isCreated());
        given(DiskTypes.request().withType(provider.getPlatform()), provider.getPlatform() + " disk types request");
        when(DiskTypes.getByType(), provider.getPlatform() + " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Collection<String> diskTypes = disktype.getByFilterResponses();

                    diskTypes.forEach(diskType -> LOGGER.debug(provider.getPlatform() + " Disk Type is ::: {}", diskType));
                    Assert.assertFalse(diskTypes.isEmpty(), "Disk Type should be present in response!");
                }), provider.getPlatform() + " Disk Type should be part of the response."
        );
    }

    @Test(priority = 17, groups = "disktypes")
    public void testListOSDiskTypes() throws Exception {
        OpenstackCloudProvider provider = new OpenstackCloudProvider(getTestParameter());

        given(CloudbreakClient.isCreated());
        given(DiskTypes.request().withType(provider.getPlatform()), provider.getPlatform() + " disk types request");
        when(DiskTypes.getByType(), provider.getPlatform() + " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Collection<String> diskTypes = disktype.getByFilterResponses();

                    diskTypes.forEach(diskType -> LOGGER.debug(provider.getPlatform() + " Disk Type is ::: {}", diskType));
                    Assert.assertFalse(diskTypes.isEmpty(), "Disk Type should be present in response!");
                }), provider.getPlatform() + " Disk Type should be part of the response."
        );
    }

    @Test(priority = 18, groups = "disktypes")
    public void testListYARNDiskTypes() throws Exception {
        given(CloudbreakClient.isCreated());
        given(DiskTypes.request().withType("YARN"), "YARN disk types request");
        when(DiskTypes.getByType(), "YARN disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Collection<String> diskTypes = disktype.getByFilterResponses();

                    diskTypes.forEach(diskType -> LOGGER.debug("YARN Disk Type is ::: {}", diskType));
                    Assert.assertTrue(diskTypes.isEmpty(), "Disk Type should be present in response!");
                }), "YARN Disk Type should be part of the response."
        );
    }
}