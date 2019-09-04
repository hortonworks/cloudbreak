package com.sequenceiq.environment.service.integration;

import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.getProxyConfig;
import static com.sequenceiq.environment.proxy.v1.ProxyTestSource.getProxyRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.sequenceiq.authorization.service.UmsAuthorizationService;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;
import com.sequenceiq.environment.api.v1.credential.model.response.InteractiveCredentialResponse;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponses;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.service.RequestProvider;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;
import com.sequenceiq.environment.service.integration.testconfiguration.TestConfigurationForServiceIntegration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestConfigurationForServiceIntegration.class)
@ActiveProfiles("test")
public class EnvironmentServiceIntegrationTest {

    private static final String SERVICE_ADDRESS = "http://localhost:%d/environmentservice";

    private static final String DEFINITION_AWS = "{\"values\":[{\"name\":\"smartSenseId\",\"type\":\"String\",\"sensitive\":false,\"optional\":true},"
            + "{\"name\":\"govCloud\",\"type\":\"String\",\"sensitive\":false,\"optional\":true}],\"selectors\":[{\"name\":\"role-based\",\"values\":"
            + "[{\"name\":\"roleArn\",\"type\":\"String\"},{\"name\":\"externalId\",\"type\":\"String\",\"optional\":true}]},"
            + "{\"name\":\"key-based\",\"values\":[{\"name\":\"accessKey\",\"type\":\"String\"},{\"name\":\"secretKey\",\"type\":\"String\"}]}]}";

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String USER_CODE = "1234";

    private static final String VERIFICATION_URL = "http://cloudera.com";

    private EnvironmentServiceCrnEndpoints client;

    @LocalServerPort
    private int port;

    @Mock
    private ResourceDefinitionRequest resourceDefinitionRequest;

    @Mock
    private InteractiveLoginRequest interactiveLoginRequest;

    @Mock
    private InitCodeGrantFlowRequest initCodeGrantFlowRequest;

    @MockBean
    private RequestProvider requestProvider;

    @MockBean
    private UmsAuthorizationService umsAuthorizationService;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private ProxyConfigRepository proxyConfigRepository;

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private SecretService secretService;

    private CredentialRequest credentialRequest;

    private Credential credential;

    @BeforeEach
    public void setup() {
        client = new EnvironmentServiceClientBuilder(String.format(SERVICE_ADDRESS, port))
                .withCertificateValidation(false)
                .withDebug(true)
                .withIgnorePreValidation(true)
                .build()
                .withCrn(TEST_CRN);

        credential = new Credential();
        credential.setName("credential_test");
        credential.setResourceCrn("credential_resourcecrn");
        credential.setAccountId(TEST_ACCOUNT_ID);
        credential.setCloudPlatform("AWS");
        credential.setCreator(TEST_CRN);
        credential.setDescription("description");
        credential.setGovCloud(false);
        credential.setArchived(false);
        credentialRequest = new CredentialRequest();

        when(entitlementService.azureEnabled(any(), any())).thenReturn(true);
        doNothing().when(grpcUmsClient).assignResourceRole(anyString(), anyString(), anyString(), any());
    }

    @AfterEach
    public void clienUpDb() {
        proxyConfigRepository.deleteAll();
        credentialRepository.deleteAll();
    }

    @Test
    public void testCredentialCreateAws() throws InterruptedException {
        credentialRequest.setAws(getAwsKeyBasedCredentialParameters(false, "yyy", "zzzz"));
        credentialRequest.setCloudPlatform("AWS");
        credentialRequest.setName("testcredential");

        when(requestProvider.getResourceDefinitionRequest(any(), any())).thenReturn(resourceDefinitionRequest);
        when(requestProvider.getCredentialVerificationRequest(any(), any())).thenAnswer(
                invocation -> new CredentialVerificationMockRequest(invocation.getArgument(0), invocation.getArgument(1))
        );
        when(resourceDefinitionRequest.await()).thenReturn(new ResourceDefinitionResult(1L, DEFINITION_AWS));

        CredentialResponse response = client.credentialV1Endpoint().post(credentialRequest);
        assertTrue(response.getName().equals(credentialRequest.getName()), " not saved, or response is different");
        assertTrue(credentialRepository.findByNameAndAccountId(credentialRequest.getName(), TEST_ACCOUNT_ID, List.of("AWS")).isPresent());
    }

