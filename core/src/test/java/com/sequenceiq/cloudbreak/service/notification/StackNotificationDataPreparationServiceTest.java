package com.sequenceiq.cloudbreak.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.notification.config.CDPConsoleUrlProvider;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.ClusterHealthNotificationAdditionalDataDto;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.InstanceStatusDto;

@ExtendWith(MockitoExtension.class)
class StackNotificationDataPreparationServiceTest {

    private static final String TEST_ACCOUNT_ID = "test-account";

    private static final String TEST_REASON = "Test Failure";

    private static final String TEST_CRN = "crn:cdp:datahub:us-west-1:test-account:datahub:test-crn";

    private static final String TEST_STACK_NAME = "test-stack";

    private static final String TEST_USER_NAME = "test-user";

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackNotificationTypePreparationService stackNotificationTypePreparationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CDPConsoleUrlProvider cdpConsoleUrlProvider;

    @Mock
    private PlatformResources platformResources;

    @InjectMocks
    private StackNotificationDataPreparationService underTest;

    @BeforeEach
    void setUp() throws TransactionService.TransactionExecutionException {
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    @Test
    void testNotificationGeneratorDtos() throws TransactionService.TransactionExecutionException {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName("env1");
        Stack stack = createTestStack();
        Status newStatus = Status.CREATE_FAILED;
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.PROVISION_FAILED;
        List<InstanceMetaData> instanceMetaData = createTestInstanceMetaData();

        when(stackNotificationTypePreparationService.notificationType(newStatus)).thenReturn(NotificationType.STACK_HEALTH);
        when(instanceMetaDataService.findAllByStackIdAndStatusGroup(eq(stack.getId()), any())).thenReturn(instanceMetaData);
        when(platformResources.getVirtualMachineUrl(any(), any(), any(), any())).thenReturn(Optional.of("http://example.com"));
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(environmentService.getByCrn(any())).thenReturn(detailedEnvironmentResponse);
        when(cdpConsoleUrlProvider.getClusterUrl(any(), any(), any())).thenReturn("http://example.com");
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(cloudConnector);

        NotificationGeneratorDtos result = underTest.notificationGeneratorDtos(stack, newStatus, newDetailedStatus, TEST_REASON, TEST_ACCOUNT_ID);

        assertNotNull(result);
        assertEquals(NotificationType.STACK_HEALTH, result.getNotificationType());
        assertEquals(1, result.getNotifications().size());

        NotificationGeneratorDto notificationDto = (NotificationGeneratorDto) result.getNotifications().iterator().next();
        assertEquals(TEST_ACCOUNT_ID, notificationDto.getAccountId());
        assertEquals(TEST_CRN, notificationDto.getResourceCrn());
        assertEquals(TEST_STACK_NAME, notificationDto.getName());

        BaseNotificationRegisterAdditionalDataDtos additionalData = notificationDto.getAdditionalData();
        assertNotNull(additionalData);
        assertEquals(1, additionalData.getResults().size());

        ClusterHealthNotificationAdditionalDataDto healthDto = (ClusterHealthNotificationAdditionalDataDto) additionalData.getResults().get(0);
        assertEquals("Create Failed", healthDto.getStatus());
        assertEquals(TEST_REASON, healthDto.getStatusReason());
        assertEquals("Provision Failed", healthDto.getDetailedStatus());
        assertEquals(TEST_USER_NAME, healthDto.getCreator());
        assertEquals("Data Hub", healthDto.getStackType());
        assertEquals(TEST_CRN, healthDto.getCrn());
        assertEquals(TEST_STACK_NAME, healthDto.getName());
        assertNotNull(healthDto.getDateTimeString());

        Set<InstanceStatusDto> instanceDtos = healthDto.getInstances();
        assertEquals(1, instanceDtos.size());
        InstanceStatusDto instanceDto = instanceDtos.iterator().next();
        assertEquals("instance-1", instanceDto.getName());
        assertEquals("master", instanceDto.getGroupName());
        assertEquals("m5.xlarge", instanceDto.getInstanceType());
        assertEquals("Services Unhealthy", instanceDto.getStatus());
    }

    private Stack createTestStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setResourceCrn(TEST_CRN);
        stack.setName(TEST_STACK_NAME);
        stack.setType(StackType.WORKLOAD);
        User creator = new User();
        creator.setUserName(TEST_USER_NAME);
        stack.setCreator(creator);
        return stack;
    }

    private List<InstanceMetaData> createTestInstanceMetaData() {
        InstanceMetaData metaData = new InstanceMetaData();
        metaData.setInstanceName("instance-1");
        metaData.setInstanceId("instance-1");
        metaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY);
        metaData.setProviderInstanceType("m5.xlarge");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        metaData.setInstanceGroup(instanceGroup);
        return List.of(metaData);
    }
}
