package com.sequenceiq.cloudbreak.externaldatabase;

import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.MYSQL;
import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.ORACLE11;
import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.ORACLE12;
import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.POSTGRES;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.validation.externaldatabase.RdsRequestValidator;

@RunWith(Parameterized.class)
public class RdsRequestValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ConstraintViolationBuilder constraintViolationBuilder;

    @Mock
    private NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    private final RdsRequestValidator rdsRequestValidator = new RdsRequestValidator();

    private final String serviceName;

    private final DatabaseVendor databaseVendor;

    private final boolean result;

    public RdsRequestValidatorTest(String serviceName, DatabaseVendor databaseVendor, boolean result) {
        this.serviceName = serviceName;
        this.databaseVendor = databaseVendor;
        this.result = result;
    }

    @Before
    public void before() {
        initMocks(this);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilderCustomizableContext);
        when(nodeBuilderCustomizableContext.addConstraintViolation()).thenReturn(constraintValidatorContext);
    }

    @Parameters(name = "{index}: service: {0} database: {1} should return {2}")
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
    public void test() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest(serviceName, databaseVendor);
        boolean valid = rdsRequestValidator.isValid(rdsConfigRequest, constraintValidatorContext);
        Assert.assertEquals(result, valid);
    }

    private RDSConfigRequest rdsConfigRequest(String serviceName, DatabaseVendor databaseVendor) {
        RDSConfigRequest rdsConfigRequest = new RDSConfigRequest();
        rdsConfigRequest.setType(serviceName);
        rdsConfigRequest.setConnectionURL(String.format("jdbc:%s:dsfdsdfsdfsdfdsf:%s.host.com:5432/%s",
                databaseVendor.jdbcUrlDriverId(), serviceName, serviceName));
        return rdsConfigRequest;
    }
}