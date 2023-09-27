package com.sequenceiq.redbeams.service.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.redbeams.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.service.CredentialService;

@Service
public class SubnetListerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetListerService.class);

    private static final String SUBNET_RESOURCE_ID_PATTERN = "^/subscriptions/[a-zA-Z0-9-]+/resourceGroups/[a-zA-Z0-9-_]+" +
            "/providers/Microsoft\\.Network/virtualNetworks/[a-zA-Z0-9-_]+/subnets/[a-zA-Z0-9-_]+$";

    @Inject
    private CredentialService credentialService;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

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

    public String getAzureSubscriptionId(String environmentCrn) {
        Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
        LOGGER.info("Found credential {} for environment {}", credential.getName(), environmentCrn);
        if (credential.getAzure().isPresent()) {
            return credential.getAzure().get().getSubscriptionId();
        } else {
            throw new RedbeamsException(String.format("Retrieved credential %s for Azure environment %s which lacks subscription ID",
                    credential.getName(), environmentCrn));
        }
    }

    public CloudSubnet expandAzureResourceId(CloudSubnet meta, DetailedEnvironmentResponse environmentResponse, String subscriptionId) {
        String expandedSubnetID = expandAzureResourceId(meta.getId(), environmentResponse, subscriptionId);
        return meta.withId(expandedSubnetID);
    }

    public String expandAzureResourceId(String subnetID, DetailedEnvironmentResponse environmentResponse, String subscriptionId) {
        if (isValidAzureSubnetResourceId(subnetID)) {
            LOGGER.debug("Subnet id: {} is a valid Azure subnet resource id.", subnetID);
            return subnetID;
        } else {
            StringBuilder expandedId = new StringBuilder("/subscriptions/");
            expandedId.append(subscriptionId);
            expandedId.append("/resourceGroups/").append(environmentResponse.getNetwork().getAzure().getResourceGroupName());
            expandedId.append("/providers/Microsoft.Network/virtualNetworks/").append(environmentResponse.getNetwork().getAzure().getNetworkId());
            expandedId.append("/subnets/").append(subnetID);

            LOGGER.debug("Subnet id: {} expanded to Azure subnet resource id: {}", subnetID, expandedId);
            return expandedId.toString();
        }
    }

    public Set<CloudSubnet> fetchNetworksFiltered(DBStack dbStack, Collection<String> subnetIds) {
        Credential credential = credentialService.getCredentialByEnvCrn(dbStack.getEnvironmentId());
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential, dbStack.getCloudPlatform());
        CloudNetworks cloudNetworks = cloudParameterService.getCloudNetworks(cloudCredential, dbStack.getRegion(), dbStack.getPlatformVariant(),
                Map.of(NetworkConstants.SUBNET_IDS, String.join(",", subnetIds)));
        LOGGER.debug("Received networks after applying filter {}: {}", subnetIds, cloudNetworks);
        Set<CloudSubnet> cloudSubnets = cloudNetworks.getCloudNetworkResponses().values().stream()
                .flatMap(Collection::stream)
                .map(CloudNetwork::getSubnetsMeta)
                .flatMap(Collection::stream)
                .filter(subnet -> subnetIds.contains(subnet.getId()))
                .collect(Collectors.toSet());
        LOGGER.debug("Subnets: {}", cloudSubnets);
        return cloudSubnets;
    }

    private boolean isValidAzureSubnetResourceId(String input) {
        Pattern pattern = Pattern.compile(SUBNET_RESOURCE_ID_PATTERN);
        Matcher matcher = pattern.matcher(input);
        boolean result = matcher.matches();
        LOGGER.debug("Checking subnet id {}, whether it conforms with the Azure resource ID pattern: {}. Pattern matching result: {}",
                input, SUBNET_RESOURCE_ID_PATTERN, result);
        return result;
    }
}
