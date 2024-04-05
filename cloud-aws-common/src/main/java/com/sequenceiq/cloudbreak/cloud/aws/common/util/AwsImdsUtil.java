package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static com.sequenceiq.cloudbreak.cloud.UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL;
import static com.sequenceiq.cloudbreak.cloud.UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED;
import static com.sequenceiq.cloudbreak.constant.ImdsConstants.AWS_IMDS_VERSION_V2;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.services.ec2.model.HttpTokensState;

@Component
public class AwsImdsUtil {

    public static final Set<UpdateType> APPLICABLE_UPDATE_TYPES = Set.of(INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL, INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED);

    @Inject
    private EntitlementService entitlementService;

    public static HttpTokensState getHttpTokensStateByUpdateType(UpdateType updateType) {
        return switch (updateType) {
            case INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL -> HttpTokensState.OPTIONAL;
            case INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED -> HttpTokensState.REQUIRED;
            default -> throw new CloudbreakServiceException("Unsupported update type for instance metadata update!");
        };
    }

    public void validateInstanceMetadataUpdate(UpdateType updateType, CloudStack stack, AuthenticatedContext authenticatedContext) {
        if (INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED.equals(updateType)) {
            String accountId = authenticatedContext.getCloudContext().getAccountId();
            if (!entitlementService.isAwsImdsV2Enforced(accountId)) {
                throw new CloudbreakServiceException(String.format("Instance metadata update is not supported for this tenant (%s)!", accountId));
            }
            String imageId = stack.getImage().getImageId();
            Map<String, String> packageVersions = stack.getImage().getPackageVersions();
            if (!packageVersions.containsKey(ImagePackageVersion.IMDS_VERSION.getKey()) ||
                    !StringUtils.equals(packageVersions.get(ImagePackageVersion.IMDS_VERSION.getKey()), AWS_IMDS_VERSION_V2)) {
                throw new CloudbreakServiceException(String.format("Instance metadata update is not supported for this image: %s!", imageId));
            }
        }
    }
}
