package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.DiskTypes;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class DiskTypeTests extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskTypeTests.class);

    public DiskTypeTests() {
    }

    public DiskTypeTests(TestParameter tp) {
        setTestParameter(tp);
    }

    @Test(priority = 1, groups = "disktypes")
    public void testListDiskTypes() throws Exception {
        given(CloudbreakClient.isCreated());
        given(DiskTypes.request(), " disk type request");
        when(DiskTypes.get(), " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Map<String, Collection<String>> diskTypesProviders = disktype.getResponse().getDiskTypes();

                    for (Entry<String, Collection<String>> diskTypesProvider : diskTypesProviders.entrySet()) {
                        LOGGER.info("Provider is ::: {}", diskTypesProvider.getKey());
                        diskTypesProvider.getValue().forEach((diskType) -> {
                            LOGGER.info("Disk Type is ::: {}", diskType);
                            Assert.assertFalse(diskType.isEmpty(), "Disk Types should be present in response!");
                        });
                    }
                }), "Disk Types should be part of the response."
        );
    }

    @Test(priority = 2, groups = "disktypes")
    public void testListDefaultDisks() throws Exception {
        given(CloudbreakClient.isCreated());
        given(DiskTypes.request(), " disk types request");
        when(DiskTypes.get(), " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Map<String, String> defaultDiskForProviders = disktype.getResponse().getDefaultDisks();

                    for (Entry<String, String> defaultDiskForProvider : defaultDiskForProviders.entrySet()) {
                        LOGGER.info("The {} provider default disk is ::: {}", defaultDiskForProvider.getKey(), defaultDiskForProvider.getValue());
                        Assert.assertNotNull(defaultDiskForProvider.getValue(), "Default disk should be present in response!");
                    }
                }), "Default disk should be part of the response."
        );
    }

    @Test(priority = 3, groups = "disktypes")
    public void testListDisplayNames() throws Exception {
        given(CloudbreakClient.isCreated());
        given(DiskTypes.request(), " disk types request");
        when(DiskTypes.get(), " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Map<String, Map<String, String>> displayNamesProviders = disktype.getResponse().getDisplayNames();

                    for (Entry<String, Map<String, String>> displayNamesProvider : displayNamesProviders.entrySet()) {
                        LOGGER.info("Provider is ::: {}", displayNamesProvider.getKey());
                        displayNamesProvider.getValue().forEach((diskType, displayName) -> {
                            LOGGER.info("Display Name is ::: {}", displayName);
                            Assert.assertFalse(displayName.isEmpty(), "Display Name should be present in response!");
                        });
                    }
                }), "Display Name should be part of the response."
        );
    }

    @Test(priority = 4, groups = "disktypes")
    public void testListDiskMappings() throws Exception {
        given(CloudbreakClient.isCreated());
        given(DiskTypes.request(), " disk types request");
        when(DiskTypes.get(), " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Map<String, Map<String, String>> diskMappingsProviders = disktype.getResponse().getDiskMappings();

                    for (Entry<String, Map<String, String>> diskMappingsProvider : diskMappingsProviders.entrySet()) {
                        LOGGER.info("Provider is ::: {}", diskMappingsProvider.getKey());
                        diskMappingsProvider.getValue().forEach((diskType, mapping) -> {
                            LOGGER.info("Disk Mapping is ::: {}", mapping);
                            Assert.assertFalse(mapping.isEmpty(), "Disk Mapping should be present in response!");
                        });
                    }
                }), "Disk Mapping should be part of the response."
        );
    }

    @Test(priority = 5, groups = "disktypes")
    public void testListAWSDiskTypes() throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        given(CloudbreakClient.isCreated());
        given(DiskTypes.request().withType(provider.getPlatform()), provider.getPlatform() + " disk types request");
        when(DiskTypes.getByType(), provider.getPlatform() + " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Collection<String> diskTypes = disktype.getByFilterResponses();

                    diskTypes.forEach((diskType) -> {
                        LOGGER.info(provider.getPlatform() + " Disk Type is ::: {}", diskType);
                        Assert.assertFalse(diskType.isEmpty(), "Disk Type should be present in response!");
                    });
                }), provider.getPlatform() + " Disk Type should be part of the response."
        );
    }

    @Test(priority = 6, groups = "disktypes")
    public void testListAzureDiskTypes() throws Exception {
        AzureCloudProvider provider = new AzureCloudProvider(getTestParameter());

        given(CloudbreakClient.isCreated());
        given(DiskTypes.request().withType(provider.getPlatform()), provider.getPlatform() + " disk types request");
        when(DiskTypes.getByType(), provider.getPlatform() + " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Collection<String> diskTypes = disktype.getByFilterResponses();

                    diskTypes.forEach((diskType) -> {
                        LOGGER.info(provider.getPlatform() + " Disk Type is ::: {}", diskType);
                        Assert.assertFalse(diskType.isEmpty(), "Disk Type should be present in response!");
                    });
                }), provider.getPlatform() + " Disk Type should be part of the response."
        );
    }

    @Test(priority = 7, groups = "disktypes")
    public void testListGCPDiskTypes() throws Exception {
        GcpCloudProvider provider = new GcpCloudProvider(getTestParameter());

        given(CloudbreakClient.isCreated());
        given(DiskTypes.request().withType(provider.getPlatform()), provider.getPlatform() + " disk types request");
        when(DiskTypes.getByType(), provider.getPlatform() + " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Collection<String> diskTypes = disktype.getByFilterResponses();

                    diskTypes.forEach((diskType) -> {
                        LOGGER.info(provider.getPlatform() + " Disk Type is ::: {}", diskType);
                        Assert.assertFalse(diskType.isEmpty(), "Disk Type should be present in response!");
                    });
                }), provider.getPlatform() + " Disk Type should be part of the response."
        );
    }

    @Test(priority = 8, groups = "disktypes")
    public void testListOSDiskTypes() throws Exception {
        OpenstackCloudProvider provider = new OpenstackCloudProvider(getTestParameter());

        given(CloudbreakClient.isCreated());
        given(DiskTypes.request().withType(provider.getPlatform()), provider.getPlatform() + " disk types request");
        when(DiskTypes.getByType(), provider.getPlatform() + " disk types are requested.");
        then(DiskTypes.assertThis(
                (disktype, t) -> {
                    Collection<String> diskTypes = disktype.getByFilterResponses();

                    diskTypes.forEach((diskType) -> {
                        LOGGER.info(provider.getPlatform() + " Disk Type is ::: {}", diskType);
                        Assert.assertFalse(diskType.isEmpty(), "Disk Type should be present in response!");
                    });
                }), provider.getPlatform() + " Disk Type should be part of the response."
        );
    }
}