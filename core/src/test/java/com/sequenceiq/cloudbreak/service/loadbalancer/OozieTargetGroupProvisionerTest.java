package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.common.network.LoadBalancerConstants.STICKY_SESSION_FOR_LOAD_BALANCER_TARGET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.TargetGroupType;

class OozieTargetGroupProvisionerTest {

    private static final String TEST_CLUSTER_NAME = "TestCluster";

    private OozieTargetGroupProvisioner underTest;

    private String blueprintText = getBlueprintText("input/cdp-data-engineering.bp");

    @BeforeEach
    void setUp() {
        underTest = new OozieTargetGroupProvisioner();
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void setupOozieHATargetGroup(String cloudPlatform, String instanceGroupName, int nodeCount, String runtimeVersion,
            boolean resultPresent, TargetGroupType targetGroupType) {

        Stack stack = new Stack();
        stack.setName(TEST_CLUSTER_NAME);
        Cluster cluster = new Cluster();
        Blueprint blueprint = new Blueprint();
        blueprintText = blueprintText.replace('"' + "7.2.16" + '"', '"' + runtimeVersion + '"');
        blueprint.setBlueprintText(blueprintText);
        cluster.setBlueprint(blueprint);
        InstanceGroup ig = new InstanceGroup();
        ig.setGroupName(instanceGroupName);
        ig.setStack(stack);
        Set<InstanceMetaData> imSet = new HashSet<>();
        for (int i = 0; i < nodeCount; i++) {
            imSet.add(new InstanceMetaData());
        }
        ig.setInstanceMetaData(imSet);
        Set<InstanceGroup> instanceGroups = Set.of(ig);
        stack.setCluster(cluster);
        stack.setCloudPlatform(cloudPlatform);
        stack.setInstanceGroups(instanceGroups);

        Optional<TargetGroup> resultOpt = underTest.setupOozieHATargetGroup(stack, false);

        if (resultPresent) {
            assertThat(resultOpt).isPresent();
            assertThat(resultOpt).hasValueSatisfying(tg -> {
                assertThat(tg.getType()).isEqualTo(targetGroupType);
                assertThat(ig.getTargetGroups()).contains(tg);
            });
        } else {
            assertThat(resultOpt).isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("stickyLBTargetScenarios")
    void testLoadBalancerTargetStickyness(String cloudPlatform, String instanceGroupName, int nodeCount, String runtimeVersion,
            boolean resultPresent, TargetGroupType targetGroupType, boolean stickySessionOnNetwork, boolean expectedStickySession) {

        Stack stack = new Stack();
        stack.setName(TEST_CLUSTER_NAME);
        Cluster cluster = new Cluster();
        Blueprint blueprint = new Blueprint();
        blueprintText = blueprintText.replace('"' + "7.2.16" + '"', '"' + runtimeVersion + '"');
        blueprint.setBlueprintText(blueprintText);
        cluster.setBlueprint(blueprint);
        InstanceGroup ig = new InstanceGroup();
        ig.setGroupName(instanceGroupName);
        ig.setStack(stack);
        Set<InstanceMetaData> imSet = new HashSet<>();
        for (int i = 0; i < nodeCount; i++) {
            imSet.add(new InstanceMetaData());
        }
        ig.setInstanceMetaData(imSet);
        Set<InstanceGroup> instanceGroups = Set.of(ig);
        stack.setCluster(cluster);
        stack.setCloudPlatform(cloudPlatform);
        stack.setInstanceGroups(instanceGroups);
        Network network = new Network();
        network.setCloudPlatform(cloudPlatform);
        network.setId(1L);
        network.setAttributes(Json.silent(Map.of("loadBalancer", Map.of(STICKY_SESSION_FOR_LOAD_BALANCER_TARGET, stickySessionOnNetwork))));
        stack.setNetwork(network);

        Optional<TargetGroup> result = underTest.setupOozieHATargetGroup(stack, false);

        if (resultPresent) {
            assertTrue(result.isPresent());
            assertEquals(expectedStickySession, result.get().isUseStickySession());
        } else {
            assertTrue(result.isEmpty());
        }
    }
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
                { "AWS",   "master", 2, "7.2.16", true, TargetGroupType.OOZIE },
                { "GCP",   "master", 2, "7.2.16", true, TargetGroupType.OOZIE_GCP },
                { "AZURE", "master", 2, "7.2.16", true, TargetGroupType.OOZIE },
                { "AWS",   "mastr2", 2, "7.2.16", false, null },
                { "AWS",   "master", 2, "7.2.10", false, null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    // CHECKSTYLE:OFF
    static Object[][] stickyLBTargetScenarios() {
        return new Object[][]{
                {"AWS", "master", 2, "7.2.16", true, TargetGroupType.OOZIE, true, true},
                {"AWS", "master", 2, "7.2.16", true, TargetGroupType.OOZIE, false, false},
                {"GCP", "master", 2, "7.2.16", true, TargetGroupType.OOZIE_GCP, false, false},
                {"GCP", "master", 2, "7.2.16", true, TargetGroupType.OOZIE_GCP, true, false},
                {"AZURE", "master", 2, "7.2.16", true, TargetGroupType.OOZIE, false, false},
                {"AZURE", "master", 2, "7.2.16", true, TargetGroupType.OOZIE, true, false},
                {"AWS", "mastr2", 2, "7.2.16", false, null, false, false},
                {"AWS", "master", 2, "7.2.10", false, null, false, false},
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

}
