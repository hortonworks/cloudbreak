package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.exceptionConsumer;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.it.cloudbreak.newway.assertion.DatabaseExistsAssertion;
import com.sequenceiq.it.cloudbreak.newway.assertion.DatabaseTestAccessDeniedAssertion;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseTestEntity;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class DatabaseTest extends AbstractIntegrationTest {

    private static final String TEST_CONTEXT = "testContext";

    private static final String DB_TYPE_PROVIDER = "databaseTypeTestProvider";

    private static final String INVALID_ATTRIBUTE_PROVIDER = "databaseInvalidAttirbutesTestProvider";

    private static final String DATABASE_PROTOCOL = "jdbc:postgresql://";

    private static final String DATABASE_HOST_PORT_DB = "somedb.com:5432/mydb";

    private static final String DATABASE_USERNAME = "username";

    private static final String DATABASE_PASSWORD = "password";

    private static final String BAD_REQUEST_KEY = "badRequest";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDeleteCreate(TestContext testContext) {
        String databaseName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(DatabaseEntity.class)
                    .withName(databaseName)
                .when(DatabaseEntity.post())
                .when(DatabaseEntity.list())
                .then(DatabaseExistsAssertion.getAssertion(databaseName, 1))
                .when(DatabaseEntity.deleteV2())
                .when(DatabaseEntity.list())
                .then(DatabaseExistsAssertion.getAssertion(databaseName, 0))
                .when(DatabaseEntity.post())
                .when(DatabaseEntity.list())
                .then(DatabaseExistsAssertion.getAssertion(databaseName, 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateTwice(TestContext testContext) {
        String databaseName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(DatabaseEntity.class)
                    .withName(databaseName)
                .when(DatabaseEntity.post())
                .when(DatabaseEntity.list())
                .then(DatabaseExistsAssertion.getAssertion(databaseName, 1))
                .when(DatabaseEntity.post(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = DB_TYPE_PROVIDER)
    public void testCreateDatabaseWithTypeAndTestConnection(TestContext testContext, DatabaseType type) {
        String databaseName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(DatabaseEntity.class)
                    .withType(type.name())
                    .withName(databaseName)
                .when(DatabaseEntity.post())
                .when(DatabaseEntity.list())
                .then(DatabaseExistsAssertion.getAssertion(databaseName, 1))
                .given(DatabaseTestEntity.class)
                    .withExistingName(databaseName)
                .when(DatabaseTestEntity.testConnection())
                .validate();
    }

    @Test(dataProvider = INVALID_ATTRIBUTE_PROVIDER)
    public void testCreateDatabaseWithInvalidAttribute(TestContext testContext, String databaseName, String username,
            String password, String connectionUrl, String expectedErrorMessage) {
        testContext
                .given(DatabaseEntity.class)
                    .withName(databaseName)
                    .withConnectionUserName(username)
                    .withConnectionPassword(password)
                    .withConnectionURL(connectionUrl)
                .when(DatabaseEntity.post(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, exceptionConsumer(e -> {
                    assertThat(e.getMessage(), getErrorMessage(e), containsString(expectedErrorMessage));
                }).withKey(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = INVALID_ATTRIBUTE_PROVIDER)
    public void testDatabaseTestConnectionWithInvalidAttribute(TestContext testContext, String databaseName, String username,
            String password, String connectionUrl, String expectedErrorMessage) {
        testContext
                .given(DatabaseTestEntity.class)
                    .withRequest(new DatabaseV4Request())
                    .withName(databaseName)
                    .withConnectionUserName(username)
                    .withConnectionPassword(password)
                    .withConnectionURL(connectionUrl)
                    .withType("HIVE")
                .when(DatabaseTestEntity.testConnection(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, exceptionConsumer(e -> {
                    assertThat(e.getMessage(), getErrorMessage(e), containsString(expectedErrorMessage));
                }).withKey(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testDatabaseTestConnectionWithNonExistingDbName(TestContext testContext) {
        testContext
                .given(DatabaseTestEntity.class)
                    .withExistingName("aNonExistingDb")
                .when(DatabaseTestEntity.testConnection())
                .then(DatabaseTestAccessDeniedAssertion.getAssertion())
                .validate();
    }

    @DataProvider(name = DB_TYPE_PROVIDER)
    public Object[][] provideTypes() {
        List<DatabaseType> databaseTypeList = Arrays.asList(DatabaseType.values());
        Object[][] objects = new Object[databaseTypeList.size()][2];
        databaseTypeList
                .forEach(databaseType -> {
                    objects[databaseTypeList.indexOf(databaseType)][0] = applicationContext.getBean(TestContext.class);
                    objects[databaseTypeList.indexOf(databaseType)][1] = databaseType;
                });
        return objects;
    }

    @DataProvider(name = INVALID_ATTRIBUTE_PROVIDER)
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {applicationContext.getBean(TestContext.class), longStringGeneratorUtil.stringGenerator(51), DATABASE_USERNAME, DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB, "The length of the name has to be in range of 4 to 50"},
                {applicationContext.getBean(TestContext.class), "abc", DATABASE_USERNAME, DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB, "The length of the name has to be in range of 4 to 50"},
                {applicationContext.getBean(TestContext.class), "a-@#$%|:&*;", DATABASE_USERNAME, DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB, "The database's name can only contain lowercase alphanumeric characters and "
                        + "hyphens and has start with an alphanumeric character"},
                {applicationContext.getBean(TestContext.class), getNameGenerator().getRandomNameForMock(), null, DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB, "connectionUserName: null, error: must not be null"},
                {applicationContext.getBean(TestContext.class), getNameGenerator().getRandomNameForMock(), DATABASE_USERNAME, null,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB, "connectionPassword: null, error: must not be null"},
                {applicationContext.getBean(TestContext.class), getNameGenerator().getRandomNameForMock(), DATABASE_USERNAME, DATABASE_PASSWORD,
                        DATABASE_HOST_PORT_DB, "Unsupported database type"}
        };
    }
}
