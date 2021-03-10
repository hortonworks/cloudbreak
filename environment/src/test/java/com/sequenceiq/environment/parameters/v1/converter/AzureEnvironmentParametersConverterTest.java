package com.sequenceiq.environment.parameters.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dao.domain.AzureParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;

@ExtendWith(MockitoExtension.class)
public class AzureEnvironmentParametersConverterTest {

        private static final String ACCOUNT_ID = "accountId";

        private static final String ENV_NAME = "envName";

        private static final String KEY_URL = "dummy-key-url";

        private static final EnvironmentView ENVIRONMENT_VIEW = new EnvironmentView();

        private static final long ID = 10L;

        @Mock
        private EnvironmentViewConverter environmentViewConverter;

        @InjectMocks
        private AzureEnvironmentParametersConverter underTest;

        @Test
        void createInstance() {
            assertEquals(AzureParameters.class, underTest.createInstance().getClass());
        }

        @Test
        void getCloudPlatform() {
            assertEquals(CloudPlatform.AZURE, underTest.getCloudPlatform());
        }

        @Test
        void convertTest() {
            when(environmentViewConverter.convert(any(Environment.class))).thenReturn(ENVIRONMENT_VIEW);
            ParametersDto parameters = ParametersDto.builder()
                    .withId(ID)
                    .withAzureParameters(AzureParametersDto.builder()
                            .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                    .withEncryptionKeyUrl(KEY_URL).build())
                            .build())
                    .build();
            Environment environment = new Environment();
            environment.setName(ENV_NAME);
            environment.setAccountId(ACCOUNT_ID);

            BaseParameters result = underTest.convert(environment, parameters);

            assertEquals(AzureParameters.class, result.getClass());
            AzureParameters azureResult = (AzureParameters) result;
            assertEquals(ENV_NAME, azureResult.getName());
            assertEquals(ACCOUNT_ID, azureResult.getAccountId());
            assertEquals(ENVIRONMENT_VIEW, azureResult.getEnvironment());
            assertEquals(ID, azureResult.getId());
            assertEquals(KEY_URL, azureResult.getEncryptionKeyUrl());
        }

        @Test
        void convertToDtoTest() {
            EnvironmentView environmentView = ENVIRONMENT_VIEW;
            AzureParameters parameters = new AzureParameters();
            parameters.setAccountId(ACCOUNT_ID);
            parameters.setEnvironment(environmentView);
            parameters.setId(ID);
            parameters.setName(ENV_NAME);
            parameters.setEncryptionKeyUrl(KEY_URL);

            ParametersDto result = underTest.convertToDto(parameters);

            assertEquals(ACCOUNT_ID, result.getAccountId());
            assertEquals(ID, result.getId());
            assertEquals(ENV_NAME, result.getName());
            assertEquals(KEY_URL, result.getAzureParametersDto().getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl());
        }
}
