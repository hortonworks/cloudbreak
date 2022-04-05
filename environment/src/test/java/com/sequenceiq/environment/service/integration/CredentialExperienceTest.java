package com.sequenceiq.environment.service.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.client.Client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.quartz.Scheduler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.handler.CredentialPrerequisitesHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformInitializer;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.response.AwsCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.response.AzureCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.cloud.response.GcpCredentialPrerequisites;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.config.ConversionConfig;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.authorization.EnvironmentCredentialFiltering;
import com.sequenceiq.environment.credential.reactor.handler.CredentialExperiencePolicyHandler;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.service.CredentialDeleteService;
import com.sequenceiq.environment.credential.service.CredentialPrerequisiteService;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.service.ServiceProviderCredentialAdapter;
import com.sequenceiq.environment.credential.v1.CredentialV1Controller;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.environment.flow.EnvEventParameterFactory;
import com.sequenceiq.environment.environment.flow.EnvironmentFlowInformation;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.verification.PolicyValidationErrorResponseConverter;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.common.CommonExperienceConnectorService;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;
import com.sequenceiq.environment.experience.policy.response.ProviderPolicyResponse;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.user.UserPreferences;
import com.sequenceiq.environment.user.UserPreferencesRepository;
import com.sequenceiq.environment.user.UserPreferencesService;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.repository.FlowChainLogRepository;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.notification.HttpNotificationSenderService;
import com.sequenceiq.notification.NotificationService;

import io.opentracing.Tracer;
import reactor.core.Dispatcher;
import reactor.core.dispatch.SynchronousDispatcher;

@ExtendWith(SpringExtension.class)
public class CredentialExperienceTest {

    private static final String MINIMAL_POLICY = "minimalPolicy";

    private static final String COMMON_POLICY = "p";

    private static final String LIFTIE_POLICY = "l";

    private static final String ASTERISK = "asterisk";

    private static final String UUID = java.util.UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + UUID + ":user:" + UUID;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private CommonExperienceConnectorService commonExperienceConnectorService;

    @MockBean
    private LiftieApi liftieApi;

    @MockBean
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private CloudConnector<Object> connector;

    @Mock
    private CredentialConnector credentialConnector;

    @Inject
    private CredentialV1Controller credentialV1Controller;

    @MockBean
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @MockBean
    private PolicyValidationErrorResponseConverter policyValidationErrorResponseConverter;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @BeforeEach
    public void setup() {
        when(userPreferencesRepository.save(any())).thenReturn(new UserPreferences("xid", "audit-xid", "user"));

        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(connector);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.credentials()).thenReturn(credentialConnector);

        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

    @Test
    public void testAwsPoliciesArePresent() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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

        when(credentialConnector.getPrerequisites(any(), any(), any(), any(), any())).thenReturn(getCredentialPrerequisitesResponse(cloudProvider, ASTERISK));

        when(entitlementService.awsRestrictedPolicy(anyString())).thenReturn(entitlementEnabled);

        when(commonExperienceConnectorService.collectPolicy(anyString(), anyString())).thenReturn(getExperiencePolicyJson(cloudProvider, COMMON_POLICY));
        when(liftieApi.getPolicy(anyString())).thenReturn(getExperiencePolicyJson(cloudProvider, LIFTIE_POLICY));

        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> credentialV1Controller.getPrerequisitesForCloudPlatform(cloudProvider, false, "addr"));
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

    @Configuration
    @Import({CredentialPrerequisiteService.class,
            CredentialService.class,
            CredentialV1Controller.class,
            EnvironmentFlowInformation.class,
            EnvironmentMetricService.class,
            EnvEventParameterFactory.class,
            UserPreferencesService.class,
            CredentialPrerequisitesHandler.class,
            CredentialExperiencePolicyHandler.class,
            CloudbreakMessagesService.class,

            CloudPlatformInitializer.class,
            ConversionConfig.class,
            TransactionService.class,
            TransactionMetricsService.class,
            Clock.class,
            HttpNotificationSenderService.class,
    })
    @ComponentScan(basePackages = {
            "com.sequenceiq.flow",
            "com.sequenceiq.environment.experience"
    })
    @EnableConfigurationProperties
    static class Config {
        @MockBean
        private CredentialRepository credentialRepository;

        @MockBean
        private FlowLogRepository flowLogRepository;

        @MockBean
        private FlowChainLogRepository flowChainLogRepository;

        @MockBean
        private CredentialValidator credentialValidator;

        @MockBean
        private ServiceProviderCredentialAdapter serviceProviderCredentialAdapter;

        @MockBean
        private OwnerAssignmentService ownerAssignmentService;

        @MockBean
        private CredentialToCredentialV1ResponseConverter credentialToCredentialV1ResponseConverter;

        @MockBean
        private CredentialDeleteService credentialDeleteService;

        @MockBean
        private EnvironmentCredentialFiltering environmentCredentialFiltering;

        @MockBean
        private NotificationService notificationService;

        @MockBean
        private Client client;

        @MockBean
        private SecretService secretService;

        @MockBean
        private FreeIpaV1Endpoint freeIpaV1Endpoint;

        @MockBean
        private Tracer tracer;

        @MockBean
        private EnvironmentService environmentService;

        @MockBean
        private Scheduler scheduler;

        @MockBean
        private FlowOperationStatisticsService flowOperationStatisticsService;

        @MockBean
        private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

        @Bean
        @Primary
        MetricService metricService() {
            return new EnvironmentMetricService();
        }

        @Bean
        public Dispatcher dispatcher(MDCCleanerThreadPoolExecutor threadPoolExecutor) {
            return new SynchronousDispatcher();
        }

    }

}
