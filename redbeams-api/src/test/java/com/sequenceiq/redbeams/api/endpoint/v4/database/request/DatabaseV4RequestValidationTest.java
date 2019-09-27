package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DatabaseV4RequestValidationTest {

    private static final String DATABASE_NAME = "mydb1";

    private static final String DATABASE_PROTOCOL = "jdbc:postgresql://";

    private static final String DATABASE_HOST_PORT_DB = "somedb.com:5432/mydb";

    private static final String DATABASE_USERNAME = "username";

    private static final String DATABASE_PASSWORD = "password";

    private static final String DATABASE_TYPE = "hive";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:cloudera:environment:myenv";

    private static ValidatorFactory validatorFactory;

    private static Validator validator;

    private final DatabaseV4Request underTest;

    private final Set<String> expectedErrorMessages;

    public DatabaseV4RequestValidationTest(
        String databaseName,
        String username,
        String password,
        String connectionUrl,
        String type,
        String environmentCrn,
        Set<String> expectedErrorMessages) {
        underTest = new DatabaseV4Request();

        underTest.setName(databaseName);
        underTest.setConnectionUserName(username);
        underTest.setConnectionPassword(password);
        underTest.setConnectionURL(connectionUrl);
        underTest.setType(type);
        underTest.setEnvironmentCrn(environmentCrn);

        this.expectedErrorMessages = expectedErrorMessages;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {
                DATABASE_NAME,
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                DATABASE_TYPE,
                ENVIRONMENT_CRN,
                Set.<String>of()
            },
            {
                Strings.repeat("a", 101),
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                DATABASE_TYPE,
                ENVIRONMENT_CRN,
                Set.<String>of("The length of the database's name must be between 5 to 100")
            },
            {
                "abc",
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                DATABASE_TYPE,
                ENVIRONMENT_CRN,
                Set.<String>of("The length of the database's name must be between 5 to 100")
            },
            {
                "a-@#$%|:&*;",
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                DATABASE_TYPE,
                ENVIRONMENT_CRN,
                Set.<String>of("The database's name may only contain lowercase characters")
            },
            {
                DATABASE_NAME,
                null,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                DATABASE_TYPE,
                ENVIRONMENT_CRN,
                Set.<String>of("must not be null")
            },
            {
                DATABASE_NAME,
                DATABASE_USERNAME,
                null,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                DATABASE_TYPE,
                ENVIRONMENT_CRN,
                Set.<String>of("must not be null")
            },
            {
                DATABASE_NAME,
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_HOST_PORT_DB,
                DATABASE_TYPE,
                ENVIRONMENT_CRN,
                Set.<String>of("JDBC connection URL is not valid", "Could not determine database vendor from JDBC URL")
            },
            {
                DATABASE_NAME,
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                Strings.repeat("a", 57),
                ENVIRONMENT_CRN,
                Set.<String>of("The length of the database's type must be between 3 and 56")
            },
            {
                DATABASE_NAME,
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                "ab",
                ENVIRONMENT_CRN,
                Set.<String>of("The length of the database's type must be between 3 and 56")
            },
            {
                DATABASE_NAME,
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                "a-@#$%|:&*;",
                ENVIRONMENT_CRN,
                Set.<String>of("The database's type may only contain alphanumeric characters")
            },
            {
                DATABASE_NAME,
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                DATABASE_TYPE,
                null,
                Set.<String>of("must not be null")
            },
            {
                DATABASE_NAME,
                DATABASE_USERNAME,
                DATABASE_PASSWORD,
                DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                DATABASE_TYPE,
                "abc",
                Set.<String>of("Invalid crn provided")
            }
        });
    }

    @BeforeClass
    public static void setUpClass() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterClass
    public static void tearDownClass() {
        validatorFactory.close();
    }

    @Test
    public void testValidation() {
        Set<ConstraintViolation<DatabaseV4Request>> violations = validator.validate(underTest);

        if (expectedErrorMessages.isEmpty()) {
            assertTrue(violations.isEmpty());
        } else {
            assertEquals(expectedErrorMessages.size(), violations.size());

            for (ConstraintViolation<DatabaseV4Request> violation : violations) {
                String violationMessage = violation.getMessage();
                assertTrue("Unexpected message: " + violationMessage,
                    expectedErrorMessages.stream().anyMatch(m -> violationMessage.contains(m)));
            }
        }
    }

}
