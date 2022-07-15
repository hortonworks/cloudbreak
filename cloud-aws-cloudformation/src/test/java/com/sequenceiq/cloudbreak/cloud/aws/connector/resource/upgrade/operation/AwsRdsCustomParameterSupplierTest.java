package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.rds.model.ApplyMethod;
import com.amazonaws.services.rds.model.Parameter;

public class AwsRdsCustomParameterSupplierTest {

    private static final String RDS_FORCE_SSL_PARAMETER_NAME = "rds.force_ssl";

    private static final String RDS_FORCE_SSL_PARAMETER_VALUE = "1";

    private final AwsRdsCustomParameterSupplier underTest = new AwsRdsCustomParameterSupplier();

    @Test
    void testParameters() {
        List<Parameter> parametersToChange = underTest.getParametersToChange();

        assertThat(parametersToChange).asList()
                .hasSize(1)
                .contains(createParameter(RDS_FORCE_SSL_PARAMETER_NAME, RDS_FORCE_SSL_PARAMETER_VALUE));
    }

    private Parameter createParameter(String name, String value) {
        return new Parameter()
                .withParameterName(name)
                .withParameterValue(value)
                .withApplyMethod(ApplyMethod.Immediate);

    }

}