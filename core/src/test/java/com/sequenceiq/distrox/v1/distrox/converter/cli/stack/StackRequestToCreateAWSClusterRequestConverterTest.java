package com.sequenceiq.distrox.v1.distrox.converter.cli.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.cdp.datahub.model.CreateAWSClusterRequest;
import com.cloudera.cdp.datahub.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.common.api.tag.request.TagsRequest;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class StackRequestToCreateAWSClusterRequestConverterTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String ENV_NAME = "envName";

    private static final String CRN = "crn";

    @Mock
    private EnvironmentClientService environmentClientService;

    @InjectMocks
    private StackRequestToCreateAWSClusterRequestConverter underTest;

    @BeforeEach
    void setUp() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setName(ENV_NAME);
        when(environmentClientService.getByCrn(CRN)).thenReturn(environment);
    }

    @Test
    void convert() {
        StackV4Request request = getStackV4Request();
        CreateAWSClusterRequest result = underTest.convert(request);
        assertEquals(request.getName(), result.getClusterName());
        assertEquals(request.getCluster().getBlueprintName(), result.getClusterTemplateName());
        assertEquals(ENV_NAME, result.getEnvironmentName());
        assertEquals(request.getImage().getCatalog(), result.getImage().getCatalogName());
        assertEquals(request.getImage().getId(), result.getImage().getId());
        assertEquals(request.getNetwork().getAws().getSubnetId(), result.getSubnetId());
        assertEquals(request.getTags().getUserDefined().size(), result.getTags().size());
        request.getTags().getUserDefined().getAll().forEach((key, value) ->
                assertTrue(result.getTags().stream().anyMatch(tr -> key.equals(tr.getKey()) && value.equals(tr.getValue()))));
        assertInstanceGroups(request.getInstanceGroups(), result.getInstanceGroups());
    }

    private StackV4Request getStackV4Request() {
        StackV4Request request = new StackV4Request();
        request.setName("sdxName");
        ClusterV4Request distroxCluster = new ClusterV4Request();
        distroxCluster.setBlueprintName("blueprintName");
        request.setCluster(distroxCluster);
        request.setEnvironmentCrn(CRN);
        ImageSettingsV4Request image = new ImageSettingsV4Request();
        image.setCatalog("catalog");
        image.setId("imageId");
        request.setImage(image);
        NetworkV4Request network = new NetworkV4Request();
        AwsNetworkV4Parameters awsNetwork = new AwsNetworkV4Parameters();
        awsNetwork.setSubnetId("subnetId");
        network.setAws(awsNetwork);
        request.setNetwork(network);
        TagsV4Request tags = new TagsV4Request();
        tags.setUserDefined(new TagsRequest(Map.of("k1", "v1", "k2", "v2")));
        request.setTags(tags);
        InstanceGroupV4Request instanceGroup1 = getInstanceGroupV4Request("ig1");
        InstanceGroupV4Request instanceGroup2 = getInstanceGroupV4Request("ig2");
        request.setInstanceGroups(List.of(instanceGroup1, instanceGroup2));
        return request;
    }

    private InstanceGroupV4Request getInstanceGroupV4Request(String igName) {
        InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
        instanceGroup.setName(igName);
        InstanceTemplateV4Request igTemplate = new InstanceTemplateV4Request();
        RootVolumeV4Request rootVol = new RootVolumeV4Request();
        rootVol.setSize(SECURE_RANDOM.nextInt());
        igTemplate.setRootVolume(rootVol);
        igTemplate.setInstanceType(igName + "_instanceType");
        VolumeV4Request attachedVolues = new VolumeV4Request();
        attachedVolues.setCount(SECURE_RANDOM.nextInt());
        attachedVolues.setSize(SECURE_RANDOM.nextInt());
        attachedVolues.setType(igName);
        igTemplate.setAttachedVolumes(Set.of(attachedVolues));
        AwsInstanceTemplateV4Parameters awsTemplate = new AwsInstanceTemplateV4Parameters();
        AwsEncryptionV4Parameters awsEncParams = new AwsEncryptionV4Parameters();
        awsEncParams.setKey("encKey_" + igName);
        awsEncParams.setType(EncryptionType.values()[SECURE_RANDOM.nextInt(EncryptionType.values().length)]);
        awsTemplate.setEncryption(awsEncParams);
        igTemplate.setAws(awsTemplate);
        instanceGroup.setTemplate(igTemplate);
        instanceGroup.setNodeCount(SECURE_RANDOM.nextInt());
        instanceGroup.setType(InstanceGroupType.values()[SECURE_RANDOM.nextInt(InstanceGroupType.values().length)]);
        instanceGroup.setRecipeNames(Set.of(igName));
        instanceGroup.setRecoveryMode(RecoveryMode.values()[SECURE_RANDOM.nextInt(RecoveryMode.values().length)]);
        return instanceGroup;
    }

    private void assertInstanceGroups(List<InstanceGroupV4Request> request, List<InstanceGroupRequest> result) {
        request.forEach(expected -> {
            Optional<InstanceGroupRequest> first = result.stream().filter(ig -> expected.getName().equals(ig.getInstanceGroupName())).findFirst();
            assertTrue(first.isPresent());
            InstanceGroupRequest igRequest = first.get();
            assertInstanceGroup(expected, igRequest);
        });
    }

    private void assertInstanceGroup(InstanceGroupV4Request expected, InstanceGroupRequest igRequest) {
        expected.getTemplate().getAttachedVolumes().forEach(av -> assertThat(igRequest.getAttachedVolumeConfiguration().stream()
                .anyMatch(igr -> Objects.equals(av.getCount(), igr.getVolumeCount())
                        && Objects.equals(av.getSize(), igr.getVolumeSize())
                        && Objects.equals(av.getType(), igr.getVolumeType())))
                .isTrue());
        assertThat(igRequest.getInstanceGroupType()).isEqualTo(expected.getType().name());
        assertThat(igRequest.getInstanceType()).isEqualTo(expected.getTemplate().getInstanceType());
        assertThat(igRequest.getNodeCount()).isEqualTo(Integer.valueOf(expected.getNodeCount()));
        assertThat(igRequest.getRecipeNames()).hasSameElementsAs(expected.getRecipeNames());
        assertThat(igRequest.getRecoveryMode()).isEqualTo(expected.getRecoveryMode().name());
        assertThat(igRequest.getRootVolumeSize()).isEqualTo(expected.getTemplate().getRootVolume().getSize());
        assertThat(igRequest.getVolumeEncryption().getEncryptionKey())
                .isEqualTo(expected.getTemplate().getAws().getEncryption().getType() == EncryptionType.NONE
                        ? null
                        : expected.getTemplate().getAws().getEncryption().getKey());
        assertThat(igRequest.getVolumeEncryption().getEnableEncryption())
                .isEqualTo(expected.getTemplate().getAws().getEncryption().getType() != EncryptionType.NONE);

    }
}
