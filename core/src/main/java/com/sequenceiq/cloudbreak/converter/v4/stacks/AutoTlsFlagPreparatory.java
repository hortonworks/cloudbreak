package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class AutoTlsFlagPreparatory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoTlsFlagPreparatory.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public boolean provideAutoTlsFlag(ClusterV4Request request, Stack stack, Optional<String> parentEnvironmentCloudPlatform) {
        String cloudPlatform = parentEnvironmentCloudPlatform.orElse(stack.getCloudPlatform());
        Boolean autoTlsFlag = Optional.ofNullable(request.getCm())
                .map(ClouderaManagerV4Request::getEnableAutoTls)
                .orElseGet(() -> {
                    CloudConnector connector = cloudPlatformConnectors.get(
                            Platform.platform(cloudPlatform), Variant.variant(stack.getPlatformVariant()));
                    PlatformParameters platformParameters = connector.parameters();
                    return platformParameters.isAutoTlsSupported();
                });
        LOGGER.info("AutoTLS flag [{}] for request {}, cloudplatform [{}]", autoTlsFlag, request, cloudPlatform);
        return autoTlsFlag;
    }
}
