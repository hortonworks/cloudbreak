package com.sequenceiq.cloudbreak.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsUpdateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class AwsSyncUserDataServiceTest {

    @Mock
    private AwsUserDataService awsUserDataService;

    @Mock
    private AwsUpdateService awsUpdateService;

    @InjectMocks
    private AwsSyncUserDataService underTest;

    @Mock
    private CloudStack cloudStack;

    private AuthenticatedContext ac;

    @BeforeEach
    void setUp() {
        Location location = Location.location(Region.region("region"));
        CloudContext context = CloudContext.Builder.builder()
                .withId(123L)
                .withName("cloudContext")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("variant")
                .withLocation(location)
                .withWorkspaceId(234L)
                .build();
        CloudCredential cred = new CloudCredential("crn", "cc", "account");
        ac = new AuthenticatedContext(context, cred);
    }

    @Test
    void testSyncUserDataWhenEmpty() {
        List<CloudResource> resources = List.of();
        underTest.syncUserData(ac, cloudStack, resources);
        verify(awsUpdateService, never()).updateUserData(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    @NullSource
    void testSyncUserDataWhenUserDataIsNullOrEmpty(String userdata) {
        List<CloudResource> resources = List.of();
        Map<String, String> userDataMap = Map.of("hostgroup1", "awsUserdata-A", "hostgroup2", "awsUserdata-B");
        when(awsUserDataService.getUserData(ac, cloudStack)).thenReturn(userDataMap);

        Group hostgroup1 = mock(Group.class);
        when(hostgroup1.getName()).thenReturn("hostgroup1");
        when(hostgroup1.getType()).thenReturn(InstanceGroupType.GATEWAY);

        List<Group> groups = List.of(hostgroup1);
        when(cloudStack.getGroups()).thenReturn(groups);
        when(cloudStack.getUserDataByType(InstanceGroupType.GATEWAY)).thenReturn(userdata);

        assertThrows(IllegalStateException.class, () -> underTest.syncUserData(ac, cloudStack, resources));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void testSyncUserDataUpdateIsNeeded(String testCaseName, String gatewayUserdataFromDatabase, String coreUserdataFromDatabase, boolean shouldSync) {
        List<CloudResource> resources = List.of();
        Map<String, String> userDataMap = Map.of("hostgroup1", "awsUserdata-A", "hostgroup2", "awsUserdata-B");
        when(awsUserDataService.getUserData(ac, cloudStack)).thenReturn(userDataMap);

        Group hostgroup1 = mock(Group.class);
        when(hostgroup1.getName()).thenReturn("hostgroup1");
        when(hostgroup1.getType()).thenReturn(InstanceGroupType.GATEWAY);

        Group hostgroup2 = mock(Group.class);
        when(hostgroup2.getName()).thenReturn("hostgroup2");
        when(hostgroup2.getType()).thenReturn(InstanceGroupType.CORE);

        List<Group> groups = List.of(hostgroup1, hostgroup2);
        when(cloudStack.getGroups()).thenReturn(groups);
        when(cloudStack.getUserDataByType(InstanceGroupType.GATEWAY)).thenReturn(gatewayUserdataFromDatabase);
        lenient().when(cloudStack.getGatewayUserData()).thenReturn(gatewayUserdataFromDatabase);
        when(cloudStack.getUserDataByType(InstanceGroupType.CORE)).thenReturn(coreUserdataFromDatabase);
        lenient().when(cloudStack.getCoreUserData()).thenReturn(coreUserdataFromDatabase);

        underTest.syncUserData(ac, cloudStack, resources);

        if (shouldSync) {
            ArgumentCaptor<Map<InstanceGroupType, String>> captor = ArgumentCaptor.forClass(Map.class);
            verify(awsUpdateService, times(1)).updateUserData(any(), any(), any(), captor.capture());
            Map<InstanceGroupType, String> capturedUserData = captor.getValue();
            assertThat(capturedUserData).containsExactlyInAnyOrderEntriesOf(
                    Map.of(InstanceGroupType.GATEWAY, gatewayUserdataFromDatabase,
                            InstanceGroupType.CORE, coreUserdataFromDatabase));
        } else {
            verify(awsUpdateService, never()).updateUserData(any(), any(), any(), any());
        }
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    public static Object[][] scenarios() {
        return new Object[][] {
            //testCaseName        gatewayUserdataFromDatabase                     coreUserdataFromDatabase             shouldSync
            { "userdata is up-to-date",      "awsUserdata-A",                     "awsUserdata-B",                     false },
            { "userdata is up-to-date",      "awsUserdata-A ",                     " awsUserdata-B",                   false },
            { "userdata changed on core",    "awsUserdata-A",                     "awsUserdata-B-changed-in-database", true },
            { "userdata changed on gateway", "awsUserdata-A-changed-in-database", "awsUserdata-B",                     true },
            { "both userdata changed",       "awsUserdata-A-changed-in-database", "awsUserdata-B-changed-in-database", true }
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on
}
