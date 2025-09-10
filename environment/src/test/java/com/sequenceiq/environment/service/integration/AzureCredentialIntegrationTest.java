package com.sequenceiq.environment.service.integration;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto.RightCheck;
import com.google.common.collect.Maps;
import com.sequenceiq.authorization.service.UmsAccountAuthorizationService;
import com.sequenceiq.authorization.service.UmsResourceAuthorizationService;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.quartz.configuration.QuartzJobInitializer;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.common.api.credential.AppCertificateStatus;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.service.RequestProvider;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;
import com.sequenceiq.environment.service.integration.testconfiguration.TestConfigurationForServiceIntegration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestConfigurationForServiceIntegration.class,
        properties = {"spring.main.allow-bean-definition-overriding=true", "info.app.version=test"})
@ActiveProfiles("test")
public class AzureCredentialIntegrationTest {

    private static final String SERVICE_ADDRESS = "http://localhost:%d/environmentservice";

    private static final String DEFINITION_AZURE = FileReaderUtils.readFileFromClasspathQuietly("testCredentialDefinitionAzure.json");

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_RESOURCE_CRN = String.format("crn:cdp:environments:us-west-1:%s:credential:asdasd", TEST_ACCOUNT_ID);

    private static final String VERIFICATION_URL = "http://cloudera.com";

    private EnvironmentServiceCrnEndpoints client;

    @LocalServerPort
    private int port;

    @Mock
    private ResourceDefinitionRequest resourceDefinitionRequest;

    @MockBean
    private RequestProvider requestProvider;

    @MockBean
    private UmsResourceAuthorizationService umsResourceAuthorizationService;

    @MockBean
    private UmsAccountAuthorizationService umsAccountAuthorizationService;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private GrpcUmsClient grpcUmsClient;

    @MockBean
    private QuartzJobInitializer quartzJobInitializer;

    @Inject
    private ProxyConfigRepository proxyConfigRepository;

    @Inject
    private CredentialRepository credentialRepository;

    private CredentialRequest credentialRequest;

    private Credential credential;

    @BeforeEach
    public void setup() {
        client = new EnvironmentServiceClientBuilder(String.format(SERVICE_ADDRESS, port))
                .withCertificateValidation(false)
                .withDebug(true)
                .withIgnorePreValidation(true)
                .build()
                .withCrn(TEST_USER_CRN);

        credential = new Credential();
        credential.setName("credential_test");
        credential.setResourceCrn(TEST_RESOURCE_CRN);
        credential.setAccountId(TEST_ACCOUNT_ID);
        credential.setCloudPlatform("AZURE");
        credential.setCreator(TEST_USER_CRN);
        credential.setDescription("description");
        credential.setGovCloud(false);
        credential.setArchived(false);
        credential.setType(ENVIRONMENT);
        credentialRequest = new CredentialRequest();

        when(entitlementService.isAzureCertificateAuthEnabled(any())).thenReturn(true);
        doNothing().when(grpcUmsClient).assignResourceRole(anyString(), anyString(), anyString());
        lenient().when(grpcUmsClient.hasRights(anyString(), anyList())).then(i -> {
            List<RightCheck> rightChecks = i.getArgument(1);
            return rightChecks.stream().map(r -> Boolean.TRUE).collect(toList());
        });
        lenient().when(grpcUmsClient.checkAccountRight(anyString(), anyString())).thenReturn(true);
        Map<String, Boolean> rightCheckMap = Maps.newHashMap();
        rightCheckMap.put(credential.getResourceCrn(), true);
        when(umsResourceAuthorizationService.getRightOfUserOnResources(anyString(), any(), anyList())).thenReturn(rightCheckMap);
        when(grpcUmsClient.getResourceRoles(any())).thenReturn(Set.of(
                "crn:altus:iam:us-west-1:altus:resourceRole:Owner",
                "crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentAdmin"));
    }

    @AfterEach
    public void clienUpDb() {
        proxyConfigRepository.deleteAll();
        credentialRepository.deleteAll();
    }

