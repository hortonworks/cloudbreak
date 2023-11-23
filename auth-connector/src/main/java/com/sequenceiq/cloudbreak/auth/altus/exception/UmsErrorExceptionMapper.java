package com.sequenceiq.cloudbreak.auth.altus.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class UmsErrorExceptionMapper extends BaseExceptionMapper<UmsErrorException> {

    private static final int UMS_ERROR_HTTP_CODE = 567;

    private static final String UMS_ERROR_REASON_PHRASE = "UMS_ERROR";

    @Override
    public Response.StatusType getResponseStatus(UmsErrorException exception) {
        return new CustomResponseStatus(UMS_ERROR_HTTP_CODE, Family.SERVER_ERROR, UMS_ERROR_REASON_PHRASE);
    }

    @Override
    public Class<UmsErrorException> getExceptionType() {
        return UmsErrorException.class;
    }
}
