package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.datahub.model.AttachedVolumeRequest;
import com.cloudera.cdp.datahub.model.CreateAWSClusterRequest;
import com.cloudera.cdp.datahub.model.DatahubResourceTagRequest;
import com.cloudera.cdp.datahub.model.ImageRequest;
import com.cloudera.cdp.datahub.model.InstanceGroupRequest;
import com.cloudera.cdp.datahub.model.VolumeEncryptionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.EncryptionParametersV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.common.api.type.EncryptionType;

@Component
public class StackRequestToCreateAWSClusterRequestConverter {

    @Inject
    private EnvironmentClientService environmentClientService;

    public CreateAWSClusterRequest convert(StackV4Request source) {
        CreateAWSClusterRequest request = new CreateAWSClusterRequest();
        request.setClusterName(source.getName());
        request.setClusterTemplateName(source.getCluster().getBlueprintName());
        request.setEnvironmentName(getEnvironmentName(source.getEnvironmentCrn()));
        request.setImage(convertImageRequest(source.getImage()));
        request.setInstanceGroups(convertInstanceGroups(source.getInstanceGroups()));
        request.setSubnetId(source.getNetwork().getAws().getSubnetId());
        request.setTags(getIfNotNull(source.getTags(), this::getTags));
        return request;
    }

    private String getEnvironmentName(String environmentCrn) {
        return environmentClientService.getByCrn(environmentCrn).getName();
    }

    private List<InstanceGroupRequest> convertInstanceGroups(List<InstanceGroupV4Request> source) {
        List<InstanceGroupRequest> instanceGroups = new ArrayList<>();
        doIfNotNull(source, s -> s.forEach(ig -> {
            InstanceGroupRequest instanceGroup = new InstanceGroupRequest();
            instanceGroup.setAttachedVolumeConfiguration(convertAttachedVolumeConfiguration(ig.getTemplate().getAttachedVolumes()));
            instanceGroup.setInstanceGroupName(ig.getName());
            instanceGroup.setInstanceGroupType(ig.getType().name());
            instanceGroup.setInstanceType(ig.getTemplate().getInstanceType());
            instanceGroup.setNodeCount(ig.getNodeCount());
            instanceGroup.setRecipeNames(getIfNotNull(ig.getRecipeNames(), List::copyOf));
            instanceGroup.setRecoveryMode(ig.getRecoveryMode().name());
            instanceGroup.setRootVolumeSize(ig.getTemplate().getRootVolume().getSize());
            instanceGroup.setVolumeEncryption(convertVolumeEncryption(ig.getTemplate()));
            instanceGroups.add(instanceGroup);
        }));
        return instanceGroups;
    }

    private List<AttachedVolumeRequest> convertAttachedVolumeConfiguration(Set<VolumeV4Request> source) {
        List<AttachedVolumeRequest> attachedVolumes = new ArrayList<>();
        source.forEach(volume -> {
            AttachedVolumeRequest attachedVolume = new AttachedVolumeRequest();
            attachedVolume.setVolumeCount(volume.getCount());
            attachedVolume.setVolumeSize(volume.getSize());
            attachedVolume.setVolumeType(volume.getType());
            attachedVolumes.add(attachedVolume);
        });
        return attachedVolumes;
    }

    private VolumeEncryptionRequest convertVolumeEncryption(InstanceTemplateV4Request source) {
        EncryptionType encryptionType = Optional.ofNullable(source.getAws())
                .map(AwsInstanceTemplateV4Parameters::getEncryption)
                .map(EncryptionParametersV4Base::getType)
                .orElse(EncryptionType.NONE);
        VolumeEncryptionRequest encryptionRequest = new VolumeEncryptionRequest();
        encryptionRequest.setEnableEncryption(EncryptionType.NONE != encryptionType);
        if (EncryptionType.NONE != encryptionType) {
            encryptionRequest.setEncryptionKey(source.getAws().getEncryption().getKey());
        }
        return encryptionRequest;
    }

    private ImageRequest convertImageRequest(ImageSettingsV4Request source) {
        ImageRequest imageRequest = new ImageRequest();
        imageRequest.setId(source.getId());
        imageRequest.setCatalogName(source.getCatalog());
        return imageRequest;
    }

    private List<DatahubResourceTagRequest> getTags(TagsV4Request source) {
        List<DatahubResourceTagRequest> tags = new ArrayList<>();
        doIfNotNull(getIfNotNull(source, TagsV4Request::getUserDefined), userDefinedTags ->
                userDefinedTags.forEach((k, v) -> {
                    DatahubResourceTagRequest tag = new DatahubResourceTagRequest();
                    tag.setKey(k);
                    tag.setValue(v);
                    tags.add(tag);
                }));
        return tags;
    }
}