    @Test
    public void testCredentialAzureAppSecret() throws InterruptedException {
        credentialRequest.setName("testcredential");
        credentialRequest.setCloudPlatform("AZURE");
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        azureCredentialRequestParameters.setSubscriptionId("subid");
        azureCredentialRequestParameters.setTenantId("tenant");

        AppBasedRequest appBasedRequest = new AppBasedRequest();
        appBasedRequest.setAuthenticationType(AppAuthenticationType.SECRET);
        appBasedRequest.setAccessKey("accesskey");
        appBasedRequest.setSecretKey("secretkey");
        azureCredentialRequestParameters.setAppBased(appBasedRequest);
        credentialRequest.setAzure(azureCredentialRequestParameters);

        when(requestProvider.getResourceDefinitionRequest(any(), any())).thenReturn(resourceDefinitionRequest);
        when(requestProvider.getCredentialVerificationRequest(any(), any(), anyBoolean())).thenAnswer(
                invocation -> new CredentialVerificationMockRequest(invocation.getArgument(0), invocation.getArgument(1))
        );
        when(resourceDefinitionRequest.await()).thenReturn(new ResourceDefinitionResult(1L, DEFINITION_AZURE));

        CredentialResponse response = client.credentialV1Endpoint().create(credentialRequest);
        assertTrue(response.getName().equals(credentialRequest.getName()), " not saved, or response is different");
        assertTrue(credentialRepository.findByNameAndAccountId(credentialRequest.getName(), TEST_ACCOUNT_ID, List.of("AZURE"), ENVIRONMENT).isPresent());

        assertEquals("testcredential", response.getName());
        assertEquals("accesskey", response.getAzure().getAccessKey());
        assertEquals("subid", response.getAzure().getSubscriptionId());
        assertEquals("tenant", response.getAzure().getTenantId());
        assertEquals(AppAuthenticationType.SECRET, response.getAzure().getAuthenticationType());
        assertNull(response.getAzure().getRoleBased(), "Since this is app based, therefore role property should be null");
    }

    @Test
    public void testCredentialAzureAppCertificate() throws InterruptedException {
        credentialRequest.setName("testcredential");
        credentialRequest.setCloudPlatform("AZURE");
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        azureCredentialRequestParameters.setSubscriptionId("subid");
        azureCredentialRequestParameters.setTenantId("tenant");

        AppBasedRequest appBasedRequest = new AppBasedRequest();
        appBasedRequest.setAuthenticationType(AppAuthenticationType.CERTIFICATE);
        azureCredentialRequestParameters.setAppBased(appBasedRequest);
        credentialRequest.setAzure(azureCredentialRequestParameters);

        when(requestProvider.getResourceDefinitionRequest(any(), any())).thenReturn(resourceDefinitionRequest);
        when(requestProvider.getCredentialVerificationRequest(any(), any(), anyBoolean())).thenAnswer(
                invocation -> new CredentialVerificationMockRequest(invocation.getArgument(0), invocation.getArgument(1))
        );
        when(resourceDefinitionRequest.await()).thenReturn(new ResourceDefinitionResult(1L, DEFINITION_AZURE));

        CredentialResponse response = client.credentialV1Endpoint().create(credentialRequest);
        assertEquals(response.getName(), credentialRequest.getName(), " not saved, or response is different");
        assertTrue(credentialRepository.findByNameAndAccountId(credentialRequest.getName(), TEST_ACCOUNT_ID, List.of("AZURE"), ENVIRONMENT).isPresent());

        assertEquals("testcredential", response.getName());
        assertNull(response.getAzure().getAccessKey(), "Since this is access key is not passed, therefore it should be null");
        assertEquals("subid", response.getAzure().getSubscriptionId());
        assertEquals("tenant", response.getAzure().getTenantId());
        assertEquals(AppAuthenticationType.CERTIFICATE, response.getAzure().getAuthenticationType());
        assertEquals(AppCertificateStatus.KEY_GENERATED, response.getAzure().getCertificate().getStatus());
        assertNull(response.getAzure().getRoleBased(), "Since this is app based, therefore role property should be null");
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
