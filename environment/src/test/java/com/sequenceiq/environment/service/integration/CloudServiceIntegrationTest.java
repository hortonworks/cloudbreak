package com.sequenceiq.environment.service.integration;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnClient;
import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;
import com.sequenceiq.environment.service.integration.testconfiguration.TestConfigurationWithCloudAccess;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestConfigurationWithCloudAccess.class)
@ActiveProfiles("test")
@Tag("outofscope")
@Disabled
public class CloudServiceIntegrationTest {

    public static final String SERVICE_ADDRESS = "http://localhost:%d/environmentservice";

    private static final String TEST_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @LocalServerPort
    private int port;

    @Inject
    private ProxyConfigRepository proxyConfigRepository;

    private EnvironmentServiceCrnClient client;

    @BeforeEach
    public void setup() {
        client = new EnvironmentServiceClientBuilder(String.format(SERVICE_ADDRESS, port))
                .withCertificateValidation(false)
                .withDebug(true)
                .withIgnorePreValidation(true)
                .build();
    }

    @Test
    void testCredentialList() {
        client.withCrn(TEST_CRN).credentialV1Endpoint().list();
    }

    @Test
    void testCredentialCreate() {
        CredentialRequest request = new CredentialRequest();
        AwsCredentialParameters aws = new AwsCredentialParameters();
        aws.setGovCloud(false);
        KeyBasedParameters keyBased = new KeyBasedParameters();
        keyBased.setAccessKey("f");
        keyBased.setSecretKey("s");
        aws.setKeyBased(keyBased);
        request.setAws(aws);
        request.setCloudPlatform("AWS");
        request.setName("testcredential");

        client.withCrn(TEST_CRN).credentialV1Endpoint().post(request);
    }

    @Test
    void testCredentialAzureInitCode() {
        CredentialRequest request = new CredentialRequest();
        request.setCloudPlatform("AZURE");
        request.setName("testcredential");

        client.withCrn(TEST_CRN).credentialV1Endpoint().initCodeGrantFlow(request);
    }
}
