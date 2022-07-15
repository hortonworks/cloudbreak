package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.amazonaws.services.rds.model.ApplyMethod;
import com.amazonaws.services.rds.model.Parameter;

@Component
public class AwsRdsCustomParameterSupplier {

    private  static final String RDS_FORCE_SSL_PARAMETER_NAME = "rds.force_ssl";

    private static final String RDS_FORCE_SSL_PARAMETER_ON = "1";

    public List<Parameter> getParametersToChange() {
        return List.of(createForceSslParameter());
    }

    private Parameter createForceSslParameter() {
        return new Parameter()
                .withParameterName(RDS_FORCE_SSL_PARAMETER_NAME)
                .withParameterValue(RDS_FORCE_SSL_PARAMETER_ON)
                .withApplyMethod(ApplyMethod.Immediate);
    }

}