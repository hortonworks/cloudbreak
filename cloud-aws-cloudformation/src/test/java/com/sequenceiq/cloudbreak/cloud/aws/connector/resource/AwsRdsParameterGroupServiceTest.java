package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.rds.model.Parameter;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsCustomParameterSupplier;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsEngineVersion;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.common.api.type.ResourceType;

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

    @InjectMocks
    private AwsRdsParameterGroupService underTest;

    @Test
    void testCreateParameterGroupWithCustomSettings() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(databaseServer.getServerId()).thenReturn(DB_SERVER_ID);
        when(databaseServer.getEngine()).thenReturn(DatabaseEngine.POSTGRESQL);
        when(awsRdsVersionOperations.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "highestVersion")).thenReturn(PARAMETER_GROUP_FAMILY);
        String dbParameterGroupName = "dpg-" + DB_SERVER_ID + "-vhighestVersion";
        when(rdsClient.isDbParameterGroupPresent(dbParameterGroupName)).thenReturn(false);
        List<Parameter> customParameterList = List.of(new Parameter());
        when(awsRdsCustomParameterSupplier.getParametersToChange()).thenReturn(customParameterList);

        underTest.createParameterGroupWithCustomSettings(rdsClient, databaseServer, new RdsEngineVersion("highestVersion"));

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

        underTest.createParameterGroupWithCustomSettings(rdsClient, databaseServer, new RdsEngineVersion("highestVersion"));

        verify(rdsClient, never()).createParameterGroup(anyString(), anyString(), anyString());
        verify(rdsClient).changeParameterInGroup(eq(dbParameterGroupName), any());
    }

    @Test
    void testRemoveFormerParamGroupsWhenNoSsl() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().withType(ResourceType.RDS_DB_PARAMETER_GROUP).withName("resource1").build(),
                new CloudResource.Builder().withType(ResourceType.AWS_INSTANCE).withName("resource2").build());
        List<CloudResource> actualResult = underTest.removeFormerParamGroups(rdsClient, databaseServer, resources);
        Assertions.assertEquals(0, actualResult.size());
        verify(rdsClient, never()).deleteParameterGroup(anyString());
    }

    @Test
    void testRemoveFormerParamGroupsWhenSslEnabled() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(databaseServer.isUseSslEnforcement()).thenReturn(true);
        List<CloudResource> resources = List.of(
                new CloudResource.Builder().withType(ResourceType.RDS_DB_PARAMETER_GROUP).withName("paramgroup1").build(),
                new CloudResource.Builder().withType(ResourceType.RDS_DB_PARAMETER_GROUP).withName("paramgroup2").build(),
                new CloudResource.Builder().withType(ResourceType.AWS_INSTANCE).withName("instance1").build());
        List<CloudResource> actualResult = underTest.removeFormerParamGroups(rdsClient, databaseServer, resources);
        Assertions.assertEquals(2, actualResult.size());
        verify(rdsClient, times(1)).deleteParameterGroup("paramgroup1");
        verify(rdsClient, times(1)).deleteParameterGroup("paramgroup2");
    }
}
