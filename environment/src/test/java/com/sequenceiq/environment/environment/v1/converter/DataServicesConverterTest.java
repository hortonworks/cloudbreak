package com.sequenceiq.environment.environment.v1.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.environment.model.AwsDataServicesV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.AzureDataServicesV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.GcpDataServicesV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.request.DataServicesRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DataServicesResponse;
import com.sequenceiq.environment.environment.dto.dataservices.AwsDataServiceParameters;
import com.sequenceiq.environment.environment.dto.dataservices.AzureDataServiceParameters;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;
import com.sequenceiq.environment.environment.dto.dataservices.GcpDataServiceParameters;

class DataServicesConverterTest {

    private static final String IDENTITY = "identity";

    private final DataServicesConverter underTest = new DataServicesConverter();

    @Test
    void convertToDto() {
        DataServicesRequest request = new DataServicesRequest();
        AwsDataServicesV1Parameters aws = new AwsDataServicesV1Parameters();
        request.setAws(aws);
        AzureDataServicesV1Parameters azure = new AzureDataServicesV1Parameters();
        azure.setSharedManagedIdentity(IDENTITY);
        request.setAzure(azure);
        GcpDataServicesV1Parameters gcp = new GcpDataServicesV1Parameters();
        request.setGcp(gcp);
        EnvironmentDataServices result = underTest.convertToDto(request);

        assertThat(result.aws()).isNotNull();
        assertThat(result.azure()).isNotNull();
        assertThat(result.gcp()).isNotNull();
        assertThat(result.azure().sharedManagedIdentity()).isEqualTo(IDENTITY);
    }

    @Test
    void convertToDtoWithNullFields() {
        DataServicesRequest request = new DataServicesRequest();
        EnvironmentDataServices result = underTest.convertToDto(request);

        assertThat(result.aws()).isNull();
        assertThat(result.azure()).isNull();
        assertThat(result.gcp()).isNull();
    }

    @Test
    void convertToDtoNullNoNPE() {
        EnvironmentDataServices result = underTest.convertToDto(null);
        assertThat(result).isNull();
    }

    @Test
    void convertToResponse() {
        EnvironmentDataServices dataServices = EnvironmentDataServices.builder()
                .withAws(AwsDataServiceParameters.builder().build())
                .withAzure(AzureDataServiceParameters.builder().withSharedManagedIdentity(IDENTITY).build())
                .withGcp(GcpDataServiceParameters.builder().build())
                .build();

        DataServicesResponse result = underTest.convertToResponse(dataServices);
        assertThat(result.getAws()).isNotNull();
        assertThat(result.getAzure()).isNotNull();
        assertThat(result.getGcp()).isNotNull();
        assertThat(result.getAzure().getSharedManagedIdentity()).isEqualTo(IDENTITY);
    }

    @Test
    void convertToResponseWithNullFields() {
        EnvironmentDataServices dataServices = EnvironmentDataServices.builder().build();
        DataServicesResponse result = underTest.convertToResponse(dataServices);
        assertThat(result.getAws()).isNull();
        assertThat(result.getAzure()).isNull();
        assertThat(result.getGcp()).isNull();
    }

    @Test
    void convertToResponseNoNPE() {
        DataServicesResponse result = underTest.convertToResponse(null);
        assertThat(result).isNull();
    }
}
