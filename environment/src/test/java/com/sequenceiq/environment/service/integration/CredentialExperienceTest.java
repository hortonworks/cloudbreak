package com.sequenceiq.environment.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.environment.service.integration.testconfiguration.TestConfigurationWithoutCloudAccess;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfigurationWithoutCloudAccess.class,
        properties = "info.app.version=test")
@ActiveProfiles("test")
public class CredentialExperienceTest {

    private static final String SERVICE_ADDRESS = "http://localhost:%d/environmentservice";

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    @LocalServerPort
    private int port;

    private EnvironmentServiceCrnEndpoints client;

    @BeforeEach
    public void setup() {
        client = new EnvironmentServiceClientBuilder(String.format(SERVICE_ADDRESS, port))
                .withCertificateValidation(false)
                .withDebug(true)
                .withIgnorePreValidation(true)
                .build()
                .withCrn(TEST_USER_CRN);
    }

    @Test
    public void test() {
        client.credentialV1Endpoint().getPrerequisitesForCloudPlatform("AWS", "addr");
    }
}
