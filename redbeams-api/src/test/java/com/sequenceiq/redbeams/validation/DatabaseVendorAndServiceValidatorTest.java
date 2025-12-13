package com.sequenceiq.redbeams.validation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.MYSQL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE11;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE12;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.POSTGRES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;

class DatabaseVendorAndServiceValidatorTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    private DatabaseVendorAndServiceValidator underTest;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new DatabaseVendorAndServiceValidator();
    }

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("HIVE", POSTGRES, true),
                Arguments.of("HIVE", ORACLE11, true),
                Arguments.of("HIVE", ORACLE12, true),
                Arguments.of("HIVE", MYSQL, true),
                Arguments.of("hive", POSTGRES, true),
                Arguments.of("Hive", POSTGRES, true),
                Arguments.of("RANGER", POSTGRES, true),
                Arguments.of("RANGER", ORACLE11, true),
                Arguments.of("RANGER", ORACLE12, true),
                Arguments.of("RANGER", MYSQL, true),
                Arguments.of("Ranger", POSTGRES, true),
                Arguments.of("ranger", POSTGRES, true),
                Arguments.of("OOZIE", POSTGRES, true),
                Arguments.of("OOZIE", ORACLE11, true),
                Arguments.of("OOZIE", ORACLE12, true),
                Arguments.of("OOZIE", MYSQL, true),
                Arguments.of("Oozie", POSTGRES, true),
                Arguments.of("oozie", POSTGRES, true),
                Arguments.of("DRUID", POSTGRES, true),
                Arguments.of("DRUID", ORACLE11, false),
                Arguments.of("DRUID", ORACLE12, false),
                Arguments.of("DRUID", MYSQL, true),
                Arguments.of("Druid", POSTGRES, true),
                Arguments.of("druid", POSTGRES, true),
                Arguments.of("SUPERSET", POSTGRES, true),
                Arguments.of("SUPERSET", ORACLE11, false),
                Arguments.of("SUPERSET", ORACLE12, false),
                Arguments.of("SUPERSET", MYSQL, true),
                Arguments.of("superset", POSTGRES, true),
                Arguments.of("Superset", POSTGRES, true),
                Arguments.of("OTHER", POSTGRES, true),
                Arguments.of("OTHER", ORACLE11, true),
                Arguments.of("OTHER", ORACLE12, true),
                Arguments.of("OTHER", MYSQL, true),
                Arguments.of("Other", POSTGRES, true),
                Arguments.of("other", POSTGRES, true),
                Arguments.of("AMBARI", POSTGRES, true),
                Arguments.of("AMBARI", ORACLE11, false),
                Arguments.of("AMBARI", ORACLE12, false),
                Arguments.of("AMBARI", MYSQL, true),
                Arguments.of("Ambari", POSTGRES, true),
                Arguments.of("ambari", POSTGRES, true),
                Arguments.of("custom", POSTGRES, true),
                Arguments.of("Custom", POSTGRES, true),
                Arguments.of("cUSTOM", POSTGRES, true)
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testIsValid(String serviceName, DatabaseVendor databaseVendor, boolean expectedResult) {
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
