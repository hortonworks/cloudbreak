package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsCustomParameterSupplier;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsEngineVersion;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsDbParameterGroupView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.rds.model.CreateDbParameterGroupResponse;
import software.amazon.awssdk.services.rds.model.ModifyDbParameterGroupResponse;
import software.amazon.awssdk.services.rds.model.Parameter;

@ExtendWith(MockitoExtension.class)
public class AwsRdsParameterGroupServiceTest {
    private static final String DB_SERVER_ID = "dbServerId";

    private static final String PARAMETER_GROUP_FAMILY = "parameterGroupFamily";

    @Mock
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @Mock
    private AwsRdsCustomParameterSupplier awsRdsCustomParameterSupplier;

    @Mock
    private AmazonRdsClient rdsClient;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private AuthenticatedContext ac;

    @InjectMocks
    private AwsRdsParameterGroupService underTest;

    @Test
    void testCreateParameterGroupWithCustomSettings() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(databaseServer.getServerId()).thenReturn(DB_SERVER_ID);
        when(databaseServer.getEngine()).thenReturn(DatabaseEngine.POSTGRESQL);
        when(cloudContext.getLocation()).thenReturn(location(region("eu-west-1"), availabilityZone("az")));
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(awsRdsVersionOperations.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "highestVersion")).thenReturn(PARAMETER_GROUP_FAMILY);
        String dbParameterGroupName = "dpg-" + DB_SERVER_ID + "-vhighestVersion";
        when(rdsClient.isDbParameterGroupPresent(dbParameterGroupName)).thenReturn(false);
        List<Parameter> customParameterList = List.of(Parameter.builder().build());
        when(awsRdsCustomParameterSupplier.getParametersToChange()).thenReturn(customParameterList);

        underTest.createParameterGroupWithCustomSettings(ac, rdsClient, databaseServer, new RdsEngineVersion("highestVersion"));

        verify(rdsClient).createParameterGroup(PARAMETER_GROUP_FAMILY, dbParameterGroupName, "DB parameter group for " + DB_SERVER_ID);
        verify(rdsClient).changeParameterInGroup(dbParameterGroupName, customParameterList);
    }

    @Test
    void testCreateParameterGroupWithCustomSettingsWhenParameterGroupIsFound() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(databaseServer.getServerId()).thenReturn(DB_SERVER_ID);
        when(databaseServer.getEngine()).thenReturn(DatabaseEngine.POSTGRESQL);
        when(awsRdsVersionOperations.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "highestVersion")).thenReturn(PARAMETER_GROUP_FAMILY);
        String dbParameterGroupName = "dpg-" + DB_SERVER_ID + "-vhighestVersion";
        when(rdsClient.isDbParameterGroupPresent(dbParameterGroupName)).thenReturn(true);

        underTest.createParameterGroupWithCustomSettings(ac, rdsClient, databaseServer, new RdsEngineVersion("highestVersion"));

        verify(rdsClient, never()).createParameterGroup(anyString(), anyString(), anyString());
        verify(rdsClient).changeParameterInGroup(eq(dbParameterGroupName), any());
    }

    @Test
    void testRemoveFormerParamGroupsWhenNoSsl() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        List<CloudResource> resources = List.of(CloudResource.builder().withType(ResourceType.RDS_DB_PARAMETER_GROUP).withName("resource1").build(),
                CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withName("resource2").build());
        List<CloudResource> actualResult = underTest.removeFormerParamGroups(rdsClient, databaseServer, resources);
        assertEquals(0, actualResult.size());
        verify(rdsClient, never()).deleteParameterGroup(anyString());
    }

    @Test
    void testRemoveFormerParamGroupsWhenSslEnabled() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(databaseServer.isUseSslEnforcement()).thenReturn(true);
        List<CloudResource> resources = List.of(
                CloudResource.builder().withType(ResourceType.RDS_DB_PARAMETER_GROUP).withName("paramgroup1").build(),
                CloudResource.builder().withType(ResourceType.RDS_DB_PARAMETER_GROUP).withName("paramgroup2").build(),
                CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withName("instance1").build());
        List<CloudResource> actualResult = underTest.removeFormerParamGroups(rdsClient, databaseServer, resources);
        assertEquals(2, actualResult.size());
        verify(rdsClient, times(1)).deleteParameterGroup("paramgroup1");
        verify(rdsClient, times(1)).deleteParameterGroup("paramgroup2");
    }

    @Test
    public void testApplySslEnforcementCreatesAndAssignsParameterGroup() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        CloudContext cloudContext = mock(CloudContext.class);
        CreateDbParameterGroupResponse createDbParameterGroupResponse = mock(CreateDbParameterGroupResponse.class);
        ModifyDbParameterGroupResponse modifyDbParameterGroupResponse = mock(ModifyDbParameterGroupResponse.class);
        String serverId = "test-server-id";
        String dbParameterGroupName = "test-db-parameter-group";
        String dbParameterGroupFamily = "test-db-parameter-group-family";

        when(databaseServer.getServerId()).thenReturn(serverId);
        when(cloudContext.getLocation()).thenReturn(location(region("eu-west-1"), availabilityZone("az")));
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(rdsClient.isDbParameterGroupPresent(anyString())).thenReturn(false);
        when(rdsClient.createParameterGroup(any(), anyString(), anyString()))
                .thenReturn(createDbParameterGroupResponse);
        AwsRdsDbParameterGroupView awsRdsDbParameterGroupView = mock(AwsRdsDbParameterGroupView.class);
        when(persistenceNotifier.notifyAllocation(any(), any())).thenReturn(new ResourcePersisted());
        when(awsRdsCustomParameterSupplier.getParametersToChange()).thenReturn(List.of());
        when(rdsClient.changeParameterInGroup(anyString(), any())).thenReturn(modifyDbParameterGroupResponse);

        String result = underTest.applySslEnforcement(ac, rdsClient, databaseServer);

        assertEquals("dpg-test-server-id", result);
        verify(persistenceNotifier, times(1)).notifyAllocation(any(), any());
    }

    @Test
    public void testApplySslEnforcementNoParameterGroupCreated() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        CloudContext cloudContext = mock(CloudContext.class);
        CreateDbParameterGroupResponse createDbParameterGroupResponse = mock(CreateDbParameterGroupResponse.class);
        ModifyDbParameterGroupResponse modifyDbParameterGroupResponse = mock(ModifyDbParameterGroupResponse.class);
        String serverId = "test-server-id";

        when(databaseServer.getServerId()).thenReturn(serverId);
        when(rdsClient.isDbParameterGroupPresent(anyString())).thenReturn(true);
        when(awsRdsCustomParameterSupplier.getParametersToChange()).thenReturn(List.of());
        when(rdsClient.changeParameterInGroup(anyString(), any())).thenReturn(modifyDbParameterGroupResponse);

        String result = underTest.applySslEnforcement(ac, rdsClient, databaseServer);

        assertEquals("dpg-test-server-id", result);
    }
}
