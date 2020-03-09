package com.sequenceiq.distrox.v1.distrox.converter.cli.distrox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cloudera.cdp.datahub.model.CreateAzureClusterRequest;
import com.cloudera.cdp.datahub.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AzureInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.RootVolumeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.tags.TagsV1Request;

class DistroXV1RequestToCreateAzureClusterRequestConverterTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private DistroXV1RequestToCreateAzureClusterRequestConverter underTest = new DistroXV1RequestToCreateAzureClusterRequestConverter();

    @Test
    void convert() {
        DistroXV1Request request = getDistroXV1Request();
        CreateAzureClusterRequest result = underTest.convert(request);
        assertEquals(request.getName(), result.getClusterName());
        assertEquals(request.getCluster().getBlueprintName(), result.getClusterTemplateName());
        assertEquals(request.getEnvironmentName(), result.getEnvironmentName());
        assertEquals(request.getImage().getCatalog(), result.getImage().getCatalogName());
        assertEquals(request.getImage().getId(), result.getImage().getId());
        assertEquals(request.getNetwork().getAzure().getSubnetId(), result.getSubnetId());
        assertEquals(request.getTags().getUserDefined().size(), result.getTags().size());
        request.getTags().getUserDefined().forEach((key, value) ->
                assertTrue(result.getTags().stream().anyMatch(tr -> key.equals(tr.getKey()) && value.equals(tr.getValue()))));
        assertInstanceGroups(request.getInstanceGroups(), result.getInstanceGroups());
    }

    private DistroXV1Request getDistroXV1Request() {
        DistroXV1Request request = new DistroXV1Request();
        request.setName("sdxName");
        DistroXClusterV1Request distroxCluster = new DistroXClusterV1Request();
        distroxCluster.setBlueprintName("blueprintName");
        request.setCluster(distroxCluster);
        request.setEnvironmentName("environmentName");
        DistroXImageV1Request image = new DistroXImageV1Request();
        image.setCatalog("catalog");
        image.setId("imageId");
        request.setImage(image);
        NetworkV1Request network = new NetworkV1Request();
        AzureNetworkV1Parameters azureNetwork = new AzureNetworkV1Parameters();
        azureNetwork.setSubnetId("subnetId");
        network.setAzure(azureNetwork);
        request.setNetwork(network);
        TagsV1Request tags = new TagsV1Request();
        Map<String, String> userDefinedTags = Map.of("k1", "v1", "k2", "v2");
        tags.setUserDefined(userDefinedTags);
        request.setTags(tags);
        InstanceGroupV1Request instanceGroup1 = getInstanceGroupV1Request("ig1");
        InstanceGroupV1Request instanceGroup2 = getInstanceGroupV1Request("ig2");
        request.setInstanceGroups(Set.of(instanceGroup1, instanceGroup2));
        return request;
    }

    private InstanceGroupV1Request getInstanceGroupV1Request(String igName) {
        InstanceGroupV1Request instanceGroup = new InstanceGroupV1Request();
        instanceGroup.setName(igName);
        InstanceTemplateV1Request igTemplate = new InstanceTemplateV1Request();
        RootVolumeV1Request rootVol = new RootVolumeV1Request();
        rootVol.setSize(SECURE_RANDOM.nextInt());
        igTemplate.setRootVolume(rootVol);
        igTemplate.setInstanceType(igName + "_instanceType");
        VolumeV1Request attachedVolues = new VolumeV1Request();
        attachedVolues.setCount(SECURE_RANDOM.nextInt());
        attachedVolues.setSize(SECURE_RANDOM.nextInt());
        attachedVolues.setType(igName);
        igTemplate.setAttachedVolumes(Set.of(attachedVolues));
        AzureInstanceTemplateV1Parameters azureTemplateParams = new AzureInstanceTemplateV1Parameters();
        azureTemplateParams.setEncrypted(true);
        azureTemplateParams.setManagedDisk(true);
        azureTemplateParams.setPrivateId("1.2.3.4");
        igTemplate.setAzure(azureTemplateParams);
        instanceGroup.setTemplate(igTemplate);
        instanceGroup.setNodeCount(SECURE_RANDOM.nextInt());
        instanceGroup.setType(InstanceGroupType.values()[SECURE_RANDOM.nextInt(InstanceGroupType.values().length)]);
        instanceGroup.setRecipeNames(Set.of(igName));
        instanceGroup.setRecoveryMode(RecoveryMode.values()[SECURE_RANDOM.nextInt(RecoveryMode.values().length)]);
        return instanceGroup;
    }

    private void assertInstanceGroups(Set<InstanceGroupV1Request> request, List<InstanceGroupRequest> result) {
        request.forEach(expected -> {
            Optional<InstanceGroupRequest> first = result.stream().filter(ig -> expected.getName().equals(ig.getInstanceGroupName())).findFirst();
            assertTrue(first.isPresent());
            InstanceGroupRequest igRequest = first.get();
            assertInstanceGroup(expected, igRequest);
        });
    }

    private void assertInstanceGroup(InstanceGroupV1Request expected, InstanceGroupRequest igRequest) {
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
        assertThat(igRequest.getVolumeEncryption().getEnableEncryption())
                .isEqualTo(expected.getTemplate().getAzure().getEncrypted());
    }

}