package com.sequenceiq.cloudbreak.service.encryption;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class AwsCloudInformationDecorator implements CloudInformationDecorator {
    @Override
    public List<String> getLoggerInstances(DetailedEnvironmentResponse environment, Stack stack) {
        if (environment.getTelemetry() != null && environment.getTelemetry().getLogging() != null
                && environment.getTelemetry().getLogging().getS3() != null
                && StringUtils.isNotBlank(environment.getTelemetry().getLogging().getS3().getInstanceProfile())) {
            return List.of(environment.getTelemetry().getLogging().getS3().getInstanceProfile());
        }
        return List.of();
    }

    @Override
    public List<String> getCloudIdentities(DetailedEnvironmentResponse environment, Stack stack) {
        if (stack.getCluster() != null && stack.getCluster().getFileSystem() != null &&
                stack.getCluster().getFileSystem().getCloudStorage() != null
                && stack.getCluster().getFileSystem().getCloudStorage().getCloudIdentities() != null) {
            return stack.getCluster().getFileSystem().getCloudStorage().getCloudIdentities().stream()
                    .filter(cloudIdentity -> cloudIdentity.getFileSystemType().isS3())
                    .map(cloudIdentity -> cloudIdentity.getS3Identity().getInstanceProfile())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public Optional<String> getCredentialPrincipal(DetailedEnvironmentResponse environment, Stack stack) {
        if (environment.getCredential() != null && environment.getCredential().getAws() != null
                && environment.getCredential().getAws().getRoleBased() != null) {
            return Optional.ofNullable(environment.getCredential().getAws().getRoleBased().getRoleArn());
        }
        return Optional.empty();
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }
}
