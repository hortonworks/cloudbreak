package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;

class DatabaseV4RequestValidationTest {

    private static final String DATABASE_NAME = "mydb1";

    private static final String DATABASE_PROTOCOL = "jdbc:postgresql://";

    private static final String DATABASE_HOST_PORT_DB = "somedb.com:5432/mydb";

    private static final String DATABASE_USERNAME = "username";

    private static final String DATABASE_PASSWORD = "password";

    private static final String DATABASE_TYPE = "hive";

    private static final String ENVIRONMENT_CRN = CrnTestUtil.getEnvironmentCrnBuilder()
            .setAccountId("cloudera")
            .setResource("myenv")
            .build()
            .toString();

    private static ValidatorFactory validatorFactory;

    private static Validator validator;

    private DatabaseV4Request underTest;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(
                        DATABASE_NAME,
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        DATABASE_TYPE,
                        ENVIRONMENT_CRN,
                        Set.<String>of()
                ),
                Arguments.of(
                        Strings.repeat("a", 101),
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        DATABASE_TYPE,
                        ENVIRONMENT_CRN,
                        Set.<String>of("The length of the database's name must be between 5 to 100")
                ),
                Arguments.of(
                        "abc",
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        DATABASE_TYPE,
                        ENVIRONMENT_CRN,
                        Set.<String>of("The length of the database's name must be between 5 to 100")
                ),
                Arguments.of(
                        "a-@#$%|:&*;",
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        DATABASE_TYPE,
                        ENVIRONMENT_CRN,
                        Set.<String>of("The database's name may only contain lowercase characters")
                ),
                Arguments.of(
                        DATABASE_NAME,
                        null,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        DATABASE_TYPE,
                        ENVIRONMENT_CRN,
                        Set.<String>of("must not be null")
                ),
                Arguments.of(
                        DATABASE_NAME,
                        DATABASE_USERNAME,
                        null,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        DATABASE_TYPE,
                        ENVIRONMENT_CRN,
                        Set.<String>of("must not be null")
                ),
                Arguments.of(
                        DATABASE_NAME,
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_HOST_PORT_DB,
                        DATABASE_TYPE,
                        ENVIRONMENT_CRN,
                        Set.<String>of("JDBC connection URL is not valid", "Could not determine database vendor from JDBC URL")
                ),
                Arguments.of(
                        DATABASE_NAME,
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        Strings.repeat("a", 57),
                        ENVIRONMENT_CRN,
                        Set.<String>of("The length of the database's type must be between 3 and 56")
                ),
                Arguments.of(
                        DATABASE_NAME,
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        "ab",
                        ENVIRONMENT_CRN,
                        Set.<String>of("The length of the database's type must be between 3 and 56")
                ),
                Arguments.of(
                        DATABASE_NAME,
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        "a-@#$%|:&*;",
                        ENVIRONMENT_CRN,
                        Set.<String>of("The database's type may only contain alphanumeric characters")
                ),
                Arguments.of(
                        DATABASE_NAME,
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        DATABASE_TYPE,
                        null,
                        Set.<String>of("must not be null")
                ),
                Arguments.of(
                        DATABASE_NAME,
                        DATABASE_USERNAME,
                        DATABASE_PASSWORD,
                        DATABASE_PROTOCOL + DATABASE_HOST_PORT_DB,
                        DATABASE_TYPE,
                        "abc",
                        Set.<String>of("Invalid Crn was provided. 'abc' does not match the Crn pattern")
                ));
    }

    @BeforeAll
    public static void setUpClass() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    public static void tearDownClass() {
        validatorFactory.close();
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testValidation(String databaseName, String username, String password, String connectionUrl, String type, String environmentCrn,
            Set<String> expectedErrorMessages) {
        underTest = new DatabaseV4Request();
        underTest.setName(databaseName);
        underTest.setConnectionUserName(username);
        underTest.setConnectionPassword(password);
        underTest.setConnectionURL(connectionUrl);
        underTest.setType(type);
        underTest.setEnvironmentCrn(environmentCrn);

        Set<ConstraintViolation<DatabaseV4Request>> violations = validator.validate(underTest);

        if (expectedErrorMessages.isEmpty()) {
            assertTrue(violations.isEmpty());
        } else {
            assertEquals(expectedErrorMessages.size(), violations.size());

            for (ConstraintViolation<DatabaseV4Request> violation : violations) {
                String violationMessage = violation.getMessage();
                assertTrue(expectedErrorMessages.stream().anyMatch(violationMessage::contains),
                        "Unexpected message: " + violationMessage + ", expected: " + String.join(", ", expectedErrorMessages));
            }
        }
    }

}
