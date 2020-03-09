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
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.v1.cli.EnvironmentDtoToCliRequestConverter;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.AzureRegistrationTypeResolver;

@Component
public class EnvironmentDtoToCreateAzureEnvironmentRequestConverter implements EnvironmentDtoToCliRequestConverter {

    private final AzureRegistrationTypeResolver azureRegistrationTypeResolver;

    public EnvironmentDtoToCreateAzureEnvironmentRequestConverter(AzureRegistrationTypeResolver azureRegistrationTypeResolver) {
        this.azureRegistrationTypeResolver = azureRegistrationTypeResolver;
    }

    @Override
    public CloudPlatform supportedPlatform() {
        return CloudPlatform.AZURE;
    }

    public CreateAzureEnvironmentRequest convert(EnvironmentDto source) {
        CreateAzureEnvironmentRequest environmentRequest = initConversionWithNetwork(source);
        environmentRequest.setCredentialName(source.getCredential().getName());
        environmentRequest.setDescription(source.getDescription());
        environmentRequest.setEnvironmentName(source.getName());
        environmentRequest.setLogStorage(environmentTelemetryToLogStorageRequest(source.getTelemetry()));
        environmentRequest.setRegion(source.getLocation().getName());
        environmentRequest.setSecurityAccess(environmentSecurityAccessToSecurityAccessRequest(source.getSecurityAccess()));
        environmentRequest.setPublicKey(source.getAuthentication().getPublicKey());
        return environmentRequest;
    }

    private CreateAzureEnvironmentRequest initConversionWithNetwork(EnvironmentDto source) {
        NetworkDto network = source.getNetwork();
        AzureParams azureNetwork = network.getAzure();
        CreateAzureEnvironmentRequest environmentRequest = new CreateAzureEnvironmentRequest();
        environmentRequest.setUsePublicIp(!azureNetwork.isNoPublicIp());
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

    private AzureLogStorageRequest environmentTelemetryToLogStorageRequest(EnvironmentTelemetry source) {
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
            SecurityAccessDto source) {

        SecurityAccessRequest securityAccessRequest = new SecurityAccessRequest();
        doIfNotNull(source, s -> {
            securityAccessRequest.setCidr(s.getCidr());
            securityAccessRequest.setDefaultSecurityGroupId(s.getDefaultSecurityGroupId());
            securityAccessRequest.setSecurityGroupIdForKnox(s.getSecurityGroupIdForKnox());
        });
        return securityAccessRequest;
    }

    private List<String> getSubnetIds(EnvironmentDto source) {
        return getIfNotNull(source.getNetwork(), net ->
                getIfNotNull(net.getSubnetIds(), List::copyOf));
    }
}
