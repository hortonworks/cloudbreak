package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsLaunchService.CREATED_DB_INSTANCE;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsLaunchService.CREATED_DB_PARAMETER_GROUP;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsLaunchService.CREATED_DB_SUBNET_GROUP;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsLaunchService.HOSTNAME;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsLaunchService.PORT;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.RDSModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsRdsLaunchServiceTest {

    private static final Long STACK_ID = 1234L;

    private static final String STACK_NAME = "myStack";

    private static final String STACK_CRN = "crn";

    private static final String STACK_NAME_CF = STACK_NAME + "-" + STACK_ID;

    private static final String PLATFORM = "AWS";

    private static final String VARIANT = "";

    private static final String REGION = "eu-central-1";

    private static final String AVAILABILITY_ZONE = "eu-central-1b";

    private static final String USER_ID = UUID.randomUUID().toString();

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String SUBNET_CIDR = "0.0.0.0/0";

    private static final String SUBNET_ID = "subnet-lkewflerwkj";

    private static final String NETWORK_CIDR = "127.0.0.1/32";

    private static final String SECURITY_GROUP_ID = "sg-98e47c";

    private static final String SSL_CERTIFICATE_IDENTIFIER = "mycert";

    private static final String OUT_DB_INSTANCE = "db-" + UUID.randomUUID().toString();

    private static final String OUT_HOSTNAME = OUT_DB_INSTANCE + ".oijeojwodihfoih." + REGION + ".rds.amazonaws.com";

    private static final String OUT_PORT = "23456";

    private static final String OUT_DB_SUBNET_GROUP = "dsg-kfhoiehjoehij";

    private static final String OUT_DB_PARAMETER_GROUP = "dpg-lojdfoiwejpoekf";

    private static final String CF_TEMPLATE = "{\"template\" : \"foo\"}";

    private static final Map<String, String> CF_OUTPUTS_WITHOUT_DB_PARAMETER_GROUP = Map.ofEntries(
            entry(HOSTNAME, OUT_HOSTNAME),
            entry(PORT, OUT_PORT),
            entry(CREATED_DB_INSTANCE, OUT_DB_INSTANCE),
            entry(CREATED_DB_SUBNET_GROUP, OUT_DB_SUBNET_GROUP));

    private static final Map<String, String> CF_OUTPUTS_WITH_DB_PARAMETER_GROUP = Map.ofEntries(
            entry(HOSTNAME, OUT_HOSTNAME),
            entry(PORT, OUT_PORT),
            entry(CREATED_DB_INSTANCE, OUT_DB_INSTANCE),
            entry(CREATED_DB_SUBNET_GROUP, OUT_DB_SUBNET_GROUP),
            entry(CREATED_DB_PARAMETER_GROUP, OUT_DB_PARAMETER_GROUP));

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsClient awsClient;

    @Mock
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Mock
    private AwsStackRequestHelper awsStackRequestHelper;

    @Mock
    private PersistenceNotifier resourceNotifier;

    @Mock
    private AmazonCloudFormationClient cfRetryClient;

    @Mock
    private AmazonCloudFormationWaiters cfWaiters;

    @Mock
    private Waiter<DescribeStacksRequest> creationWaiter;

    @Captor
    private ArgumentCaptor<RDSModelContext> rdsModelContextCaptor;

    @InjectMocks
    private AwsRdsLaunchService underTest;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void setUp() {
        Region region = Region.region(REGION);
        AvailabilityZone availabilityZone = AvailabilityZone.availabilityZone(AVAILABILITY_ZONE);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(STACK_ID)
                .withName(STACK_NAME)
                .withCrn(STACK_CRN)
                .withPlatform(PLATFORM)
                .withVariant(VARIANT)
                .withLocation(Location.location(region, availabilityZone))
                .withUserId(USER_ID)
                .withAccountId(ACCOUNT_ID)
                .build();

        CloudCredential cloudCredential = new CloudCredential();
        authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);

        when(cfStackUtil.getCfStackName(authenticatedContext)).thenReturn(STACK_NAME_CF);

        when(awsClient.createCloudFormationClient(isA(AwsCredentialView.class), eq(REGION))).thenReturn(cfRetryClient);

        when(cfRetryClient.describeStacks(isA(DescribeStacksRequest.class))).thenThrow(new AmazonServiceException("Stack not found"));

        when(cfRetryClient.waiters()).thenReturn(cfWaiters);
        when(cfWaiters.stackCreateComplete()).thenReturn(creationWaiter);

        when(cloudFormationTemplateBuilder.build(isA(RDSModelContext.class))).thenReturn(CF_TEMPLATE);
    }

    @Test
    void launchTestUseSslEnforcementWhenEnforcementDisabled() {
        launchTestUseSslEnforcementInternal(false, false);
    }

    @Test
    void launchTestUseSslEnforcementWhenEnforcementEnabledAndCertificateIdentifierMissing() {
        launchTestUseSslEnforcementInternal(true, false);
    }

    @Test
    void launchTestUseSslEnforcementWhenEnforcementEnabledAndCertificateIdentifierPresent() {
        launchTestUseSslEnforcementInternal(true, true);
    }

    private void launchTestUseSslEnforcementInternal(boolean useSslEnforcement, boolean sslCertificateIdentifierDefined) {
        when(cfStackUtil.getOutputs(STACK_NAME_CF, cfRetryClient))
                .thenReturn(useSslEnforcement ? CF_OUTPUTS_WITH_DB_PARAMETER_GROUP : CF_OUTPUTS_WITHOUT_DB_PARAMETER_GROUP);

        List<CloudResourceStatus> cloudResourceStatuses = underTest.launch(authenticatedContext,
                createDatabaseStack(useSslEnforcement, sslCertificateIdentifierDefined), resourceNotifier);

        assertThat(cloudResourceStatuses).isNotNull();
        assertThat(cloudResourceStatuses).hasSize(useSslEnforcement ? 6 : 5);

        cloudResourceStatuses.forEach(status -> assertThat(status).isNotNull());
        cloudResourceStatuses.forEach(status -> assertThat(status.getCloudResource()).isNotNull());

        checkOutputResourceExists(cloudResourceStatuses, ResourceType.RDS_HOSTNAME, OUT_HOSTNAME);
        checkOutputResourceExists(cloudResourceStatuses, ResourceType.RDS_PORT, OUT_PORT);
        checkOutputResourceExists(cloudResourceStatuses, ResourceType.RDS_INSTANCE, OUT_DB_INSTANCE);
        checkOutputResourceExists(cloudResourceStatuses, ResourceType.RDS_DB_SUBNET_GROUP, OUT_DB_SUBNET_GROUP);
        if (useSslEnforcement) {
            checkOutputResourceExists(cloudResourceStatuses, ResourceType.RDS_DB_PARAMETER_GROUP, OUT_DB_PARAMETER_GROUP);
        } else {
            checkOutputResourceIsAbsent(cloudResourceStatuses, ResourceType.RDS_DB_PARAMETER_GROUP, OUT_DB_PARAMETER_GROUP);
        }
        checkOutputResourceExists(cloudResourceStatuses, ResourceType.CLOUDFORMATION_STACK, STACK_NAME_CF);

        verify(cloudFormationTemplateBuilder).build(rdsModelContextCaptor.capture());
        RDSModelContext rdsModelContext = rdsModelContextCaptor.getValue();
        assertThat(rdsModelContext).isNotNull();
        Boolean useSslEnforcementResult = (Boolean) ReflectionTestUtils.getField(rdsModelContext, "useSslEnforcement");
        assertThat(useSslEnforcementResult).isNotNull();
        assertThat(useSslEnforcementResult.booleanValue()).isEqualTo(useSslEnforcement);
        Boolean sslCertificateIdentifierDefinedResult = (Boolean) ReflectionTestUtils.getField(rdsModelContext, "sslCertificateIdentifierDefined");
        assertThat(sslCertificateIdentifierDefinedResult).isNotNull();
        assertThat(sslCertificateIdentifierDefinedResult.booleanValue()).isEqualTo(sslCertificateIdentifierDefined);
    }

    private DatabaseStack createDatabaseStack(boolean useSslEnforcement, boolean sslCertificateIdentifierDefined) {
        Subnet subnet = new Subnet(SUBNET_CIDR);
        Network network = new Network(subnet, List.of(NETWORK_CIDR), OutboundInternetTraffic.ENABLED);
        network.putParameter("subnetId", SUBNET_ID);
        network.putParameter("vpcCidr", NETWORK_CIDR);

        Security security = new Security(Collections.emptyList(), List.of(SECURITY_GROUP_ID));
        DatabaseServer databaseServer = DatabaseServer.builder()
                .useSslEnforcement(useSslEnforcement)
                .params(sslCertificateIdentifierDefined ? Map.of(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER, SSL_CERTIFICATE_IDENTIFIER) : Map.of())
                .security(security)
                .build();

        return new DatabaseStack(network, databaseServer, Collections.emptyMap(), null);
    }

    private void checkOutputResourceExists(List<CloudResourceStatus> cloudResourceStatuses, ResourceType type, String nameExpected) {
        checkOutputResourceInternal(cloudResourceStatuses, type, nameExpected, true);
    }

    private void checkOutputResourceIsAbsent(List<CloudResourceStatus> cloudResourceStatuses, ResourceType type, String nameExpected) {
        checkOutputResourceInternal(cloudResourceStatuses, type, nameExpected, false);
    }

    private void checkOutputResourceInternal(List<CloudResourceStatus> cloudResourceStatuses, ResourceType type, String nameExpected,
            boolean existenceExpected) {
        Optional<CloudResourceStatus> cloudResourceStatusOptional =
                cloudResourceStatuses.stream().filter(resource -> resource.getCloudResource().getType() == type).findFirst();
        if (existenceExpected) {
            assertThat(cloudResourceStatusOptional.isPresent()).isTrue();
            CloudResourceStatus cloudResourceStatus = cloudResourceStatusOptional.get();
            assertThat(cloudResourceStatus.getStatus()).isEqualTo(ResourceStatus.CREATED);
            assertThat(cloudResourceStatus.getCloudResource().getName()).isEqualTo(nameExpected);
        } else {
            assertThat(cloudResourceStatusOptional.isEmpty()).isTrue();
        }
    }

}