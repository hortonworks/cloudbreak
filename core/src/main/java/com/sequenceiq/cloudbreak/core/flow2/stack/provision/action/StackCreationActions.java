package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants.START_DATE;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudPlatformResponseToFlowFailureConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;

@Configuration
public class StackCreationActions {
    @Inject
    private ImageService imageService;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private StackCreationService stackCreationService;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;
    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Bean(name = "SETUP_STATE")
    public Action provisioningSetupAction() {
        return new AbstractStackCreationAction<ProvisionRequest>(ProvisionRequest.class) {
            @Override
            protected void doExecute(StackContext context, ProvisionRequest payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new SetupRequest<SetupResult>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "IMAGESETUP_STATE")
    public Action prepareImageAction() {
        return new AbstractStackCreationAction<SetupResult>(SetupResult.class) {
            @Override
            protected void doExecute(StackContext context, SetupResult payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                CloudStack cloudStack = cloudStackConverter.convert(context.getStack());
                Image image = imageService.getImage(context.getCloudContext().getId());
                return new PrepareImageRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudStack, image);
            }
        };
    }

    @Bean(name = "START_PROVISIONING_STATE")
    public Action startProvisioningAction() {
        return new AbstractStackCreationAction<FlowStackEvent>(FlowStackEvent.class) {
            @Override
            protected void doExecute(StackContext context, FlowStackEvent payload, Map<Object, Object> variables) throws Exception {
                variables.put(START_DATE, new Date());
                stackCreationService.startProvisioning(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                FailurePolicy policy = Optional.fromNullable(context.getStack().getFailurePolicy()).or(new FailurePolicy());
                return new LaunchStackRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        policy.getAdjustmentType(), policy.getThreshold());
            }
        };
    }

    @Bean(name = "PROVISIONING_FINISHED_STATE")
    public Action provisioningFinishedAction() {
        return new AbstractStackCreationAction<LaunchStackResult>(LaunchStackResult.class) {
            @Override
            protected void doExecute(StackContext context, LaunchStackResult payload, Map<Object, Object> variables) throws Exception {
                Stack stack = stackCreationService.provisioningFinished(context, payload, variables);
                StackContext newContext = new StackContext(context.getFlowId(), stack, context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(context.getStack());
                List<CloudResource> cloudResources = cloudResourceConverter.convert(context.getStack().getResources());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances);
            }
        };
    }

    @Bean(name = "COLLECTMETADATA_STATE")
    public Action collectMetadataAction() {
        return new AbstractStackCreationAction<CollectMetadataResult>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackContext context, CollectMetadataResult payload, Map<Object, Object> variables) throws Exception {
                Stack stack = stackCreationService.setupMetadata(context, payload);
                StackContext newContext = new StackContext(context.getFlowId(), stack, context.getCloudContext(), context.getCloudCredential(),
                        context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                InstanceMetaData gatewayMetaData = context.getStack().getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
                CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData);
                return new GetSSHFingerprintsRequest<GetSSHFingerprintsResult>(context.getCloudContext(), context.getCloudCredential(), gatewayInstance);
            }
        };
    }

    @Bean(name = "TLS_SETUP_STATE")
    public Action tlsSetupAction() {
        return new AbstractStackCreationAction<GetSSHFingerprintsResult>(GetSSHFingerprintsResult.class) {
            @Override
            protected void doExecute(StackContext context, GetSSHFingerprintsResult payload, Map<Object, Object> variables) throws Exception {
                stackCreationService.setupTls(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new SelectableFlowStackEvent(context.getStack().getId(), StackCreationEvent.STACK_CREATION_FINISHED_EVENT.stringRepresentation());
            }
        };
    }

    @Bean(name = "STACK_CREATION_FAILED_STATE")
    public Action stackCreationFailureAction() {
        return new AbstractStackCreationAction<FlowFailureEvent>(FlowFailureEvent.class) {
            @Override
            protected void doExecute(StackContext context, FlowFailureEvent payload, Map<Object, Object> variables) throws Exception {
                stackCreationService.handleStackCreationFailure(context, payload.getException());
                sendEvent(context.getFlowId(), StackCreationEvent.STACK_CREATION_FAILED_EVENT.stringRepresentation(), payload);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return null;
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<FlowFailureEvent>> payloadConverters) {
                payloadConverters.add(new CloudPlatformResponseToFlowFailureConverter());
            }
        };
    }
}
