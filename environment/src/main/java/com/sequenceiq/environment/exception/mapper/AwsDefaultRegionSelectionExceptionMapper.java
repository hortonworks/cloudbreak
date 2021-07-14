package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsDefaultRegionSelectionFailed;

@Component
public class AwsDefaultRegionSelectionExceptionMapper extends EnvironmentBaseExceptionMapper<AwsDefaultRegionSelectionFailed> {

    @Override
    public Response.Status getResponseStatus(AwsDefaultRegionSelectionFailed exception) {
        return Response.Status.FORBIDDEN;
    }

    @Override
    public Class<AwsDefaultRegionSelectionFailed> getExceptionType() {
        return AwsDefaultRegionSelectionFailed.class;
    }

}
