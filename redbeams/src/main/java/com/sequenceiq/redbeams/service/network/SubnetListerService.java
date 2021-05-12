package com.sequenceiq.redbeams.service.network;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.service.CredentialService;

@Service
public class SubnetListerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetListerService.class);

    @Inject
    private CredentialService credentialService;

    public List<CloudSubnet> listSubnets(DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform) {
        EnvironmentNetworkResponse environmentNetworkResponse = environmentResponse.getNetwork();
        if (environmentNetworkResponse == null || environmentNetworkResponse.getSubnetMetas() == null) {
            throw new RedbeamsException("Environment does not contain metadata for subnets");
        }

        switch (cloudPlatform) {
            case AWS:
            case GCP:
            case MOCK:
                // IDs in metas are fine as is
                return new ArrayList<>(environmentNetworkResponse.getCbSubnets().values());
            case AZURE:
                // IDs in metas must be expanded to full resource IDs
                String subscriptionId = getAzureSubscriptionId(environmentResponse.getCrn());
                return new ArrayList<>(environmentNetworkResponse.getSubnetMetas().values().stream()
                        .map(meta -> expandAzureResourceId(meta, environmentResponse, subscriptionId))
                        .collect(Collectors.toList()));
            default:
                throw new RedbeamsException(String.format("Support for cloud platform %s not yet added", cloudPlatform.name()));
        }
    }

    String getAzureSubscriptionId(String environmentCrn) {
        Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
        LOGGER.info("Found credential {} for environment {}", credential.getName(), environmentCrn);
        if (credential.getAzure().isPresent()) {
            return credential.getAzure().get().getSubscriptionId();
        } else {
            throw new RedbeamsException(String.format("Retrieved credential %s for Azure environment %s which lacks subscription ID",
                    credential.getName(), environmentCrn));
        }
    }

    @VisibleForTesting
    CloudSubnet expandAzureResourceId(CloudSubnet meta, DetailedEnvironmentResponse environmentResponse, String subscriptionId) {

        StringBuilder expandedId = new StringBuilder("/subscriptions/");
        expandedId.append(subscriptionId);
        expandedId.append("/resourceGroups/").append(environmentResponse.getNetwork().getAzure().getResourceGroupName());
        expandedId.append("/providers/Microsoft.Network/virtualNetworks/").append(environmentResponse.getNetwork().getAzure().getNetworkId());
        expandedId.append("/subnets/").append(meta.getId());

        return meta.withId(expandedId.toString());
    }
}