    @Test
    public void testCredentialInteractiveLogin() throws InterruptedException {
        credentialRequest.setName("testcredential");
        credentialRequest.setCloudPlatform("AZURE");
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        azureCredentialRequestParameters.setSubscriptionId("subid");
        azureCredentialRequestParameters.setTenantId("tenant");
        RoleBasedRequest roleBasedRequest = new RoleBasedRequest();
        roleBasedRequest.setDeploymentAddress("alma");
        roleBasedRequest.setRoleName("role");
        azureCredentialRequestParameters.setRoleBased(roleBasedRequest);
        credentialRequest.setAzure(azureCredentialRequestParameters);

        InteractiveLoginResult interactiveLoginResult = new InteractiveLoginResult(1L, Map.of("user_code", USER_CODE, "verification_url", VERIFICATION_URL));
        when(requestProvider.getInteractiveLoginRequest(any(), any())).thenReturn(interactiveLoginRequest);
        when(interactiveLoginRequest.await()).thenReturn(interactiveLoginResult);
        InteractiveCredentialResponse result = client.credentialV1Endpoint().interactiveLogin(credentialRequest);
        assertEquals(result.getUserCode(), USER_CODE);
        assertEquals(result.getVerificationUrl(), VERIFICATION_URL);
    }

    @Test
    public void testCredentialList() {
        credentialRepository.save(credential);
        CredentialResponses results = client.credentialV1Endpoint().list();
        assertTrue(results.getResponses().stream().anyMatch(credentialResponse -> credentialResponse.getName().equals(credential.getName())),
                String.format("Result set should have credential with name: %s", credential.getName()));
    }

    @Test
    public void testCredentialGetByName() {
        credentialRepository.save(credential);
        CredentialResponse results = client.credentialV1Endpoint().getByName(credential.getName());
        assertTrue(results.getName().equals(credential.getName()),
                String.format("Result should have credential with name: %s", credential.getName()));
    }

    @Test
    public void testCredentialGetByNameNotFound() {
        assertThrows(NotFoundException.class, () -> client.credentialV1Endpoint().getByName("nonexisting"));
    }

    @Test
    public void testCredentialGetByCrn() {
        credentialRepository.save(credential);
        CredentialResponse results = client.credentialV1Endpoint().getByResourceCrn(credential.getResourceCrn());
        assertTrue(results.getName().equals(credential.getName()),
                String.format("Result should have credential with name: %s", credential.getName()));
    }

    @Test
    public void testCredentialGetByCrnNotFound() {
        assertThrows(NotFoundException.class, () -> client.credentialV1Endpoint().getByResourceCrn("nonexisting"));
    }

    @Test
    public void testCredentialDeleteByName() {
        credentialRepository.save(credential);
        CredentialResponse results = client.credentialV1Endpoint().deleteByName(credential.getName());
        assertTrue(results.getName().startsWith(credential.getName()),
                String.format("Result should have credential with name: %s", credential.getName()));
    }

    @Test
    public void testCredentialDeleteByNameNotFound() {
        assertThrows(NotFoundException.class, () -> client.credentialV1Endpoint().deleteByName("nonexisting"));
    }

    @Test
    public void testCredentialDeleteByCrn() {
        credentialRepository.save(credential);
        CredentialResponse results = client.credentialV1Endpoint().deleteByResourceCrn(credential.getResourceCrn());
        assertTrue(results.getName().startsWith(credential.getName()),
                String.format("Result should have credential with name: %s", credential.getName()));
    }

    @Test
    public void testCredentialDeleteByCrnNotFound() {
        assertThrows(NotFoundException.class, () -> client.credentialV1Endpoint().deleteByResourceCrn("nonexisting"));
    }

