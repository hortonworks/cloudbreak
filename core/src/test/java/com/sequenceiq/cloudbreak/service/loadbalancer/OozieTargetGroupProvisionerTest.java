package com.sequenceiq.cloudbreak.service.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.TargetGroupType;

class OozieTargetGroupProvisionerTest {

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
        stack.setName("TestStack");
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
                assertThat(tg.getInstanceGroups()).isEqualTo(Set.of(ig));
                assertThat(ig.getTargetGroups()).contains(tg);
            });
        } else {
            assertThat(resultOpt).isEmpty();
        }
    }

    // @formatter:off
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

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
