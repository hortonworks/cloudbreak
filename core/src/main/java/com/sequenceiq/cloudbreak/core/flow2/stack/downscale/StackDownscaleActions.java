package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Configuration
public class StackDownscaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackDownscaleActions.class);

    private static final String RESOURCES = "RESOURCES";

    private static final String INSTANCES = "INSTANCES";

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private StackDownscaleService stackDownscaleService;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Inject
    private EnvironmentService environmentClientService;

    @Bean(name = "DOWNSCALE_COLLECT_RESOURCES_STATE")
    public Action<?, ?> stackDownscaleCollectResourcesAction() {
        return new AbstractStackDownscaleAction<>(StackDownscaleTriggerEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, StackDownscaleTriggerEvent payload, Map<Object, Object> variables) {
                stackDownscaleService.startStackDownscale(context, payload);
                StackView stack = context.getStack();
                LOGGER.debug("Assembling downscale stack event for stack: {}", stack.getName());
                StackDto stackDto = stackDtoService.getById(stack.getId());
                List<CloudResource> resources = stackDto.getResources()
                        .stream().map(r -> cloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
                variables.put(RESOURCES, resources);
                final Set<InstanceMetadataView> candidatesInstanceMetadata = stackDto.getNotTerminatedInstanceMetaData()
                        .stream()
                        .filter(im -> context.getHostGroupWithPrivateIds().values().stream().anyMatch(privateIds -> privateIds.contains(im.getPrivateId())))
                        .collect(Collectors.toSet());

                List<CloudInstance> instances = metadataConverter.convert(candidatesInstanceMetadata, stack);
                variables.put(INSTANCES, instances);

                final Map<String, String> addressesByFqdn = candidatesInstanceMetadata.stream()
                        .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null && instanceMetaData.getPrivateIp() != null)
                        .collect(toMap(InstanceMetadataView::getDiscoveryFQDN, InstanceMetadataView::getPrivateIp));
                clusterPublicEndpointManagementService.downscale(stackDto, addressesByFqdn);
                CloudStack cloudStack = stackToCloudStackConverter.convert(stackDto);
                Selectable request = new DownscaleStackCollectResourcesRequest(context.getCloudContext(),
                        context.getCloudCredential(), cloudStack, resources, instances);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_STATE")
    public Action<?, ?> stackDownscaleAction() {
        return new AbstractStackDownscaleAction<>(DownscaleStackCollectResourcesResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, DownscaleStackCollectResourcesResult payload, Map<Object, Object> variables) {
                StackDto stackDto = stackDtoService.getById(context.getStackId());
                CloudStack cloudStack = stackToCloudStackConverter.convert(stackDto);
                Selectable request = new DownscaleStackRequest(context.getCloudContext(), context.getCloudCredential(), cloudStack,
                        (List<CloudResource>) variables.get(RESOURCES), (List<CloudInstance>) variables.get(INSTANCES), payload.getResourcesToScale());
                sendEvent(context, request);
            }
        };
    }

    @Bean("DOWNSCALE_REMOVE_USERDATA_SECRETS_STATE")
    public Action<?, ?> downscaleRemoveUserdataSecretsAction() {
        return new AbstractStackDownscaleAction<>(DownscaleStackResult.class) {

            @Override
            protected void doExecute(StackScalingFlowContext context, DownscaleStackResult payload, Map<Object, Object> variables) {
                StackView stack = context.getStack();
                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                if (environment.isEnableSecretEncryption()) {
                    List<Long> downscaledInstancePrivateIds = context.getHostGroupWithPrivateIds().values().stream().flatMap(Collection::stream).toList();
                    DownscaleRemoveUserdataSecretsRequest request = new DownscaleRemoveUserdataSecretsRequest(stack.getId(), context.getCloudContext(),
                            context.getCloudCredential(), downscaledInstancePrivateIds);
                    sendEvent(context, request.selector(), request);
                } else {
                    LOGGER.info("Skipping userdata secret deletion, since secret encryption is not enabled.");
                    sendEvent(context, new DownscaleRemoveUserdataSecretsSuccess(stack.getId()));
                }
            }
        };
    }

    @Bean(name = "DOWNSCALE_FINISHED_STATE")
    public Action<?, ?> stackDownscaleFinishedAction() {
        return new AbstractStackDownscaleAction<>(DownscaleRemoveUserdataSecretsSuccess.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, DownscaleRemoveUserdataSecretsSuccess payload, Map<Object, Object> variables)
                    throws TransactionExecutionException {
                List<Long> privateIds = context.getHostGroupWithPrivateIds().values().stream().flatMap(Collection::stream).collect(Collectors.toList());
                stackDownscaleService.finishStackDownscale(context, privateIds);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new StackEvent(StackDownscaleEvent.DOWNSCALE_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "DOWNSCALE_FAILED_STATE")
    public Action<?, ?> stackDownscaleFailedAction() {
        return new AbstractStackFailureAction<StackDownscaleState, StackDownscaleEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackDownscaleService.handleStackDownscaleError(context, payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackDownscaleEvent.DOWNSCALE_FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