    @Test
    public void testProxyList() {
        proxyConfigRepository.save(getProxyConfig());
        ProxyResponses results = client.proxyV1Endpoint().list();
        assertTrue(results.getResponses().stream().anyMatch(proxyResponse -> proxyResponse.getName().equals(getProxyConfig().getName())),
                String.format("Result set should have proxy with name: %s", getProxyConfig().getName()));
    }

    @Test
    public void testProxyGetByName() {
        proxyConfigRepository.save(getProxyConfig());
        ProxyResponse results = client.proxyV1Endpoint().getByName(getProxyRequest().getName());
        assertTrue(results.getName().equals(getProxyConfig().getName()),
                String.format("Result should have proxy with name: %s", getProxyConfig().getName()));
    }

    @Test
    public void testProxyGetByNameNotFound() {
        assertThrows(NotFoundException.class, () -> client.proxyV1Endpoint().getByName("nonexisting"));
    }

    @Test
    public void testProxyGetByCrnName() {
        proxyConfigRepository.save(getProxyConfig());
        ProxyResponse results = client.proxyV1Endpoint().getByResourceCrn(getProxyConfig().getResourceCrn());
        assertTrue(results.getCrn().equals(getProxyConfig().getResourceCrn()),
                String.format("Result should have proxy with resource crn: %s", getProxyConfig().getResourceCrn()));
    }

    @Test
    public void testProxyGetByCrnNotFound() {
        assertThrows(NotFoundException.class, () -> client.proxyV1Endpoint().getByResourceCrn("nonexisting"));
    }

    @Test
    public void testProxyDeleteByName() {
        proxyConfigRepository.save(getProxyConfig());
        ProxyResponse results = client.proxyV1Endpoint().deleteByName(getProxyRequest().getName());
        assertTrue(results.getName().equals(getProxyConfig().getName()),
                String.format("Result should have proxy with name: %s", getProxyConfig().getName()));
    }

    @Test
    public void testProxyDeleteByNameNotFound() {
        assertThrows(NotFoundException.class, () -> client.proxyV1Endpoint().deleteByName("nonexisting"));
    }

    @Test
    public void testProxyDeleteByCrnName() {
        proxyConfigRepository.save(getProxyConfig());
        ProxyResponse results = client.proxyV1Endpoint().deleteByCrn(getProxyConfig().getResourceCrn());
        assertTrue(results.getCrn().equals(getProxyConfig().getResourceCrn()),
                String.format("Result should have proxy with resource crn: %s", getProxyConfig().getResourceCrn()));
    }

    @Test
    public void testProxyDeleteByCrnNotFound() {
        assertThrows(NotFoundException.class, () -> client.proxyV1Endpoint().deleteByCrn("nonexisting"));
    }

    @Test
    public void testProxyCreate() throws Exception {
        ProxyRequest request = getProxyRequest();
        request.setPort(8080);
        ProxyResponse result = client.proxyV1Endpoint().post(request);
        assertEquals(request.getName(), result.getName());
        Optional<ProxyConfig> saved = proxyConfigRepository.findByNameInAccount(request.getName(), TEST_ACCOUNT_ID);
        assertTrue(saved.isPresent());
    }

    @Test
    public void testProxyCreateSwaggerError() throws Exception {
        ProxyRequest request = getProxyRequest();
        request.setPort(0);
        assertThrows(BadRequestException.class, () -> client.proxyV1Endpoint().post(request));
    }

    private AwsCredentialParameters getAwsKeyBasedCredentialParameters(boolean govCloud, String yyy, String zzzz) {
        AwsCredentialParameters aws = new AwsCredentialParameters();
        aws.setGovCloud(govCloud);
        KeyBasedParameters keyBased = new KeyBasedParameters();
        keyBased.setAccessKey(yyy);
        keyBased.setSecretKey(zzzz);
        aws.setKeyBased(keyBased);
        return aws;
    }

    static class CredentialVerificationMockRequest extends CredentialVerificationRequest {

        CredentialVerificationMockRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
            super(cloudContext, cloudCredential);
        }

        @Override
        public CredentialVerificationResult await() {
            return new CredentialVerificationResult(1L,
                    new CloudCredentialStatus(getCloudCredential(), CredentialStatus.CREATED));
        }
    }
}
