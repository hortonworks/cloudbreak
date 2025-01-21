package com.sequenceiq.cloudbreak.cloud.aws.authentication;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_SSH_KEY_RESOURCE_BUILDER_ORDER;
import static org.slf4j.LoggerFactory.getLogger;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;
import com.sequenceiq.cloudbreak.cloud.template.init.SshKeyNameGenerator;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsSshKeyResourceBuilder extends AbstractAwsAuthenticationBuilder {

    private static final Logger LOGGER = getLogger(AwsSshKeyResourceBuilder.class);

    @Inject
    private SshKeyNameGenerator sshKeyNameGenerator;

    @Inject
    private AwsPublicKeyConnector awsPublicKeyConnector;

    @Override
    public CloudResource create(AwsContext context, AuthenticatedContext auth, CloudStack stack) {
        String availabilityZone = context.getLocation().getAvailabilityZone().value();
        CloudResource ret = null;
        if (sshKeyNameGenerator.mustUploadPublicKey(stack)) {
            ret = createNamedResource(resourceType(), sshKeyNameGenerator.getKeyPairName(auth, stack), availabilityZone);
        }
        return ret;
    }

    @Override
    public CloudResource build(AwsContext context, AuthenticatedContext auth, CloudStack stack, CloudResource resource) throws Exception {
        String keyPairName = sshKeyNameGenerator.getKeyPairName(auth, stack);
        PublicKeyRegisterRequest publicKeyRegisterRequest = PublicKeyRegisterRequest.builder()
                .withCloudPlatform(auth.getCloudContext().getPlatform().getValue())
                .withCredential(auth.getCloudCredential())
                .withPublicKeyId(keyPairName)
                .withPublicKey(stack.getInstanceAuthentication().getPublicKey())
                .withRegion(auth.getCloudContext().getLocation().getRegion().getRegionName())
                .build();
        awsPublicKeyConnector.register(publicKeyRegisterRequest);
        LOGGER.debug("Ssh key group successfully created with id: {}", keyPairName);
        return CloudResource.builder()
                .cloudResource(resource)
                .withReference(keyPairName)
                .build();
    }

    @Override
    public CloudResourceStatus update(AwsContext context, AuthenticatedContext auth, CloudStack stack, CloudResource resource) {
        return null;
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource, CloudStack stack) throws Exception {
        PublicKeyUnregisterRequest publicKeyUnRegisterRequest = PublicKeyUnregisterRequest.builder()
                .withCloudPlatform(auth.getCloudContext().getPlatform().getValue())
                .withCredential(auth.getCloudCredential())
                .withPublicKeyId(resource.getReference())
                .withRegion(auth.getCloudContext().getLocation().getRegion().getRegionName())
                .build();
        awsPublicKeyConnector.unregister(publicKeyUnRegisterRequest);
        return resource;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_SSH_KEY;
    }

    @Override
    public int order() {
        return NATIVE_SSH_KEY_RESOURCE_BUILDER_ORDER;
    }
}
