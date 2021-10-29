package com.sequenceiq.freeipa.controller.mapper;

import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.freeipa.client.InvalidFreeIpaStateException;

@Component
public class InvalidFreeIpaStateExceptionMapper extends BaseExceptionMapper<InvalidFreeIpaStateException> {

    private static final Logger LOGGER = getLogger(InvalidFreeIpaStateExceptionMapper.class);

    @Override
    protected String getErrorMessage(InvalidFreeIpaStateException exception) {
        LOGGER.info("Error during interaction with FreeIPA (invalid state): {}", exception.getMessage());
        return "Error during interaction with FreeIPA: " + exception.getMessage();
    }

    @Override
    public Status getResponseStatus(InvalidFreeIpaStateException exception) {
        return Status.BAD_GATEWAY;
    }

    @Override
    public Class<InvalidFreeIpaStateException> getExceptionType() {
        return InvalidFreeIpaStateException.class;
    }
}
