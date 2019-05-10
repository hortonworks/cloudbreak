package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;

public class RDSConfigJsonValidatorTest {

    private static final String VALID_CONNECTION_URL = "jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/test";

    private static final String VALID_CONNECTION_URL_WITH_GETPARAM = "jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306/hivedb?useSSL=true&requireSSL=false";

    private static final String VALID_JAR_URL = "https://someurl.com";

    private static final String VALID_NAME = "some-valid-name";

    private static final String TEST_TYPE = "HIVE";

    private static final int NOT_EVEN_ONCE = 0;

    private static final int ONCE = 1;

    private RDSConfigJsonValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ConstraintViolationBuilder constraintViolationBuilder;

    @Mock
    private NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    @Mock
    private DatabaseV4Base json;

    @Before
    public void setUp() {
        underTest = new RDSConfigJsonValidator();
        initMocks(this);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilderCustomizableContext);
        when(nodeBuilderCustomizableContext.addConstraintViolation()).thenReturn(constraintValidatorContext);
    }

    @Test
    public void testIsValidWhenConnectionUrlIsValidThenTrueReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertTrue(underTest.isValid(json, constraintValidatorContext));
    }

    @Test
    public void testIsValidWhenConnectionUrlContainsHyphenInMiddleInDatabaseNameThenTrueReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql://somedb.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/mydb-asd");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertTrue(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(NOT_EVEN_ONCE)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testIsValidWhenConnectionUrlContainsGetParametersThenTrueReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL_WITH_GETPARAM);
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertTrue(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(NOT_EVEN_ONCE)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testIsValidWhenConnectionUrlContainsHyphenAtTheEndInDatabaseNameThenTrueReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql://somedb.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/mydbasd-");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertTrue(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(NOT_EVEN_ONCE)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testIsValidWhenConnectionUrlContainsHyphenAtStartInDatabaseNameThenFalseReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql://somedb.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/-mydbasd");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("Wrong host, port or table name. Valid form: host:1234/tablename or host:1234:tablename");
    }

    @Test
    public void testIsValidWhenConnectionUrlContainsHyphenAtDifferentPlacesInDatabaseNameThenTrueReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql://somedb.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/my-db-asd");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertTrue(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(NOT_EVEN_ONCE)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testIsValidWhenConnectionUrlContainsDoubleHyphenInDatabaseNameThenTrueReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql://somedb.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/mydb--asd");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertTrue(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(NOT_EVEN_ONCE)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testIsValidWhenConnectionUrlEndsWithDoubleHyphenInDatabaseNameThenTrueReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql://somedb.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/mydbasd--");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertTrue(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(NOT_EVEN_ONCE)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testIsValidWhenConnectionUrlContainsDoubleAndSingleHyphenInDifferentPlacesInDatabaseNameThenTrueReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql://somedb.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/my--db-asd");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertTrue(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(NOT_EVEN_ONCE)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testIsValidWhenConnectionUrlHasDatabaseNameWhichContainsOnlyHyphenThenFalseReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql://somedb.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/-");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("Wrong host, port or table name. Valid form: host:1234/tablename or host:1234:tablename");
    }

    @Test
    public void testIsValidWhenConnectionUrlIsInvalidDueToMissingEndingThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql::test.eu-west-1.rds.amazonaws.com:5432");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("Unknown separator. Valid formation: jdbc:postgresql://host:1234/tablename "
                        + "or jdbc:oracle:thin:@host:1234:tablename");
    }

    @Test
    public void testIsValidWhenConnectionUrlIsInvalidDueToWrongDbTypeThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:smalldog://test.eu-west-1.rds.amazonaws.com:5432/test");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("Unsupported database type. Supported databases: PostgreSQL, Oracle, MySQL.");
    }

    @Test
    public void testIsValidWhenJdbcUrlWithoutJdbcPrefixThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn("test.eu-west-1.rds.amazonaws.com:5432");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("Unsupported database type. Supported databases: PostgreSQL, Oracle, MySQL.");
    }

    @Test
    public void testIsValidWhenConnectionUrlIsInvalidDueToWrongPortThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:oracle:thin:@test.eu-west-1.rds.amazonaws.com:test");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("Wrong host, port or table name. Valid form: host:1234/tablename or host:1234:tablename");
    }

    @Test
    public void testIsValidWhenJdbcUrlWithoutTableNameThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432");
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("Wrong host, port or table name. Valid form: host:1234/tablename or host:1234:tablename");
    }

    @Test
    public void testIsValidWhenNameIsInvalidDueToMismatchingFormatThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn("1-dssd23asd+?");
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("The name can only contain lowercase alphanumeric characters and hyphens "
                        + "and has start with an alphanumeric character");
    }

    @Test
    public void testIsValidWhenNameIsInvalidBecauseItsTooShortThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn("abc");
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE)).buildConstraintViolationWithTemplate("The length of the name has to be in range of 4 to 50");
    }

    @Test
    public void testIsValidWhenNameIsInvalidBecauseItsTooLongThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn("abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabdsdsdddsdsssd");
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE)).buildConstraintViolationWithTemplate("The length of the name has to be in range of 4 to 50");
    }

    @Test
    public void testIsValidWhenTypeHasNotMatchesToTheExpectedPatternThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn("ab?cd-_:2ds");
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("The type can only contain alphanumeric characters and hyphens "
                        + "and has start with an alphanumeric character. The length of the name has to be in range of 3 to 12");
    }

    @Test
    public void testIsValidWhenTypeHasNotEnoughLengthThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn("RA");
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE)).buildConstraintViolationWithTemplate("The length of the type has to be in range of 3 to 56");
    }

    @Test
    public void testIsValidWhenTypeIsTooLongThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn("CLOUDERA_MANAGER_MANAGEMENT_SERVICE_NAVIGATOR_METASERVER_AND_EVEN_MORE");
        when(json.getConnectorJarUrl()).thenReturn(VALID_JAR_URL);

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE)).buildConstraintViolationWithTemplate("The length of the type has to be in range of 3 to 56");
    }

    @Test
    public void testIsValidWhenConnectorJarUrlDoesNotMatchesToTheExpectedPatternThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn("some invalid url input, like: ttps://someurl.com");

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE)).buildConstraintViolationWithTemplate("The URL must be proper and valid!");
    }

    @Test
    public void testIsValidWhenConnectorJarUrlIsTooLongThenRelatedWarningMessageShouldBeStoredAsViolationAndFalseReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn("https://someurlwhichistoolongtoprocessorstoreorwhateverbutthislengthshoulddefinitelyleadthevalidation"
                + "processtofailedsoyoucantestthevalidationwhiththis.com");

        assertFalse(underTest.isValid(json, constraintValidatorContext));
        verify(constraintValidatorContext, times(ONCE))
                .buildConstraintViolationWithTemplate("The length of the connectorJarUrl has to be in range of 0 to 150");
    }

    @Test
    public void testIsValidWhenConnectorJarUrlIsEmptyThenTrueReturns() {
        when(json.getConnectionURL()).thenReturn(VALID_CONNECTION_URL);
        when(json.getName()).thenReturn(VALID_NAME);
        when(json.getType()).thenReturn(TEST_TYPE);
        when(json.getConnectorJarUrl()).thenReturn(null);

        assertTrue(underTest.isValid(json, constraintValidatorContext));
    }

}