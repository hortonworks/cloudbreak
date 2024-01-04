package com.sequenceiq.freeipa.service.rotation.executor;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsUpdateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class LaunchTemplateRotationExecutor extends AbstractRotationExecutor<RotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchTemplateRotationExecutor.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Override
    protected void rotate(RotationContext rotationContext) throws Exception {
        updateLaunchTemplateIfNeeded(rotationContext);
    }

    private void updateLaunchTemplateIfNeeded(RotationContext rotationContext) {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        CloudStack cloudStack = stackToCloudStackConverter.convert(stack);
        if (cloudStack.getTemplate() != null && cloudStack.getTemplate().contains(AwsUpdateService.LAUNCH_TEMPLATE)) {
            LOGGER.info("Updating aws launch template based on current user data.");
            List<CloudResource> cloudResources = getCloudFormationStackResources(stack);
            Map<InstanceGroupType, String> userData = Map.of(InstanceGroupType.GATEWAY, stack.getImage().getUserdataWrapper());
            CloudContext cloudContext = convertToCloudContext(stack);
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, cloudCredential);
            try {
                connector.resources().updateUserData(auth, cloudStack, cloudResources, userData);
                LOGGER.info("Updated launch template with new user data.");
            } catch (Exception e) {
                String msg = "Failed to update aws launch template.";
                LOGGER.info(msg, e);
                throw new SecretRotationException(msg, e);
            }
        } else {
            LOGGER.info("No template update is needed.");
        }
    }

    @Override
    protected void rollback(RotationContext rotationContext) throws Exception {
        updateLaunchTemplateIfNeeded(rotationContext);
    }

    @Override
    protected void finalize(RotationContext rotationContext) throws Exception {

    }

    @Override
    protected void preValidate(RotationContext rotationContext) throws Exception {

    }

    @Override
    protected void postValidate(RotationContext rotationContext) throws Exception {

    }

    @Override
    protected Class<RotationContext> getContextClass() {
        return RotationContext.class;
    }

    @Override
    public SecretRotationStep getType() {
        return FreeIpaSecretRotationStep.LAUNCH_TEMPLATE;
    }

    private List<CloudResource> getCloudFormationStackResources(Stack stack) {
        return resourceService.findAllByStackId(stack.getId())
                .stream()
                .map(resourceToCloudResourceConverter::convert)
                .collect(Collectors.toList());
    }

    private static CloudContext convertToCloudContext(Stack stack) {
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformvariant())
                .withLocation(location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone())))
                .withUserName(stack.getOwner())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .withAccountId(stack.getAccountId())
                .build();
    }
}
