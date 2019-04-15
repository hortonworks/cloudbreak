package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.assertion.database.DatabaseTestAssertion.containsDatabaseName;
import static com.sequenceiq.it.cloudbreak.newway.assertion.database.DatabaseTestTestAssertion.validConnectionTest;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.it.cloudbreak.newway.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class DatabaseTest extends AbstractIntegrationTest {

    private static final String DB_TYPE_PROVIDER = "databaseTypeTestProvider";

    private static final String INVALID_ATTRIBUTE_PROVIDER = "databaseInvalidAttributesTestProvider";

    private static final String DATABASE_PROTOCOL = "jdbc:postgresql://";

    private static final String DATABASE_HOST_PORT_DB = "somedb.com:5432/mydb";

    private static final String DATABASE_USERNAME = "username";

    private static final String DATABASE_PASSWORD = "password";

    private static final Class<MockedTestContext> TEST_CONTEXT_CLASS = MockedTestContext.class;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a prepared database",
            when = "the database is deleted and then a create request is sent with the same database name",
            then = "the database should be created again")
    public void createAndDeleteAndCreateWithSameNameThenShouldRecreatedDatabase(TestContext testContext) {
        String databaseName = resourcePropertyProvider().getName();
        testContext
                .given(DatabaseTestDto.class)
                .withName(databaseName)
                .when(databaseTestClient.createV4(), key(databaseName))
                .when(databaseTestClient.listV4(), key(databaseName))
                .then(containsDatabaseName(databaseName, 1), key(databaseName))
                .when(databaseTestClient.deleteV4(), key(databaseName))
                .when(databaseTestClient.listV4(), key(databaseName))
                .then(containsDatabaseName(databaseName, 0), key(databaseName))
                .when(databaseTestClient.createV4(), key(databaseName))
                .when(databaseTestClient.listV4(), key(databaseName))
                .then(containsDatabaseName(databaseName, 1), key(databaseName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a prepared database",
            when = "when a database create request is sent with the same database name",
            then = "the create should return a BadRequestException")
    public void createAndCreateWithSameNameThenShouldThrowBadRequestException(TestContext testContext) {
        String databaseName = resourcePropertyProvider().getName();
        testContext
                .given(DatabaseTestDto.class)
                .withName(databaseName)
                .when(databaseTestClient.createV4(), key(databaseName))
                .when(databaseTestClient.listV4(), key(databaseName))
                .then(containsDatabaseName(databaseName, 1), key(databaseName))
                .when(databaseTestClient.createV4(), key(databaseName))
                .expect(BadRequestException.class, key(databaseName))
                .validate();
    }

    @Test(dataProvider = DB_TYPE_PROVIDER)
    public void testCreateDatabaseWithTypeAndTestConnection(
            TestContext testContext,
            DatabaseType type,
            @Description TestCaseDescription testCaseDescription) {
        String databaseName = resourcePropertyProvider().getName();
        testContext
                .given(DatabaseTestDto.class)
                .withType(type.name())
                .withName(databaseName)
                .when(databaseTestClient.createV4(), key(databaseName))
                .when(databaseTestClient.listV4(), key(databaseName))
                .then(containsDatabaseName(databaseName, 1), key(databaseName))
                .given(DatabaseTestTestDto.class)
                .withExistingName(databaseName)
                .when(databaseTestClient.testV4(), key(databaseName))
                .validate();
    }

    @Test(dataProvider = INVALID_ATTRIBUTE_PROVIDER)
    public void testCreateDatabaseWithInvalidAttribute(
            TestContext testContext,
            String databaseName,
            String username,
            String password,
            String connectionUrl,
            String expectedErrorMessage,
            @Description TestCaseDescription testCaseDescription) {
        String generatedKey = resourcePropertyProvider().getName();
        testContext
                .given(DatabaseTestDto.class)
                .withName(databaseName)
                .withConnectionUserName(username)
                .withConnectionPassword(password)
                .withConnectionURL(connectionUrl)
                .when(databaseTestClient.createV4(), key(generatedKey))
                .expect(BadRequestException.class,
                        expectedMessage(expectedErrorMessage)
                                .withKey(generatedKey))
                .validate();
    }

    @Test(dataProvider = INVALID_ATTRIBUTE_PROVIDER)
    public void testDatabaseTestConnectionWithInvalidAttribute(
            TestContext testContext,
            String databaseName,
            String username,
            String password,
            String connectionUrl,
            String expectedErrorMessage,
            @Description TestCaseDescription testCaseDescription) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given(DatabaseTestTestDto.class)
                .withRequest(new DatabaseV4Request())
                .withName(databaseName)
                .withConnectionUserName(username)
                .withConnectionPassword(password)
                .withConnectionURL(connectionUrl)
                .withType("HIVE")
                .when(databaseTestClient.testV4(), key(generatedKey))
                .expect(BadRequestException.class,
                        expectedMessage(expectedErrorMessage)
                                .withKey(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "calling test database endpoint with a non-existent database name",
            then = "the test connection should return access denied")
    public void testDatabaseTestConnectionWithNonExistingDbName(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given(DatabaseTestTestDto.class)
                .withExistingName("aNonExistentDb")
                .when(databaseTestClient.testV4(), key(generatedKey))
                .then(validConnectionTest(), key(generatedKey))
                .validate();
    }

    @DataProvider(name = DB_TYPE_PROVIDER)
    public Object[][] provideTypes() {
        List<DatabaseType> databaseTypeList = Arrays.asList(DatabaseType.values());
        Object[][] objects = new Object[databaseTypeList.size()][3];
        databaseTypeList
                .forEach(databaseType -> {
                    objects[databaseTypeList.indexOf(databaseType)][0] = getBean(TEST_CONTEXT_CLASS);
                    objects[databaseTypeList.indexOf(databaseType)][1] = databaseType;
                    objects[databaseTypeList.indexOf(databaseType)][2] =
                            new TestCaseDescription.TestCaseDescriptionBuilder()
                                    .given("there is a running cloudbreak")
                                    .when("sending database create request with databaseType '" + databaseType + '\'')
                                    .then("creation should be successful");
                });
        return objects;
    }

    @DataProvider(name = INVALID_ATTRIBUTE_PROVIDER)
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {
                        getBean(TEST_CONTEXT_CLASS),
                        getLongNameGenerator().stringGenerator(51),
                        DATABASE_USERNAME, DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        "The length of the name has to be in range of 4 to 50",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("there is a running cloudbreak")
                                .when("calling database create endpoint with 51 characters long name")
                                .then("a BadRequestException should be returned")
                },
                {
                        getBean(TEST_CONTEXT_CLASS),
                        "abc",
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        "The length of the name has to be in range of 4 to 50",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("there is a running cloudbreak")
                                .when("calling database create endpoint with 3 characcters long name")
                                .then("a BadRequestException should be returned")
                },
                {
                        getBean(TEST_CONTEXT_CLASS),
                        "a-@#$%|:&*;",
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        "The database's name can only contain lowercase alphanumeric characters and "
                                + "hyphens and has start with an alphanumeric character",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("there is a running cloudbreak")
                                .when("calling database create endpoint with invalid characters in the name")
                                .then("a BadRequestException should be returned")
                },
                {
                        getBean(TEST_CONTEXT_CLASS),
                        resourcePropertyProvider().getName(),
                        null,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        "must not be null",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("there is a running cloudbreak")
                                .when("calling database create endpoint with 'null' connectionUserName")
                                .then("a BadRequestException should be returned")
                },
                {
                        getBean(TEST_CONTEXT_CLASS),
                        resourcePropertyProvider().getName(),
                        DATABASE_USERNAME,
                        null,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        "must not be null",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("there is a running cloudbreak")
                                .when("calling database create endpoint with 'null' connectionPassword")
                                .then("a BadRequestException should be returned")
                },
                {
                        getBean(TEST_CONTEXT_CLASS),
                        resourcePropertyProvider().getName(),
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_HOST_PORT_DB,
                        "Unsupported database type",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("there is a running cloudbreak")
                                .when("calling database create endpoint with unsupported database type")
                                .then("a BadRequestException should be returned")
                }
        };
    }
}
