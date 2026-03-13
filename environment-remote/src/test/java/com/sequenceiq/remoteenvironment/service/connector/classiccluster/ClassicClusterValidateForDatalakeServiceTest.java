package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.ValidateForDatalakeResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.ValidateForDatalakeValidationResponse;

@ExtendWith(MockitoExtension.class)
class ClassicClusterValidateForDatalakeServiceTest {

    @Mock
    private RemoteClusterServiceClient remoteClusterServiceClient;

    @InjectMocks
    private ClassicClusterValidateForDatalakeService underTest;

    @Test
    void validateDatalake() {
        OnPremisesApiProto.ValidateClusterForDatalakeResponse response = OnPremisesApiProto.ValidateClusterForDatalakeResponse.newBuilder()
                .setIsValidForDatalake(false)
                .addValidations(OnPremisesApiProto.DatalakeValidation.newBuilder()
                        .setType(OnPremisesApiProto.DatalakeValidationType.Value.KERBERIZED)
                        .setPassed(true)
                        .setMessage(""))
                .addValidations(OnPremisesApiProto.DatalakeValidation.newBuilder()
                        .setType(OnPremisesApiProto.DatalakeValidationType.Value.NOT_COMPUTE_CLUSTER)
                        .setPassed(false)
                        .setMessage("compute")
                )
                .build();
        when(remoteClusterServiceClient.validateClusterForDatalake("usercrn", "crn")).thenReturn(response);

        ValidateForDatalakeResponse result = underTest.validateForDatalake("usercrn", "crn");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getValidations()).hasSize(2);
        assertThat(result.getValidations().get(0))
                .returns("KERBERIZED", ValidateForDatalakeValidationResponse::getValidationType)
                .returns(true, ValidateForDatalakeValidationResponse::isPassed)
                .returns("", ValidateForDatalakeValidationResponse::getMessage);
        assertThat(result.getValidations().get(1))
                .returns("NOT_COMPUTE_CLUSTER", ValidateForDatalakeValidationResponse::getValidationType)
                .returns(false, ValidateForDatalakeValidationResponse::isPassed)
                .returns("compute", ValidateForDatalakeValidationResponse::getMessage);
    }

}
