package com.sequenceiq.cloudbreak.service.metering;

import static com.sequenceiq.cloudbreak.constant.AwsPlatformResourcesFilterConstants.ARCHITECTURE;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
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
import com.sequenceiq.cloudbreak.service.multiaz.ProviderBasedMultiAzSetupValidator;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
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

    @Inject
    private VerticalScaleInstanceProvider verticalScaleInstanceProvider;

    @Inject
    private ProviderBasedMultiAzSetupValidator providerBasedMultiAzSetupValidator;

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
            CloudVmTypes vmTypes = cloudParameterService.getVmTypesV2(
                    extendedCloudCredential,
                    stack.getRegion(),
                    stack.getCloudPlatform(),
                    CdpResourceType.DATAHUB,
                    Map.of(ARCHITECTURE, stack.getArchitecture().getName()));
            handleMismatchingInstanceTypes(stack, mismatchingInstanceGroups, vmTypes);
        } catch (Exception e) {
            metricService.incrementMetricCounter(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED);
            LOGGER.warn("Cannot handle mismatching instance types", e);
            throw e;
        }
    }

    private void handleMismatchingInstanceTypes(StackDto stack, Set<MismatchingInstanceGroup> mismatchingInstanceGroups, CloudVmTypes availableVmTypes) {
        mismatchingInstanceGroups.forEach(mismatchingInstanceGroup -> {
            Long instanceGroupId = stack.getInstanceGroupByInstanceGroupName(mismatchingInstanceGroup.instanceGroup()).getInstanceGroup().getId();
            Set<String> groupAvailabilityZones = stack.getAvailabilityZonesByInstanceGroup(instanceGroupId);
            String instanceTypeFromTemplate = mismatchingInstanceGroup.instanceTypeFromTemplate();
            Set<String> actualInstanceTypes = new HashSet<>(mismatchingInstanceGroup.instanceTypesByInstanceId().values());
            String availabilityZone = verticalScaleInstanceProvider.getAvailabilityZone(stack.getAvailabilityZone(), availableVmTypes.getCloudVmResponses());
            boolean validateMultiAz = stack.getStack().isMultiAz() && providerBasedMultiAzSetupValidator.getAvailabilityZoneConnector(stack.getStack()) != null;
            CloudVmTypes suitableVmTypes = verticalScaleInstanceProvider.listInstanceTypes(stack.getAvailabilityZone(),
                    mismatchingInstanceGroup.instanceTypeFromTemplate(), availableVmTypes, validateMultiAz ? groupAvailabilityZones : null);

            String largestSuitableInstanceType = calculateLargestSuitableInstanceType(suitableVmTypes.getCloudVmResponses().get(availabilityZone),
                    actualInstanceTypes);
            if (largestSuitableInstanceType == null) {
                LOGGER.warn("Mismatching instance types found for group {}, but no suitable instance type found in {}, no further action needed!." +
                                " (instance type from template: {})",
                        mismatchingInstanceGroup.instanceGroup(), actualInstanceTypes, mismatchingInstanceGroup.instanceTypeFromTemplate());
            } else if (largestSuitableInstanceType.equals(instanceTypeFromTemplate)) {
                LOGGER.info("Mismatching instance types found for group {}, but the instance type from template {} is not smaller than {}, " +
                                "no further action needed!",
                        mismatchingInstanceGroup.instanceGroup(), mismatchingInstanceGroup.instanceTypeFromTemplate(), actualInstanceTypes);
            } else {
                changeInstanceTypeInTemplate(stack, mismatchingInstanceGroup.instanceGroup(), largestSuitableInstanceType);
                metricService.incrementMetricCounter(MetricType.METERING_CHANGE_INSTANCE_TYPE_SUCCESSFUL);
            }
        });
    }

    private String calculateLargestSuitableInstanceType(Set<VmType> suitableVmTypes, Set<String> actualInstanceTypes) {
        return suitableVmTypes.stream()
                .filter(vmType -> actualInstanceTypes.contains(vmType.value()))
                .sorted(new VmTypeComparator())
                .map(StringType::value)
                .findFirst()
                .orElse(null);
    }

    private void changeInstanceTypeInTemplate(StackDto stack, String instanceGroup, String newInstanceType) {
        LOGGER.info("Change instance type in template for instance group: {}, new instance type: {}", instanceGroup, newInstanceType);
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
        InstanceStoreMetadata instanceStoreMetadata = connector.metadata().collectInstanceStorageCount(ac, List.of(newInstanceType));
        Integer instanceStorageCount = instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled(newInstanceType);
        Integer instanceStorageSize = instanceStoreMetadata.mapInstanceTypeToInstanceSizeNullHandled(newInstanceType);
        CoreVerticalScaleRequest<CoreVerticalScaleResult> request = new CoreVerticalScaleRequest<>(cloudContext, cloudCredential, cloudStack, cloudResources,
                stackVerticalScaleV4Request);
        try {
            stackUpscaleService.verticalScaleWithoutInstances(ac, request, connector, instanceGroup);
        } catch (Exception e) {
            LOGGER.warn("Vertical scale without instances on provider failed", e);
            throw new CloudConnectorException("Vertical scale without instances on provider failed", e);
        }
        coreVerticalScaleService.updateTemplateWithVerticalScaleInformation(stack.getId(), stackVerticalScaleV4Request, instanceStorageCount,
                instanceStorageSize);
        LOGGER.info("Changing instance type in template for instance group: {} to new instance type: {} finished", instanceGroup, newInstanceType);
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

    private class VmTypeComparator implements Comparator<VmType> {

        @Override
        public int compare(VmType vmType1, VmType vmType2) {
            Integer cpu1 = vmType1.getMetaData().getCPU();
            Integer cpu2 = vmType2.getMetaData().getCPU();

            int cpuComparsion = Integer.compare(cpu2, cpu1);
            if (cpuComparsion == 0) {
                Float memory1 = vmType1.getMetaData().getMemoryInGb();
                Float memory2 = vmType2.getMetaData().getMemoryInGb();
                int memoryComparsion = Float.compare(memory2, memory1);
                if (memoryComparsion == 0) {
                    String name1 = vmType1.value();
                    String name2 = vmType2.value();
                    return StringUtils.compare(name1, name2);
                }
                return memoryComparsion;
            }
            return cpuComparsion;
        }
    }
}
