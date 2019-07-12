package com.sequenceiq.redbeams.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;

import java.util.Arrays;

import javax.validation.ConstraintValidatorContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

@RunWith(Parameterized.class)
public class ConnectorJarUrlForDatabaseVendorValidatorTest {

    private static final String CONNECTOR_JAR_URL = "http://drivers.example.com/mydriver.jar";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    private final Class requestClass;

    private final DatabaseVendor databaseVendor;

    private final boolean connectorJarUrlProvided;

    private final boolean expectedResult;

    private ConnectorJarUrlForDatabaseVendorValidator underTest;

    public ConnectorJarUrlForDatabaseVendorValidatorTest(Class requestClass, DatabaseVendor databaseVendor,
        boolean connectorJarUrlProvided, boolean expectedResult) {
        this.requestClass = requestClass;
        this.databaseVendor = databaseVendor;
        this.connectorJarUrlProvided = connectorJarUrlProvided;
        this.expectedResult = expectedResult;
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new ConnectorJarUrlForDatabaseVendorValidator();
    }

    @Parameters(name = "{index}: request type: {0} database vendor: {1} connector JAR URL provided: {2} should return: {3}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { DatabaseV4Request.class, DatabaseVendor.POSTGRES, true, true },
            { DatabaseV4Request.class, DatabaseVendor.POSTGRES, false, true },
            { DatabaseV4Request.class, DatabaseVendor.MYSQL, true, true },
            { DatabaseV4Request.class, DatabaseVendor.MYSQL, false, false },
            { DatabaseServerV4Request.class, DatabaseVendor.POSTGRES, true, true },
            { DatabaseServerV4Request.class, DatabaseVendor.POSTGRES, false, true },
            { DatabaseServerV4Request.class, DatabaseVendor.MYSQL, true, true },
            { DatabaseServerV4Request.class, DatabaseVendor.MYSQL, false, false },
            { com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request.class, DatabaseVendor.POSTGRES, true, true },
            { com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request.class, DatabaseVendor.POSTGRES, false, true },
            { com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request.class, DatabaseVendor.MYSQL, true, true },
            { com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request.class, DatabaseVendor.MYSQL, false, false }
        });
    }

    @Test
    public void testValidation() {
        Object request;
        if (requestClass.equals(DatabaseV4Request.class)) {
            DatabaseV4Request req = new DatabaseV4Request();
            req.setConnectorJarUrl(connectorJarUrlProvided ? CONNECTOR_JAR_URL : null);
            req.setConnectionURL("jdbc:" + databaseVendor.jdbcUrlDriverId() + ":etc");
            request = req;
        } else if (requestClass.equals(DatabaseServerV4Request.class)) {
            DatabaseServerV4Request req = new DatabaseServerV4Request();
            req.setConnectorJarUrl(connectorJarUrlProvided ? CONNECTOR_JAR_URL : null);
            req.setDatabaseVendor(databaseVendor.databaseType());
            request = req;
        } else {
            com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request req =
                new com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request();
            req.setConnectorJarUrl(connectorJarUrlProvided ? CONNECTOR_JAR_URL : null);
            req.setDatabaseVendor(databaseVendor.databaseType());
            request = req;
        }

        assertEquals(expectedResult, underTest.isValid(request, context));

        int expectedTimes = expectedResult ? 0 : 1;
        verify(context, times(expectedTimes)).buildConstraintViolationWithTemplate(any(String.class));
    }

}
