package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure.ParameterCheck.HAS_THESE_PARAMETERS;
import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;

public class DistroXUpgradeTests extends AbstractMockTest {

    private static final String SALT_3006_10_IMAGE_ID = "c1d2e3f4-3006-4a10-9b10-5a17a0000010";

    private static final Map<String, String> SALT_MASTER_MINIMUM_AUTH_VERSION_FILE = Map.of(
            "file", "minimum_auth_version.conf",
            "path", "/etc/salt/master.d");

    private static final String SALT_STOP_ACTION = "stop";

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "there is a running Cloudbreak, and an environment with Data Lake and DistroX cluster in available state",
            when = "upgrade called on the DistroX cluster",
            then = "DistroX upgrade should be successful, the cluster should be up and running")
    public void testDistroXUpgrade(MockedTestContext testContext) {
        String imageSettings = "imageSettingsUpgrade";
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String distroXName = resourcePropertyProvider().getName();
        String targetRuntimeVersion = getNextRuntimeVersion(commonClusterManagerProperties.getUpgrade().getCurrentRuntimeVersion(false));
        testContext
                .given(imageSettings, DistroXImageTestDto.class)
                .withImageId("73f5db23-293d-4934-b124-1d5361375480")
                .withImageCatalog(upgradeImageCatalogName)
                .given(distroXName, DistroXTestDto.class)
                .withImageSettings(imageSettings)
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion)
                .given(distroXName, DistroXTestDto.class)
                .then((tc, entity, client) ->  {
                    DistroXUpgradeV1Request request = new DistroXUpgradeV1Request();
                    request.setRuntime(targetRuntimeVersion);
                    request.setDryRun(true);
                    DistroXUpgradeV1Response distroXUpgradeV1Response = client.getDefaultClient(tc).distroXUpgradeV1Endpoint()
                            .upgradeClusterByName(entity.getName(), request);
                    ImageComponentVersions componentVersions = distroXUpgradeV1Response.upgradeCandidates().getFirst().getComponentVersions();
                    entity.mockCm().setCmVersion(componentVersions.getCm() + "-" + componentVersions.getCmGBN());
                    return entity;
                })
                .when(distroXTestClient.upgrade(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "there is a running Cloudbreak, and an environment with Data Lake and DistroX cluster in available state",
            when = "upgrade by upgrade sets called on the DistroX cluster",
            then = "DistroX upgrade should be successful, the cluster should be up and running")
    public void testDistroXOsUpgradeByUpgradeSets(MockedTestContext testContext) {
        String imageSettings = "imageSettingsUpgrade";
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String distroXName = resourcePropertyProvider().getName();
        testContext
                .given(imageSettings, DistroXImageTestDto.class)
                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given(distroXName, DistroXTestDto.class)
                .withImageSettings(imageSettings)
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .given(DistroXUpgradeTestDto.class)
                .given(distroXName, DistroXTestDto.class)
                .then((tc, entity, client) ->  {
                    List<OrderedOSUpgradeSet> osUpgradeByUpgradeSets = new ArrayList<>();
                    Map<String, List<InstanceMetaDataV4Response>> upgradeSetMap = entity.getResponse().getInstanceGroups().stream().
                            flatMap(hardwareInfoGroupV4Response -> hardwareInfoGroupV4Response.getMetadata().stream())
                            .collect(Collectors.groupingBy(InstanceMetaDataV4Response::getInstanceGroup));
                    int i = 0;
                    for (Map.Entry<String, List<InstanceMetaDataV4Response>> upgradeSetEntry : upgradeSetMap.entrySet()) {
                        osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(i,
                                upgradeSetEntry.getValue().stream().map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toSet())));
                        i++;
                    }
                    entity.setOsUpgradeByUpgradeSets(osUpgradeByUpgradeSets);
                    return entity;
                })
                .when(distroXTestClient.osUpgradeByUpgradeSets())
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "there is a running Cloudbreak, and an environment with Data Lake and a DistroX cluster on an image with Salt 3006.10",
            when = "OS upgrade by upgrade sets, gateway set first, called on the DistroX cluster to an image with Salt 3006.25",
            then = "the salt masters get minimum_auth_version and are restarted only when its value changes: "
                    + "when the first gateway is replaced and when the last old minion is gone")
    public void testDistroXOsUpgradeByUpgradeSetsFromSaltWithoutMinimumAuthVersion(MockedTestContext testContext) {
        String imageSettings = "imageSettingsSaltAuthUpgrade";
        String imageCatalogName = resourcePropertyProvider().getName();
        createImageCatalog(testContext, imageCatalogName, getImageCatalogMockServerSetup().getSaltAuthUpgradeImageCatalogUrl());
        String distroXName = resourcePropertyProvider().getName();
        testContext
                .given(imageSettings, DistroXImageTestDto.class)
                .withImageId(SALT_3006_10_IMAGE_ID)
                .withImageCatalog(imageCatalogName)
                .given(distroXName, DistroXTestDto.class)
                .withImageSettings(imageSettings)
                .when(distroXTestClient.create(), key(distroXName))
                .enableVerification()
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .mockSalt().saltActionDistribute().post().atLeast(1).verify()
                .mockSalt().saltFileDistribute().post().parameters(SALT_MASTER_MINIMUM_AUTH_VERSION_FILE, HAS_THESE_PARAMETERS).times(0).verify()
                .mockSalt().saltActionDistribute().post().bodyCheck(DistroXUpgradeTests::isSaltMasterStop, 0).verify()
                .resetCalls()
                .given(DistroXUpgradeTestDto.class)
                .given(distroXName, DistroXTestDto.class)
                .then((tc, entity, client) -> {
                    entity.setOsUpgradeByUpgradeSets(gatewayFirstUpgradeSets(entity.getResponse()));
                    return entity;
                })
                .when(distroXTestClient.osUpgradeByUpgradeSets())
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .mockSalt().saltFileDistribute().post().parameters(SALT_MASTER_MINIMUM_AUTH_VERSION_FILE, HAS_THESE_PARAMETERS).atLeast(2).verify()
                .mockSalt().saltActionDistribute().post().bodyCheck(DistroXUpgradeTests::isSaltMasterStop, 2).verify()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "there is a running Cloudbreak, and an environment with Data Lake and DistroX cluster in available state, scale up",
            when = "upgrade by upgrade sets called on the DistroX cluster",
            then = "DistroX upgrade should be successful, the cluster should be up and running")
    public void testDistroXBatchOsUpgrade(MockedTestContext testContext) {
        String imageSettings = "imageSettingsUpgrade";
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String distroXName = resourcePropertyProvider().getName();
        testContext
                .given(imageSettings, DistroXImageTestDto.class)
                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given(distroXName, DistroXTestDto.class)
                .withImageSettings(imageSettings)
                .when(distroXTestClient.create(), key(distroXName))
                .enableVerification()
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.scale("compute", 100))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .mockSpi().addInstances().post().bodyCheck(s -> verifyGroup(s, Map.of("compute", 99)), 1).verify()
                .when(distroXTestClient.scale("compute", 200))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .mockSpi().addInstances().post().bodyCheck(s -> verifyGroup(s, Map.of("compute", 100)), 1).verify()
                .given(DistroXUpgradeTestDto.class)
                .withLockComponents(true)
                .withRuntime(null)
                .withReplaceVms(DistroXUpgradeReplaceVms.ENABLED)
                .given(distroXName, DistroXTestDto.class)
                .resetCalls()
                .when(distroXTestClient.upgrade())
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .mockSpi().addInstances().post().bodyCheck(s -> verifyGroup(s, Map.of("master", 1, "worker", 3, "compute", 96)), 1).verify()
                .mockSpi().addInstances().post().bodyCheck(s -> verifyGroup(s, Map.of("compute", 100)), 1).verify()
                .mockSpi().addInstances().post().bodyCheck(s -> verifyGroup(s, Map.of("compute", 4)), 1).verify()
                .validate();
    }

    private static boolean verifyGroup(String s, Map<String, Integer> groupSizeMap) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<Group> groups = objectMapper.readValue(s, new TypeReference<>() { });
            if (!groups.stream().map(Group::getName).collect(Collectors.toSet()).equals(groupSizeMap.keySet())) {
                return false;
            }
            for (Group group : groups) {
                if (!groupSizeMap.containsKey(group.getName())) {
                    return false;
                } else {
                    Integer groupSize = groupSizeMap.get(group.getName());
                    if (groupSize != group.getInstances().size()) {
                        return false;
                    }
                }
            }
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private String getNextRuntimeVersion(String runtime) {
        if ("7.2.18".equals(runtime)) {
            return "7.3.1";
        }
        String[] split = runtime.split("\\.");
        int last = Integer.parseInt(split[split.length - 1]);
        List<String> elements = new ArrayList<>(Arrays.asList(split).subList(0, split.length - 1));
        elements.add(String.valueOf(last + 1));
        return String.join(".", elements);
    }

    private static boolean isSaltMasterStop(String saltActionJson) {
        try {
            JsonNode saltAction = new ObjectMapper().readTree(saltActionJson);
            return SALT_STOP_ACTION.equals(saltAction.path("action").asText()) && !saltAction.path("masters").isEmpty();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private static List<OrderedOSUpgradeSet> gatewayFirstUpgradeSets(StackV4Response stack) {
        List<Set<String>> instanceIdsOfGatewayGroupFirst = stack.getInstanceGroups().stream()
                .sorted(Comparator.comparing(instanceGroup -> instanceGroup.getType() != InstanceGroupType.GATEWAY))
                .map(instanceGroup -> instanceGroup.getMetadata().stream().map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toSet()))
                .filter(not(Set::isEmpty))
                .toList();
        return IntStream.range(0, instanceIdsOfGatewayGroupFirst.size())
                .mapToObj(order -> new OrderedOSUpgradeSet(order, instanceIdsOfGatewayGroupFirst.get(order)))
                .toList();
    }

    protected void createImageCatalogForOsUpgrade(MockedTestContext testContext, String name) {
        createImageCatalog(testContext, name, getImageCatalogMockServerSetup().getUpgradeImageCatalogUrl());
    }

    private void createImageCatalog(MockedTestContext testContext, String name, String url) {
        testContext
                .given(ImageCatalogTestDto.class)
                .withName(name)
                .withUrl(url)
                .when(imageCatalogTestClient.createV4(), key(name));
    }
}
