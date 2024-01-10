package com.sequenceiq.freeipa.controller.mapper;

import static org.slf4j.LoggerFactory.getLogger;

import jakarta.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.freeipa.client.FreeIpaClientBuildException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;

@Component
public class FreeipaClientExceptionWrapperMapper extends BaseExceptionMapper<FreeIpaClientExceptionWrapper> {

    private static final Logger LOGGER = getLogger(FreeipaClientExceptionWrapperMapper.class);

    @Override
    protected String getErrorMessage(FreeIpaClientExceptionWrapper exception) {
        LOGGER.info("Error during interaction with FreeIPA (client exception wrapper): {}", exception.getMessage());
        return "Error during interaction with FreeIPA: " + exception.getWrappedException().getMessage();    }

    @Override
    public Status getResponseStatus(FreeIpaClientExceptionWrapper exception) {
        if (exception.getWrappedException() != null && exception.getWrappedException() instanceof FreeIpaClientBuildException) {
            return Status.BAD_GATEWAY;
        } else {
            return Status.INTERNAL_SERVER_ERROR;
        }
    }

    @Override
    public Class<FreeIpaClientExceptionWrapper> getExceptionType() {
        return FreeIpaClientExceptionWrapper.class;
    }
}
