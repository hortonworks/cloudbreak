package com.sequenceiq.environment.credential.v1.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.aws.AwsCredentialV1ParametersToAwsCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.azure.AzureCredentialAttributesToAzureCredentialResponseParametersConverter;
import com.sequenceiq.environment.credential.v1.converter.gcp.GcpCredentialV1ParametersToGcpCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.mock.MockCredentialV1ParametersToMockCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.yarn.YarnCredentialV1ParametersToAwsYarnAttributesConverter;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.credential.validation.definition.CredentialDefinitionService;

@ExtendWith(SpringExtension.class)
public class CredentialToCredentialV1ResponseConverterTest {

    private static final Credential CREDENTIAL = new Credential();

    private static final String CREDENTIAL_NAME = "testcredential";

    private static final String PLATFORM = "PLATFORM";

    @MockBean
    private CredentialValidator credentialValidator;

    @MockBean
    private CredentialDefinitionService credentialDefinitionService;

    @MockBean
    private AwsCredentialV1ParametersToAwsCredentialAttributesConverter awsConverter;

    @MockBean
    private AzureCredentialAttributesToAzureCredentialResponseParametersConverter azureConverter;

    @MockBean
    private GcpCredentialV1ParametersToGcpCredentialAttributesConverter gcpConverter;

    @MockBean
    private MockCredentialV1ParametersToMockCredentialAttributesConverter mockConverter;

    @MockBean
    private YarnCredentialV1ParametersToAwsYarnAttributesConverter yarnConverter;

    @MockBean
    private StringToSecretResponseConverter secretConverter;

    @Inject
    private CredentialToCredentialV1ResponseConverter converterUnderTest;

    @BeforeEach
    public void setupTestCredential() {
        CREDENTIAL.setName(CREDENTIAL_NAME);
        CREDENTIAL.setCloudPlatform(PLATFORM);
    }

    @Test
    public void testCredentialToCredentialResponseConverterHasCreated() {
        assertNotNull(converterUnderTest.convert(CREDENTIAL).getCreated());
    }

    @Configuration
    @Import(CredentialToCredentialV1ResponseConverter.class)
    static class Config {
    }
}
