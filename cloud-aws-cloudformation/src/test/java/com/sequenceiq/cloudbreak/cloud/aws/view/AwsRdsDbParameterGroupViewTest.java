package com.sequenceiq.cloudbreak.cloud.aws.view;

import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsDbParameterGroupView.ENGINE_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

class AwsRdsDbParameterGroupViewTest {

    private static final String ENGINE_VERSION_PARAMETER_REGEX = ".*EngineVersionParameter.*Default\": \"(\\d*)\".*";

    private static final String DBSTACK_TEMPLATE_FILE = "templates/aws-cf-dbstack.ftl";

    private static final Pattern DEFAULT_ENGINE_VERSION = Pattern.compile(ENGINE_VERSION_PARAMETER_REGEX);

    @Test
    void getDBParameterGroupNameTestWhenNullServerId() {
        DatabaseServer databaseServer = DatabaseServer.builder().build();

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer);

        assertThat(underTest.getDBParameterGroupName()).isNull();
    }

    @Test
    void getDBParameterGroupNameTestWhenHasServerId() {
        DatabaseServer databaseServer = DatabaseServer.builder().serverId("myserver").build();

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer);

        assertThat(underTest.getDBParameterGroupName()).isEqualTo("dpg-myserver");
    }

    @Test
    void getDBParameterGroupFamilyTestWhenNullEngine() {
        DatabaseServer databaseServer = DatabaseServer.builder().build();

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer);

        assertThat(underTest.getDBParameterGroupFamily()).isNull();
    }

    // Note: There is no easy way to test the "bad engine variant" case as enum classes are final and hard to mock.

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndBadVersionFormat() {
        DatabaseServer databaseServer = DatabaseServer.builder().engine(DatabaseEngine.POSTGRESQL).build();
        databaseServer.putParameter(ENGINE_VERSION, "latest");

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer);

        assertThatCode(underTest::getDBParameterGroupFamily).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndMissingMinorVersion() {
        DatabaseServer databaseServer = DatabaseServer.builder().engine(DatabaseEngine.POSTGRESQL).build();
        databaseServer.putParameter(ENGINE_VERSION, "10");

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer);

        assertThat(underTest.getDBParameterGroupFamily()).isEqualTo("postgres10");
    }

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndMajorVersionNumericOverflow() {
        DatabaseServer databaseServer = DatabaseServer.builder().engine(DatabaseEngine.POSTGRESQL).build();
        databaseServer.putParameter(ENGINE_VERSION, "12345678901234567890.1");

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer);

        assertThatCode(underTest::getDBParameterGroupFamily).isInstanceOf(NumberFormatException.class);
    }

    static Object[][] unsupportedMajorVersionDataProvider() {
        return new Object[][]{
                // testCaseName version
                {"version 0.1", "0.1"},
                {"version 1.1", "1.1"},
                {"version 2.1", "2.1"},
                {"version 3.1", "3.1"},
                {"version 4.1", "4.1"},
                {"version 5.1", "5.1"},
                {"version 6.1", "6.1"},
                {"version 7.1", "7.1"},
                {"version 8.1", "8.1"},
                {"version 123.1", "123.1"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedMajorVersionDataProvider")
    void getDBParameterGroupFamilyTestWhenPgSqlAndUnsupportedMajorVersion(String testCaseName, String version) {
        DatabaseServer databaseServer = DatabaseServer.builder().engine(DatabaseEngine.POSTGRESQL).build();
        databaseServer.putParameter(ENGINE_VERSION, version);

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer);

        assertThatCode(underTest::getDBParameterGroupFamily).isInstanceOf(IllegalStateException.class);
    }

    static Object[][] validVersionDataProvider() {
        return new Object[][]{
                // testCaseName version familyExpected
                {"version 9.5", "9.5", "postgres9.5"},
                {"version 9.6", "9.6", "postgres9.6"},
                {"version 10.1", "10.1", "postgres10"},
                {"version 11.1", "11.1", "postgres11"},
                {"version 12.1", "12.1", "postgres12"},
                {"version 13.1", "13.1", "postgres13"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validVersionDataProvider")
    void getDBParameterGroupFamilyTestWhenPgSqlAndValidVersion(String testCaseName, String version, String familyExpected) {
        DatabaseServer databaseServer = DatabaseServer.builder().engine(DatabaseEngine.POSTGRESQL).build();
        databaseServer.putParameter(ENGINE_VERSION, version);

        AwsRdsDbParameterGroupView underTest = new AwsRdsDbParameterGroupView(databaseServer);

        assertThat(underTest.getDBParameterGroupFamily()).isEqualTo(familyExpected);
    }

    private String getDefaultEngineVersionFromTemplate() throws IOException {
        String template = getTemplate();
        Matcher engineVersionMatcher = DEFAULT_ENGINE_VERSION.matcher(template);
        String version = null;
        if (engineVersionMatcher.matches()) {
            version = engineVersionMatcher.group(1);
        }
        return version;
    }

    private String getTemplate() throws IOException {
        String template;
        try (BufferedReader bs = new BufferedReader(
                new InputStreamReader(AwsRdsDbParameterGroupViewTest.class.getClassLoader().getResourceAsStream(DBSTACK_TEMPLATE_FILE)))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = bs.readLine()) != null) {
                out.append(line);
            }
            template = out.toString();
        }
        return template;
    }

}