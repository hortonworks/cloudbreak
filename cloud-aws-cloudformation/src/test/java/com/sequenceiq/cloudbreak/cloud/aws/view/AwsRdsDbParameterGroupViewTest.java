package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

class AwsRdsDbParameterGroupViewTest {

    private static final String ENGINE_VERSION_PARAMETER_REGEX = ".*EngineVersionParameter.*Default\": \"(\\d*)\".*";

    private static final String DBSTACK_TEMPLATE_FILE = "templates/aws-cf-dbstack.ftl";

    private static final Pattern DEFAULT_ENGINE_VERSION = Pattern.compile(ENGINE_VERSION_PARAMETER_REGEX);

    @Test
    void getDBParameterGroupNameTestWhenNullServerId() {
        DatabaseServer databaseServer = DatabaseServer.builder().build();

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer, null);

        assertThat(underTest.getDBParameterGroupName()).isNull();
    }

    @Test
    void getDBParameterGroupNameTestWhenHasServerId() {
        DatabaseServer databaseServer = DatabaseServer.builder().withServerId("myserver").build();

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer, null);

        assertThat(underTest.getDBParameterGroupName()).isEqualTo("dpg-myserver");
    }

    @Test
    void getDBParameterGroupFamilyTest() {
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withEngine(DatabaseEngine.POSTGRESQL)
                .build();
        databaseServer.putParameter("engineVersion", "myEngineVersion");
        AwsRdsVersionOperations awsRdsVersionOperations = mock(AwsRdsVersionOperations.class);
        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer, awsRdsVersionOperations);

        underTest.getDBParameterGroupFamily();

        verify(awsRdsVersionOperations).getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "myEngineVersion");
    }

    @Test
    void getDBParameterGroupFamilyTestWhenNullEngine() {
        DatabaseServer databaseServer = DatabaseServer.builder().build();

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer, null);

        assertThat(underTest.getDBParameterGroupFamily()).isNull();
    }

}
