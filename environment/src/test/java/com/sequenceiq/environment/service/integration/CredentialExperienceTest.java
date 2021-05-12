package com.sequenceiq.environment.service.integration;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.google.common.collect.Maps;
import com.sequenceiq.authorization.service.UmsAccountAuthorizationService;
import com.sequenceiq.authorization.service.UmsResourceAuthorizationService;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.response.AwsCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.response.AzureCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.cloud.response.GcpCredentialPrerequisites;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.common.CommonExperienceConnectorService;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;
import com.sequenceiq.environment.experience.policy.response.ProviderPolicyResponse;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;
import com.sequenceiq.environment.service.integration.testconfiguration.TestConfigurationWithoutCloudAccess;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfigurationWithoutCloudAccess.class,
        properties = "info.app.version=test")
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "environment.experience.scan.enabled=true"
})
public class CredentialExperienceTest {

    private static final String SERVICE_ADDRESS = "http://localhost:%d/environmentservice";

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_RESOURCE_CRN = String.format("crn:cdp:environments:us-west-1:%s:credential:asdasd", TEST_ACCOUNT_ID);

    private static final String MINIMAL_POLICY = "minimalPolicy";

    private static final String COMMON_POLICY = "p";

    private static final String LIFTIE_POLICY = "l";

    private static final String ASTERISK = "asterisk";

    @LocalServerPort
    private int port;

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

    @Inject
    private ProxyConfigRepository proxyConfigRepository;

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private SecretService secretService;

    private Credential credential;

    @Mock
    private CloudConnector<Object> connector;

    @Mock
    private CredentialConnector credentialConnector;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private CommonExperienceConnectorService commonExperienceConnectorService;

    @MockBean
    private LiftieApi liftieApi;

    private EnvironmentServiceCrnEndpoints client;

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
        credential.setCloudPlatform("AWS");
        credential.setCreator(TEST_USER_CRN);
        credential.setDescription("description");
        credential.setGovCloud(false);
        credential.setArchived(false);
        credential.setType(ENVIRONMENT);

        doNothing().when(grpcUmsClient).assignResourceRole(anyString(), anyString(), anyString(), any());
        lenient().when(grpcUmsClient.hasRights(anyString(), anyList(), any())).then(i -> {
            List<AuthorizationProto.RightCheck> rightChecks = i.getArgument(1);
            return rightChecks.stream().map(r -> Boolean.TRUE).collect(toList());
        });
        lenient().when(grpcUmsClient.checkAccountRight(anyString(), anyString(), any())).thenReturn(true);
        Map<String, Boolean> rightCheckMap = Maps.newHashMap();
        rightCheckMap.put(credential.getResourceCrn(), true);
        when(umsResourceAuthorizationService.getRightOfUserOnResources(anyString(), any(), anyList())).thenReturn(rightCheckMap);

        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(connector);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.credentials()).thenReturn(credentialConnector);
    }

    @Test
    public void testAwsPoliciesArePresent() {
        CredentialPrerequisitesResponse res = testSkeleton("AWS", Boolean.TRUE);
        Assertions.assertNotNull(res.getAws().getPolicies());
        Assertions.assertEquals(3, res.getAws().getPolicies().size());
        ArgumentCaptor<String> a = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> b = ArgumentCaptor.forClass(String.class);
        verify(commonExperienceConnectorService).collectPolicy(a.capture(), b.capture());
        Assertions.assertTrue(a.getValue().contains("{cloudProvider}"));
        Assertions.assertTrue(res.getAws().getPolicies().containsKey("Environment"));
        Assertions.assertTrue(res.getAws().getPolicies().containsKey("Data Warehouses"));
        Assertions.assertTrue(res.getAws().getPolicies().containsKey("Kubernetes cluster manager"));
        Assertions.assertTrue(res.getAws().getPolicies().get("Environment").equals(MINIMAL_POLICY));
        Assertions.assertTrue(res.getAws().getPolicies().get("Data Warehouses").equals(COMMON_POLICY));
        Assertions.assertTrue(res.getAws().getPolicies().get("Kubernetes cluster manager").equals(LIFTIE_POLICY));
    }

    @Test
    public void testAwsEntitlementDisabled() {
        CredentialPrerequisitesResponse res = testSkeleton("AWS", Boolean.FALSE);
        Assertions.assertNotNull(res.getAws().getPolicies());
        Assertions.assertEquals(1, res.getAws().getPolicies().size());

        verify(commonExperienceConnectorService, never()).collectPolicy(anyString(), anyString());
        Assertions.assertTrue(res.getAws().getPolicies().containsKey("Environment"));
        Assertions.assertTrue(res.getAws().getPolicies().get("Environment").equals(MINIMAL_POLICY));
    }

    @Test
    public void testAzureEntitlementAllowed() {
        when(entitlementService.azureEnabled(any())).thenReturn(true);

        CredentialPrerequisitesResponse res = testSkeleton("AZURE", Boolean.FALSE);
        Assertions.assertNotNull(res.getAzure().getPolicies());
        Assertions.assertEquals(0, res.getAzure().getPolicies().size());

        verify(commonExperienceConnectorService, never()).collectPolicy(anyString(), anyString());
    }

    private CredentialPrerequisitesResponse testSkeleton(String cloudProvider, Boolean entitlementEnabled) {

        when(credentialConnector.getPrerequisites(any(), any(), any(), any())).thenReturn(getCredentialPrerequisitesResponse(cloudProvider, ASTERISK));

        when(entitlementService.awsRestrictedPolicy(anyString())).thenReturn(entitlementEnabled);

        when(commonExperienceConnectorService.collectPolicy(anyString(), anyString())).thenReturn(getExperiencePolicyJson(cloudProvider, COMMON_POLICY));
        when(liftieApi.getPolicy(anyString())).thenReturn(getExperiencePolicyJson(cloudProvider, LIFTIE_POLICY));

        return client.credentialV1Endpoint().getPrerequisitesForCloudPlatform(cloudProvider, "addr");
    }

    private ExperiencePolicyResponse getExperiencePolicyJson(String cloudProvider, String policy) {
        ExperiencePolicyResponse value = new ExperiencePolicyResponse();
        ProviderPolicyResponse policies = new ProviderPolicyResponse();
        policies.setPolicy(policy);
        switch (cloudProvider) {
            case "AWS":
                value.setAws(policies);
                break;
            case "AZURE":
                value.setAzure(policies);
                break;
            default:
        }
        return value;
    }

    private CredentialPrerequisitesResponse getCredentialPrerequisitesResponse(String cloudProvider, String policy) {
        CredentialPrerequisitesResponse response = new CredentialPrerequisitesResponse();
        switch (cloudProvider) {
            case "AWS":
                response.setAws(new AwsCredentialPrerequisites());
                response.getAws().setPolicyJson(policy);
                response.getAws().setPolicies(Map.of("Environment", MINIMAL_POLICY));
                break;
            case "AZURE":
                response.setAzure(new AzureCredentialPrerequisites());
                response.getAzure().setRoleDefitionJson(policy);
                break;
            case "GCP":
                response.setGcp(new GcpCredentialPrerequisites());
                break;
            default:
        }
        return response;
    }
}
