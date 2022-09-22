package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import java.util.List;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.rds.model.ApplyMethod;
import software.amazon.awssdk.services.rds.model.Parameter;

@Component
public class AwsRdsCustomParameterSupplier {

    private  static final String RDS_FORCE_SSL_PARAMETER_NAME = "rds.force_ssl";

    private static final String RDS_FORCE_SSL_PARAMETER_ON = "1";

    public List<Parameter> getParametersToChange() {
        return List.of(createForceSslParameter());
    }

    private Parameter createForceSslParameter() {
        return Parameter.builder()
                .parameterName(RDS_FORCE_SSL_PARAMETER_NAME)
                .parameterValue(RDS_FORCE_SSL_PARAMETER_ON)
                .applyMethod(ApplyMethod.IMMEDIATE)
                .build();
    }
}