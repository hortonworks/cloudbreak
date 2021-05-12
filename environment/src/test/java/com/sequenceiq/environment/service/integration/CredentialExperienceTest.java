package com.sequenceiq.environment.service.integration;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

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
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;
import com.sequenceiq.environment.service.integration.testconfiguration.TestConfigurationWithoutCloudAccess;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfigurationWithoutCloudAccess.class,
        properties = "info.app.version=test")
@ActiveProfiles("test")
public class CredentialExperienceTest {

    private static final String SERVICE_ADDRESS = "http://localhost:%d/environmentservice";

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_RESOURCE_CRN = String.format("crn:cdp:environments:us-west-1:%s:credential:asdasd", TEST_ACCOUNT_ID);

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

    private CredentialRequest credentialRequest;

    private Credential credential;

    @Mock
    private CloudConnector<Object> connector;

    @Mock
    private CredentialConnector credentialConnector;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

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
        credentialRequest = new CredentialRequest();

        when(entitlementService.azureEnabled(any())).thenReturn(true);
        doNothing().when(grpcUmsClient).assignResourceRole(anyString(), anyString(), anyString(), any());
        lenient().when(grpcUmsClient.hasRights(anyString(), anyString(), anyList(), any())).then(i -> {
            List<AuthorizationProto.RightCheck> rightChecks = i.getArgument(2);
            return rightChecks.stream().map(r -> Boolean.TRUE).collect(toList());
        });
        lenient().when(grpcUmsClient.checkAccountRight(anyString(), anyString(), anyString(), any())).thenReturn(true);
        Map<String, Boolean> rightCheckMap = Maps.newHashMap();
        rightCheckMap.put(credential.getResourceCrn(), true);
        when(umsResourceAuthorizationService.getRightOfUserOnResources(anyString(), any(), anyList())).thenReturn(rightCheckMap);

        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(connector);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.credentials()).thenReturn(credentialConnector);
    }

    @Test
    public void test() {
        client.credentialV1Endpoint().getPrerequisitesForCloudPlatform("AWS", "addr");
    }
}
