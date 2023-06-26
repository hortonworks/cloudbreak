package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.USER_DATA;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
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
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.secret.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataSecretModifier;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class UserDataRotationExecutor extends AbstractRotationExecutor<UserDataRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataRotationExecutor.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private SecretService secretService;

    @Inject
    private UserDataService userDataService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Override
    public void rotate(UserDataRotationContext rotationContext) {
        LOGGER.info("Userdata rotation is requested {}", rotationContext);
        modifyUserData(rotationContext, useNewSecret());
        updateLaunchTemplateIfNeeded(rotationContext.getResourceCrn());
        LOGGER.info("Userdata rotation is completed.");
    }

    @Override
    public void rollback(UserDataRotationContext rotationContext) {
        LOGGER.info("Userdata rollback is requested {}", rotationContext);
        modifyUserData(rotationContext, useOldSecret());
        updateLaunchTemplateIfNeeded(rotationContext.getResourceCrn());
        LOGGER.info("Userdata rollback is completed.");
    }

    @Override
    public void finalize(UserDataRotationContext rotationContext) {

    }

    @Override
    public void preValidate(UserDataRotationContext rotationContext) throws Exception {

    }

    @Override
    public void postValidate(UserDataRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return USER_DATA;
    }

    @Override
    public Class<UserDataRotationContext> getContextClass() {
        return UserDataRotationContext.class;
    }

    private void modifyUserData(UserDataRotationContext context, Function<RotationSecret, String> secretSelector) {
        Map<String, RotationSecret> secrets = getSecrets(context.getSecretModifierMap().stream().map(Pair::getValue).collect(Collectors.toList()));
        StackDto stack = stackService.getByCrn(context.getResourceCrn());
        Map<InstanceGroupType, String> userData = userDataService.getUserData(stack.getId())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> modifyUserData(e.getValue(), secrets, context, secretSelector)));
        userDataService.createOrUpdateUserData(stack.getId(), userData);
    }

    private Function<RotationSecret, String> useNewSecret() {
        return RotationSecret::getSecret;
    }

    private Function<RotationSecret, String> useOldSecret() {
        return RotationSecret::getBackupSecret;
    }

    private Map<String, RotationSecret> getSecrets(List<String> values) {
        return values
                .stream()
                .collect(Collectors.toMap(vaultPath -> vaultPath, vaultPath -> {
                    RotationSecret rotationSecret = secretService.getRotation(vaultPath);
                    if (!rotationSecret.isRotation()) {
                        LOGGER.error("Secret {} is not in a rotated state. User data modification failed.", vaultPath);
                        throw new SecretRotationException("Secret is not in a rotated state. User data modification failed.", getType());
                    }
                    return rotationSecret;
                }));
    }

    private String modifyUserData(
            String userData,
            Map<String, RotationSecret> secrets,
            UserDataRotationContext context,
            Function<RotationSecret, String> secretSelector) {
        UserDataReplacer userDataReplacer = new UserDataReplacer(userData);
        context.getSecretModifierMap()
                .forEach(pair -> {
                    UserDataSecretModifier modifier = pair.getKey();
                    String vaultPath = pair.getValue();
                    modifier.modify(userDataReplacer, secretSelector.apply(secrets.get(vaultPath)));
                });
        return userDataReplacer.getUserData();
    }

    private void updateLaunchTemplateIfNeeded(String resourceCrn) {
        StackDto stack = stackService.getByCrn(resourceCrn);
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        if (cloudStack.getTemplate() != null && cloudStack.getTemplate().contains(AwsUpdateService.LAUNCH_TEMPLATE)) {
            LOGGER.info("Updating aws launch template based on current user data.");
            List<CloudResource> cloudResources = getCloudFormationStackResources(stack);
            Map<InstanceGroupType, String> userData = userDataService.getUserData(stack.getId());
            CloudContext cloudContext = convertToCloudContext(stack);
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, cloudCredential);
            try {
                connector.resources().updateUserData(auth, cloudStack, cloudResources, userData);
                LOGGER.info("Updated launch template with new user data.");
            } catch (Exception e) {
                String msg = "Failed to update aws launch template.";
                LOGGER.info(msg, e);
                throw new SecretRotationException(msg, e, getType());
            }
        } else {
            LOGGER.info("No template update is needed.");
        }
    }

    private static CloudContext convertToCloudContext(StackDto stack) {
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone())))
                .withUserName(stack.getCreator().getUserName())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .withTenantId(stack.getWorkspace().getId())
                .build();
    }

    private List<CloudResource> getCloudFormationStackResources(StackDto stack) {
        return resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED, ResourceType.CLOUDFORMATION_STACK, stack.getId())
                .stream()
                .map(resourceToCloudResourceConverter::convert)
                .collect(Collectors.toList());
    }
}
