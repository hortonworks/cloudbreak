package com.sequenceiq.environment.environment.v1.cli.azure;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.environments.model.AzureLogStorageRequest;
import com.cloudera.cdp.environments.model.CreateAzureEnvironmentRequest;
import com.cloudera.cdp.environments.model.CreateAzureEnvironmentRequestNewNetworkParams;
import com.cloudera.cdp.environments.model.ExistingAzureNetworkRequest;
import com.cloudera.cdp.environments.model.SecurityAccessRequest;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.environment.v1.cli.EnvironmentRequestToCliRequestConverter;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.v1.AzureRegistrationTypeResolver;

@Component
public class EnvironmentRequestToCreateAzureEnvironmentRequestConverter implements EnvironmentRequestToCliRequestConverter {

    private final AzureRegistrationTypeResolver azureRegistrationTypeResolver;

    public EnvironmentRequestToCreateAzureEnvironmentRequestConverter(AzureRegistrationTypeResolver azureRegistrationTypeResolver) {
        this.azureRegistrationTypeResolver = azureRegistrationTypeResolver;
    }

    @Override
    public CloudPlatform supportedPlatform() {
        return CloudPlatform.AZURE;
    }

    public CreateAzureEnvironmentRequest convert(EnvironmentRequest source) {
        CreateAzureEnvironmentRequest environmentRequest = initConversionWithNetwork(source);
        environmentRequest.setCredentialName(source.getCredentialName());
        environmentRequest.setDescription(source.getDescription());
        environmentRequest.setEnvironmentName(source.getName());
        environmentRequest.setLogStorage(environmentTelemetryToLogStorageRequest(source.getTelemetry()));
        environmentRequest.setRegion(source.getLocation().getName());
        environmentRequest.setSecurityAccess(environmentSecurityAccessToSecurityAccessRequest(source.getSecurityAccess()));
        environmentRequest.setPublicKey(source.getAuthentication().getPublicKey());
        return environmentRequest;
    }

    private CreateAzureEnvironmentRequest initConversionWithNetwork(EnvironmentRequest source) {
        EnvironmentNetworkRequest network = getNetwork(source);
        EnvironmentNetworkAzureParams azureNetwork = getAzureNetwork(network);
        CreateAzureEnvironmentRequest environmentRequest = new CreateAzureEnvironmentRequest();
        environmentRequest.setUsePublicIp(!azureNetwork.getNoPublicIp());
        if (azureRegistrationTypeResolver.getRegistrationType(network) == RegistrationType.CREATE_NEW) {
            var networkParams = new CreateAzureEnvironmentRequestNewNetworkParams();
            networkParams.setNetworkCidr(network.getNetworkCidr());
            environmentRequest.setNewNetworkParams(networkParams);
        } else {
            var networkParams = new ExistingAzureNetworkRequest();
            networkParams.setNetworkId(azureNetwork.getNetworkId());
            networkParams.setResourceGroupName(azureNetwork.getResourceGroupName());
            networkParams.setSubnetIds(getSubnetIds(source));
            environmentRequest.setExistingNetworkParams(networkParams);
        }
        return environmentRequest;
    }

    private EnvironmentNetworkRequest getNetwork(EnvironmentRequest source) {
        EnvironmentNetworkRequest network = source.getNetwork();
        if (network == null) {
            throw new IllegalArgumentException("Network is missing (null) from the EnvironmentRequest");
        }
        return network;
    }

    private EnvironmentNetworkAzureParams getAzureNetwork(EnvironmentNetworkRequest network) {
        EnvironmentNetworkAzureParams azureNetwork = network.getAzure();
        if (azureNetwork == null) {
            throw new IllegalArgumentException("EnvironmentNetworkAzureParams is missing (null) from the EnvironmentNetworkRequest");
        }
        return azureNetwork;
    }

    private AzureLogStorageRequest environmentTelemetryToLogStorageRequest(TelemetryRequest source) {
        AzureLogStorageRequest azureLogStorageRequest = new AzureLogStorageRequest();
        doIfNotNull(source, s -> {
            doIfNotNull(s.getLogging(), logging -> {
                doIfNotNull(logging.getAdlsGen2(), adls -> azureLogStorageRequest.setManagedIdentity(adls.getManagedIdentity()));
                azureLogStorageRequest.setStorageLocationBase(logging.getStorageLocation());
            });
        });
        return azureLogStorageRequest;
    }

    private SecurityAccessRequest environmentSecurityAccessToSecurityAccessRequest(
            com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest source) {

        SecurityAccessRequest securityAccessRequest = new SecurityAccessRequest();
        doIfNotNull(source, s -> {
            securityAccessRequest.setCidr(s.getCidr());
            securityAccessRequest.setDefaultSecurityGroupId(s.getDefaultSecurityGroupId());
            securityAccessRequest.setSecurityGroupIdForKnox(s.getSecurityGroupIdForKnox());
        });
        return securityAccessRequest;
    }

    private List<String> getSubnetIds(EnvironmentRequest source) {
        return getIfNotNull(source.getNetwork(), net ->
                getIfNotNull(net.getSubnetIds(), List::copyOf));
    }
}
