package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.ValidateForDatalakeResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.ValidateForDatalakeValidationResponse;

@Component
public class ClassicClusterValidateForDatalakeService {

    @Inject
    private RemoteClusterServiceClient remoteClusterServiceClient;

    public ValidateForDatalakeResponse validateForDatalake(String userCrn, String environmentCrn) {
        OnPremisesApiProto.ValidateClusterForDatalakeResponse validateClusterForDatalakeResponse =
                remoteClusterServiceClient.validateClusterForDatalake(userCrn, environmentCrn);
        ValidateForDatalakeResponse response = new ValidateForDatalakeResponse();
        response.setCrn(environmentCrn);
        response.setValid(validateClusterForDatalakeResponse.getIsValidForDatalake());
        response.setValidations(validateClusterForDatalakeResponse.getValidationsList().stream()
                .map(this::getValidateForDatalakeValidationResponse)
                .toList());
        return response;
    }

    private ValidateForDatalakeValidationResponse getValidateForDatalakeValidationResponse(OnPremisesApiProto.DatalakeValidation datalakeValidation) {
        ValidateForDatalakeValidationResponse validation = new ValidateForDatalakeValidationResponse();
        validation.setValidationType(datalakeValidation.getType().name());
        validation.setPassed(datalakeValidation.getPassed());
        validation.setMessage(datalakeValidation.getMessage());
        return validation;
    }
}
