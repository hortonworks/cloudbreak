package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.attach;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.ARM_TEMPLATE;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.service.LoadBalancerMetadataService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AttachPublicIpsAddLBHandler extends ExceptionCatcherEventHandler<AttachPublicIpsAddLBRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachPublicIpsAddLBHandler.class);

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private LoadBalancerMetadataService loadBalancerMetadataService;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AttachPublicIpsAddLBRequest> event) {
        return new SkuMigrationFailedEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<AttachPublicIpsAddLBRequest> event) {
        AttachPublicIpsAddLBRequest request = event.getData();
        try {
            LOGGER.info("Attaching public IPs and add LB");
            StackView stack = request.getStack();
            Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(request.getResourceId());
            CloudConnector connector = request.getCloudConnector();
            CloudCredential cloudCredential = request.getCloudCredential();
            CloudContext cloudContext = request.getCloudContext();
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            List<CloudResource> cloudResources = measure(() -> connector.resources().attachPublicIpAddressesForVMsAndAddLB(ac, request.getCloudStack(),
                            persistenceNotifier),
                    LOGGER, "Attaching public IP addresses and add LB took {} ms");
            List<LoadBalancerType> loadBalancerTypes = loadBalancers.stream().map(LoadBalancer::getType).toList();
            CloudResource armCloudResource = resourceRetriever.findByStatusAndTypeAndStack(CREATED, ARM_TEMPLATE, cloudContext.getId())
                    .orElseThrow(() -> new CloudbreakRuntimeException("Could not find ARM template in our resources"));
            cloudResources.add(armCloudResource);

            List<CloudLoadBalancerMetadata> loadBalancerStatuses = loadBalancerMetadataService.collectMetadata(cloudContext, cloudCredential,
                    loadBalancerTypes, cloudResources);
            metadataSetupService.saveLoadBalancerMetadata(stack, loadBalancerStatuses);

            List<InstanceMetadataView> instances = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stack.getId());
            List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(instances, stack);
            List<CloudVmMetaDataStatus> instanceStatuses = connector.metadata().collect(ac, cloudResources, cloudInstances, cloudInstances);

            updatePublicIps(instanceStatuses);
        } catch (Exception e) {
            LOGGER.error("Failed to attach public IPs and add LB", e);
            return new SkuMigrationFailedEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(),
                    request.getResourceId(), e);
        }

        return new AttachPublicIpsAddLBResult(request.getResourceId());
    }

    private void updatePublicIps(List<CloudVmMetaDataStatus> instanceStatuses) {
        instanceStatuses.stream()
                .filter(cloudVmMetaDataStatus -> cloudVmMetaDataStatus.getMetaData().getPublicIp() != null)
                .forEach(status -> instanceMetaDataService.updatePublicIp(status.getCloudVmInstanceStatus().getCloudInstance().getInstanceId(),
                        status.getMetaData().getPublicIp()));
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AttachPublicIpsAddLBRequest.class);
    }
}
