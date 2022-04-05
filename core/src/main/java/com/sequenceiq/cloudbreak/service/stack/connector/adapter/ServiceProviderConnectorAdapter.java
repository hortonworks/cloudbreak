package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Component
public class ServiceProviderConnectorAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderConnectorAdapter.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup) {
        LOGGER.debug("Assembling downscale stack event for stack: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .withTenantId(stack.getTenant().getId())
                .build();
        Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        List<CloudResource> resources = stack.getResources().stream()
                .map(r -> cloudResourceConverter.convert(r))
                .collect(Collectors.toList());
        List<CloudInstance> instances = new ArrayList<>();
        InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        DetailedEnvironmentResponse environment = environmentClientService.getByCrnAsInternal(stack.getEnvironmentCrn());
        for (InstanceMetaData metaData : group.getAllInstanceMetaData()) {
            if (instanceIds.contains(metaData.getInstanceId())) {
                CloudInstance cloudInstance = metadataConverter.convert(metaData, environment, stack.getStackAuthentication());
                instances.add(cloudInstance);
            }
        }
        CloudStack cloudStack = cloudStackConverter.convertForDownscale(stack, instanceIds);
        DownscaleStackRequest downscaleRequest = new DownscaleStackRequest(cloudContext,
                cloudCredential, cloudStack, resources, instances);
        LOGGER.debug("Triggering downscale stack event: {}", downscaleRequest);
        eventBus.notify(downscaleRequest.selector(), eventFactory.createEvent(downscaleRequest));
        try {
            DownscaleStackResult res = downscaleRequest.await();
            LOGGER.debug("Downscale stack result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.info("Failed to downscale the stack", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return instanceIds;
        } catch (InterruptedException e) {
            LOGGER.error("Error while downscaling the stack", e);
            throw new OperationException(e);
        }
    }

    public void deleteStack(Stack stack) {
        LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .withTenantId(stack.getTenant().getId())
                .build();
        Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        List<CloudResource> resources = stack.getResources().stream()
                .map(r -> cloudResourceConverter.convert(r))
                .collect(Collectors.toList());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        TerminateStackRequest<TerminateStackResult> terminateRequest = new TerminateStackRequest<>(cloudContext, cloudStack, cloudCredential, resources);
        LOGGER.debug("Triggering terminate stack event: {}", terminateRequest);
        eventBus.notify(terminateRequest.selector(), eventFactory.createEvent(terminateRequest));
        try {
            TerminateStackResult res = terminateRequest.await();
            LOGGER.debug("Terminate stack result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                if (res.getErrorDetails() != null) {
                    LOGGER.info("Failed to terminate the stack", res.getErrorDetails());
                    throw new OperationException(res.getErrorDetails());
                }
                throw new OperationException(format("Failed to terminate the stack: %s due to %s", cloudContext, res.getStatusReason()));
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while terminating the stack", e);
            throw new OperationException(e);
        }
    }

    public void rollback(Stack stack, Set<Resource> resourceSet) {
        LOGGER.debug("Rollback the whole stack for {}", stack.getId());
        deleteStack(stack);
    }

    public GetPlatformTemplateRequest triggerGetTemplate(Stack stack) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .build();
        Credential credential = ThreadBasedUserCrnProvider
                .doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn()));
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        GetPlatformTemplateRequest getPlatformTemplateRequest = new GetPlatformTemplateRequest(cloudContext, cloudCredential);
        eventBus.notify(getPlatformTemplateRequest.selector(), eventFactory.createEvent(getPlatformTemplateRequest));
        return getPlatformTemplateRequest;
    }

    public String waitGetTemplate(GetPlatformTemplateRequest getPlatformTemplateRequest) {
        try {
            GetPlatformTemplateResult res = getPlatformTemplateRequest.await();
            LOGGER.debug("Get template result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get template", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getTemplate();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting template: " + getPlatformTemplateRequest.getCloudContext(), e);
            throw new OperationException(e);
        }
    }

    public PlatformParameters getPlatformParameters(Stack stack, String userCrn) {
        return ThreadBasedUserCrnProvider.doAs(userCrn, () -> getPlatformParameters(stack));
    }

    public PlatformParameters getPlatformParameters(Stack stack) {
        LOGGER.debug("Get platform parameters for: {}", stack);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .build();
        Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        PlatformParameterRequest parameterRequest = new PlatformParameterRequest(cloudContext, cloudCredential);
        eventBus.notify(parameterRequest.selector(), eventFactory.createEvent(parameterRequest));
        try {
            PlatformParameterResult res = parameterRequest.await();
            LOGGER.debug("Platform parameter result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform parameters", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformParameters();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting platform parameters: " + cloudContext, e);
            throw new OperationException(e);
        }
    }

    public Variant checkAndGetPlatformVariant(Stack stack) {
        LOGGER.debug("Get platform variant for: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        Credential credential = ThreadBasedUserCrnProvider
                .doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(Crn.safeFromString(credential.getCrn()).getAccountId())
                .withGovCloud(credential.isGovCloud())
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        CheckPlatformVariantRequest checkPlatformVariantRequest = new CheckPlatformVariantRequest(cloudContext, cloudCredential);
        eventBus.notify(checkPlatformVariantRequest.selector(), eventFactory.createEvent(checkPlatformVariantRequest));
        try {
            CheckPlatformVariantResult res = checkPlatformVariantRequest.await();
            LOGGER.debug("Platform variant result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform variant", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getDefaultPlatformVariant();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform variant: " + cloudContext, e);
            throw new OperationException(e);
        }
    }

}
