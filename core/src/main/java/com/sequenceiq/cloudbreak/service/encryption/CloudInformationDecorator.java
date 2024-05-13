package com.sequenceiq.cloudbreak.service.encryption;

import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public interface CloudInformationDecorator {
    List<String> getLoggerInstances(DetailedEnvironmentResponse environment, Stack stack);

    List<String> getCloudIdentities(DetailedEnvironmentResponse environment, Stack stack);

    Optional<String> getCredentialPrincipal(DetailedEnvironmentResponse environment, Stack stack);

    Platform platform();

    Variant variant();

}
