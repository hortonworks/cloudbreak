package com.sequenceiq.cloudbreak.startup;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.VolumeTemplateRepository;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class MissingVolumeTemplatesMigrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MissingVolumeTemplatesMigrator.class);

    @Inject
    private StackService stackService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private VolumeTemplateRepository volumeTemplateRepository;

    @Inject
    private CloudParameterService cloudParameterService;

    private final AtomicBoolean finished = new AtomicBoolean(false);

    public void run() {
        try {
            Set<Stack> liveStacks = stackService.getAllAliveWithInstanceGroups();
            liveStacks.stream()
                    .forEach(stack -> stack.getInstanceGroups().stream()
                        .filter(ig -> ig.getTemplate().getVolumeTemplates().isEmpty())
                        .forEach(ig -> addMountedVolumesToInstanceMetadata(stack, ig)));
        } catch (Exception e) {
            LOGGER.error("Exception during missing volumes migration: ", e);
        }
        finished.set(true);
    }

    private void addMountedVolumesToInstanceMetadata(Stack stack, InstanceGroup instanceGroup) {
        List<Resource> diskResources = stack.getDiskResources();
        Optional<Resource> resource = diskResources.stream()
                .filter(diskResource -> diskResource.getInstanceGroup().contentEquals(instanceGroup.getGroupName()))
                .findFirst();
        if (resource.isPresent()) {
            setVolumeTemplateForInstanceGroup(stack, instanceGroup, resource.get());
        } else {
            setDefaultVolumeTemplateForInstanceGroup(stack, instanceGroup);
        }
    }

    private void setVolumeTemplateForInstanceGroup(Stack stack, InstanceGroup instanceGroup, Resource diskResource) {
        Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(diskResource, VolumeSetAttributes.class);
        if (attributes.isPresent()) {
            VolumeSetAttributes volumeSetAttributes = attributes.get();
            try {
                Set<String> volumeTypes = volumeSetAttributes.getVolumes().stream().map(VolumeSetAttributes.Volume::getType).collect(Collectors.toSet());
                volumeTypes.stream().forEach(volumeType -> createVolumeTemplate(volumeSetAttributes, instanceGroup, volumeType));
                LOGGER.info("Added volumetemplate to template in instancegroup {} in stack {}", instanceGroup.getGroupName(), stack.getName());
            } catch (Exception e) {
                LOGGER.error("Exception occured during addition of volumetemplate to template in instancegroup "
                        + diskResource.getInstanceGroup() + " in stack " + stack.getName(), e);
            }
        }
    }

    private void setDefaultVolumeTemplateForInstanceGroup(Stack stack, InstanceGroup instanceGroup) {
        try {
            VmRecommendations recommendations = cloudParameterService.getRecommendation(stack.cloudPlatform());
            VmRecommendation recommendation = recommendations.getWorker();
            PlatformDisks platformDisks = cloudParameterService.getDiskTypes();
            Platform platform = platform(stack.cloudPlatform());
            DiskTypes diskTypes = new DiskTypes(
                    platformDisks.getDiskTypes().get(platform),
                    platformDisks.getDefaultDisks().get(platform),
                    platformDisks.getDiskMappings().get(platform),
                    platformDisks.getDiskDisplayNames().get(platform));
            VolumeParameterType volumeParameterType = VolumeParameterType.valueOf(recommendation.getVolumeType());
            if (diskTypes.diskMapping().containsValue(volumeParameterType)) {
                Optional<Map.Entry<String, VolumeParameterType>> recommendedVolumeName = diskTypes
                        .diskMapping()
                        .entrySet()
                        .stream()
                        .filter(entry -> volumeParameterType.equals(entry.getValue()))
                        .findFirst();
                if (recommendedVolumeName.isPresent()) {
                    createDefaultVolumeTemplate(instanceGroup, recommendedVolumeName.get().getKey(),
                            Math.toIntExact(recommendation.getVolumeCount()), Math.toIntExact(recommendation.getVolumeSizeGB()));
                    LOGGER.info("Added default volumetemplate to template in instancegroup {} in stack {}", instanceGroup.getGroupName(), stack.getName());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception occured during addition of volumetemplate to template in instancegroup "
                    + instanceGroup.getGroupName() + " in stack " + stack.getName(), e);
        }
    }

    private void createVolumeTemplate(VolumeSetAttributes volumeSetAttributes, InstanceGroup instanceGroup, String volumeType) {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(volumeType);
        volumeTemplate.setVolumeSize(volumeSetAttributes.getVolumes().stream()
                .filter(volume -> volume.getType().contentEquals(volumeType))
                .findFirst()
                .get()
                .getSize());
        volumeTemplate.setVolumeCount(Math.toIntExact(volumeSetAttributes.getVolumes().stream()
                .filter(volume -> volume.getType().contentEquals(volumeType))
                .count()));
        volumeTemplate.setTemplate(instanceGroup.getTemplate());
        volumeTemplateRepository.save(volumeTemplate);
    }

    private void createDefaultVolumeTemplate(InstanceGroup instanceGroup, String volumeType, Integer volumeCount, Integer volumeSize) {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(volumeType);
        volumeTemplate.setVolumeSize(volumeSize);
        volumeTemplate.setVolumeCount(volumeCount);
        volumeTemplate.setTemplate(instanceGroup.getTemplate());
        volumeTemplateRepository.save(volumeTemplate);
    }

    public boolean isFinished() {
        return finished.get();
    }
}
