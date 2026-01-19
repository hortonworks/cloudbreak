package com.sequenceiq.it.cloudbreak.action.v4.environment;

import java.util.Optional;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentAddUserManagedIdentityAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Optional<AzureResourceEncryptionParameters> azureResourceEncryptionParameters = createAzureResourceEncryptionParameters(testDto);
        if (azureResourceEncryptionParameters.isPresent()) {
            AzureEnvironmentParameters azureEnvironmentParameters = createAzureEnvironmentParameters(azureResourceEncryptionParameters.get(), testDto);
            EnvironmentEditRequest request = new EnvironmentEditRequest();
            request.setAzure(azureEnvironmentParameters);
            DetailedEnvironmentResponse response = environmentClient.getDefaultClient(testContext)
                    .environmentV1Endpoint()
                    .editByCrn(testDto.getResponse().getCrn(), request);
            testDto.setResponse(response);
        }
        return testDto;
    }

    private AzureEnvironmentParameters createAzureEnvironmentParameters(AzureResourceEncryptionParameters params, EnvironmentTestDto testDto) {
        AzureEnvironmentParameters azureEnvironmentParameters = new AzureEnvironmentParameters();
        azureEnvironmentParameters.setResourceEncryptionParameters(params);
        azureEnvironmentParameters.setResourceGroup(testDto.getAzure().getResourceGroup());
        return azureEnvironmentParameters;
    }

    private Optional<AzureResourceEncryptionParameters> createAzureResourceEncryptionParameters(EnvironmentTestDto testDto) {
        return Optional.ofNullable(testDto.getAzure())
                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                .map(encryptionParameters ->
                        AzureResourceEncryptionParameters.builder()
                                .withUserManagedIdentity(encryptionParameters.getUserManagedIdentity())
                                .withEncryptionKeyUrl(encryptionParameters.getEncryptionKeyUrl())
                                .withEncryptionKeyResourceGroupName(encryptionParameters.getEncryptionKeyResourceGroupName())
                                .build());
    }

}