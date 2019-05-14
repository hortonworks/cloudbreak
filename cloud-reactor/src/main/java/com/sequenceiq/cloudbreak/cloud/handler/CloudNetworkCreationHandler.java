package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.CloudNetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.CloudNetworkCreationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class CloudNetworkCreationHandler implements CloudPlatformEventHandler<CloudNetworkCreationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudNetworkCreationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<CloudNetworkCreationRequest> type() {
        return CloudNetworkCreationRequest.class;
    }

    @Override
    public void accept(Event<CloudNetworkCreationRequest> createCloudNetworkRequest) {
        LOGGER.debug("Received event: {}", createCloudNetworkRequest);
        CloudNetworkCreationRequest request = createCloudNetworkRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CreatedCloudNetwork createdCloudNetwork = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .networkConnector()
                    .createNetworkWithSubnets(createNetworkRequest(request));
            CloudNetworkCreationResult cloudNetworkCreationResult = new CloudNetworkCreationResult(request, createdCloudNetwork);
            request.getResult().onNext(cloudNetworkCreationResult);
            LOGGER.debug("Create network finished.");
        } catch (Exception e) {
            request.getResult().onNext(new CloudNetworkCreationResult(e.getMessage(), e, request));
        }
    }

    private NetworkCreationRequest createNetworkRequest(CloudNetworkCreationRequest request) {
        NetworkCreationRequest.Builder builder = new NetworkCreationRequest.Builder()
                .withEnvName(request.getEnvName())
                .withCloudCredential(request.getExtendedCloudCredential())
                .withVariant(request.getVariant())
                .withRegion(Region.region(request.getRegion()))
                .withNetworkCidr(request.getNetworkCidr())
                .withSubnetCidrs(request.getSubnetCidrs());
        Optional.ofNullable(request.isNoPublicIp()).ifPresent(builder::withNoPublicIp);
        Optional.ofNullable(request.isNoFirewallRules()).ifPresent(builder::withNoFirewallRules);
        return builder.build();
    }
}
