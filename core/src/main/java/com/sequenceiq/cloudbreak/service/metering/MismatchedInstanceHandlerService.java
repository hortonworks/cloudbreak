package com.sequenceiq.cloudbreak.service.metering;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleResult;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.common.api.type.CdpResourceType;

@Service
public class MismatchedInstanceHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MismatchedInstanceHandlerService.class);

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CoreVerticalScaleService coreVerticalScaleService;

    @Inject
    private StackUpscaleService stackUpscaleService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CloudContextProvider cloudContextProvider;

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    public void handleMismatchingInstanceTypes(StackDto stack, Set<MismatchingInstanceGroup> mismatchingInstanceGroups) {
        if (mismatchingInstanceGroups.isEmpty()) {
            return;
        }

        try {
            LOGGER.info("Handle mismatching instance groups: {}", mismatchingInstanceGroups);
            ExtendedCloudCredential extendedCloudCredential = credentialClientService.getExtendedCloudCredential(stack.getEnvironmentCrn());
            CloudVmTypes vmTypes = cloudParameterService.getVmTypesV2(extendedCloudCredential, stack.getRegion(), stack.getCloudPlatform(),
                    CdpResourceType.DATAHUB, Map.of());
            Set<VmType> availableVmTypes = getVmTypes(stack.getAvailabilityZone(), vmTypes);
            handleMismatchingInstanceTypes(stack, mismatchingInstanceGroups, availableVmTypes);
        } catch (Exception e) {
            metricService.incrementMetricCounter(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED);
            LOGGER.warn("Cannot handle mismatching instance types", e);
            throw e;
        }
    }

    private void handleMismatchingInstanceTypes(StackDto stack, Set<MismatchingInstanceGroup> mismatchingInstanceGroups, Set<VmType> availableVmTypes) {
        mismatchingInstanceGroups.forEach(mismatchingInstanceGroup -> {
            String originalInstanceType = mismatchingInstanceGroup.originalInstanceType();
            Set<String> mismatchingInstanceTypes = new HashSet<>(mismatchingInstanceGroup.mismatchingInstanceTypes().values());
            VmType originalVmType = availableVmTypes.stream()
                    .filter(vmType -> vmType.value().equals(originalInstanceType))
                    .findFirst()
                    .orElseThrow(NotFoundException.notFound("vmType", originalInstanceType));

            String largestInstanceType = calculateLargestInstanceType(availableVmTypes, Sets.union(Set.of(originalInstanceType), mismatchingInstanceTypes),
                    originalVmType);
            if (largestInstanceType != null && !largestInstanceType.equals(originalInstanceType)) {
                changeDefaultInstanceType(stack, mismatchingInstanceGroup.instanceGroup(), largestInstanceType);
                metricService.incrementMetricCounter(MetricType.METERING_CHANGE_INSTANCE_TYPE_SUCCESSFUL);
            } else {
                LOGGER.info("Mismatching instance types found for group {}, but the original instance type {} is larger than {}, no further action needed!",
                        mismatchingInstanceGroup.instanceGroup(), mismatchingInstanceGroup.originalInstanceType(), mismatchingInstanceTypes);
            }
        });
    }

    private Set<VmType> getVmTypes(String availabilityZone, CloudVmTypes vmTypes) {
        Set<VmType> availableVmTypes = Collections.emptySet();
        if (vmTypes.getCloudVmResponses() != null && StringUtils.isNotBlank(availabilityZone)
                && vmTypes.getDefaultCloudVmResponses().containsKey(availabilityZone)) {
            availableVmTypes = vmTypes.getCloudVmResponses().get(availabilityZone);
        } else if (vmTypes.getCloudVmResponses() != null && !vmTypes.getCloudVmResponses().isEmpty()) {
            availableVmTypes = vmTypes.getCloudVmResponses().values().iterator().next();
        }
        return availableVmTypes.stream()
                .filter(Objects::nonNull)
                .filter(VmType::isMetaSet)
                .filter(vmType -> ObjectUtils.allNotNull(vmType.value(), vmType.getMetaData().getCPU(), vmType.getMetaData().getMemoryInGb()))
                .collect(Collectors.toSet());
    }

    private String calculateLargestInstanceType(Set<VmType> availableVmTypes, Set<String> existingInstanceTypes, VmType originalVmType) {
        return availableVmTypes.stream()
                .filter(vmType -> existingInstanceTypes.contains(vmType.value()))
                .filter(vmType -> filterVmTypeLargerThanOriginal(vmType, originalVmType))
                .sorted(new VmTypeComparator())
                .map(StringType::value)
                .findFirst()
                .orElse(null);
    }

    private void changeDefaultInstanceType(StackDto stack, String instanceGroup, String newInstanceType) {
        LOGGER.info("Change default instance type for instance group: {}, new instance type: {}", instanceGroup, newInstanceType);
        CloudContext cloudContext = cloudContextProvider.getCloudContext(stack);
        CloudCredential cloudCredential = credentialClientService.getCloudCredential(stack.getEnvironmentCrn());
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
        CloudStack cloudStack = stackToCloudStackConverter.convert(stack);
        StackVerticalScaleV4Request stackVerticalScaleV4Request = createStackVerticalScaleV4Request(stack, instanceGroup, newInstanceType);
        cloudStack = stackToCloudStackConverter.updateWithVerticalScaleRequest(cloudStack, stackVerticalScaleV4Request);
        Set<Resource> resources = stack.getResources();
        List<CloudResource> cloudResources = resources.stream()
                .map(resource -> resourceToCloudResourceConverter.convert(resource))
                .collect(Collectors.toList());
        CoreVerticalScaleRequest<CoreVerticalScaleResult> request = new CoreVerticalScaleRequest<>(cloudContext, cloudCredential, cloudStack, cloudResources,
                stackVerticalScaleV4Request);
        try {
            stackUpscaleService.verticalScaleWithoutInstances(ac, request, connector, instanceGroup);
        } catch (Exception e) {
            LOGGER.warn("Vertical scale without instances on provider failed", e);
            throw new CloudConnectorException("Vertical scale without instances on provider failed", e);
        }
        coreVerticalScaleService.updateTemplateWithVerticalScaleInformation(stack.getId(), stackVerticalScaleV4Request);
        LOGGER.info("Changing default instance type for instance group: {} to new instance type: {} finished", instanceGroup, newInstanceType);
    }

    private StackVerticalScaleV4Request createStackVerticalScaleV4Request(StackDto stack, String instanceGroup, String newInstanceType) {
        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setStackId(stack.getId());
        stackVerticalScaleV4Request.setGroup(instanceGroup);
        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType(newInstanceType);
        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);
        return stackVerticalScaleV4Request;
    }

    private boolean filterVmTypeLargerThanOriginal(VmType vmType, VmType originalVmType) {
        Integer vmTypeCPU = vmType.getMetaData().getCPU();
        Float vmTypeMemory = vmType.getMetaData().getMemoryInGb();
        Integer originalCPU = originalVmType.getMetaData().getCPU();
        Float originalMemory = originalVmType.getMetaData().getMemoryInGb();
        if (vmTypeCPU.equals(originalCPU) && vmTypeMemory.equals(originalMemory)) {
            return false;
        } else {
            return vmTypeCPU >= originalCPU && vmTypeMemory >= originalMemory;
        }
    }

    private class VmTypeComparator implements Comparator<VmType> {

        @Override
        public int compare(VmType vmType1, VmType vmType2) {
            Integer cpu1 = vmType1.getMetaData().getCPU();
            Integer cpu2 = vmType2.getMetaData().getCPU();

            int cpuComparsion = Integer.compare(cpu2, cpu1);
            if (cpuComparsion == 0) {
                Float memory1 = vmType1.getMetaData().getMemoryInGb();
                Float memory2 = vmType2.getMetaData().getMemoryInGb();
                return Float.compare(memory2, memory1);
            }
            return cpuComparsion;
        }
    }
}
