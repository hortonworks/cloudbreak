package com.sequenceiq.redbeams.validation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.MYSQL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE11;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE12;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.POSTGRES;
import static org.junit.Assert.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Optional;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;

@RunWith(Parameterized.class)
public class DatabaseVendorAndServiceValidatorTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    private final String serviceName;

    private final DatabaseVendor databaseVendor;

    private final boolean expectedResult;

    private DatabaseVendorAndServiceValidator underTest;

    public DatabaseVendorAndServiceValidatorTest(String serviceName, DatabaseVendor databaseVendor, boolean expectedResult) {
        this.serviceName = serviceName;
        this.databaseVendor = databaseVendor;
        this.expectedResult = expectedResult;
    }

    @Before
    public void setUp() {
        initMocks(this);
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new DatabaseVendorAndServiceValidator();
    }

    @Parameters(name = "{index}: service: {0} database vendor: {1} should return {2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "HIVE",      POSTGRES,    true },
                { "HIVE",      ORACLE11,    true },
                { "HIVE",      ORACLE12,    true },
                { "HIVE",      MYSQL,       true },
                { "hive",      POSTGRES,    true },
                { "Hive",      POSTGRES,    true },
                { "RANGER",    POSTGRES,    true },
                { "RANGER",    ORACLE11,    true },
                { "RANGER",    ORACLE12,    true },
                { "RANGER",    MYSQL,       true },
                { "Ranger",    POSTGRES,    true },
                { "ranger",    POSTGRES,    true },
                { "OOZIE",     POSTGRES,    true },
                { "OOZIE",     ORACLE11,    true },
                { "OOZIE",     ORACLE12,    true },
                { "OOZIE",     MYSQL,       true },
                { "Oozie",     POSTGRES,    true },
                { "oozie",     POSTGRES,    true },
                { "DRUID",     POSTGRES,    true },
                { "DRUID",     ORACLE11,    false },
                { "DRUID",     ORACLE12,    false },
                { "DRUID",     MYSQL,       true },
                { "Druid",     POSTGRES,    true },
                { "druid",     POSTGRES,    true },
                { "SUPERSET",  POSTGRES,    true },
                { "SUPERSET",  ORACLE11,    false },
                { "SUPERSET",  ORACLE12,    false },
                { "SUPERSET",  MYSQL,       true },
                { "superset",  POSTGRES,    true },
                { "Superset",  POSTGRES,    true },
                { "OTHER",     POSTGRES,    true },
                { "OTHER",     ORACLE11,    true },
                { "OTHER",     ORACLE12,    true },
                { "OTHER",     MYSQL,       true },
                { "Other",     POSTGRES,    true },
                { "other",     POSTGRES,    true },
                { "AMBARI",    POSTGRES,    true },
                { "AMBARI",    ORACLE11,    false },
                { "AMBARI",    ORACLE12,    false },
                { "AMBARI",    MYSQL,       true },
                { "Ambari",    POSTGRES,    true },
                { "ambari",    POSTGRES,    true },
                { "custom",    POSTGRES,    true },
                { "Custom",    POSTGRES,    true },
                { "cUSTOM",    POSTGRES,    true }
        });
    }

    @Test
    public void testIsValid() {
        DatabaseV4Request request = new DatabaseV4Request();
        request.setType(serviceName);
        request.setConnectionURL(new DatabaseCommon().getJdbcConnectionUrl(databaseVendor.jdbcUrlDriverId(),
            "mydb.example.com",
            1234,
            Optional.of(serviceName)));

        assertEquals(expectedResult, underTest.isValid(request, context));

        int expectedTimes = expectedResult ? 0 : 1;
        verify(context, times(expectedTimes)).buildConstraintViolationWithTemplate(any(String.class));
    }
}
