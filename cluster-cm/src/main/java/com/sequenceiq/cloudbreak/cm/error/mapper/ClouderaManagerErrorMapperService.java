package com.sequenceiq.cloudbreak.cm.error.mapper;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Component
public class ClouderaManagerErrorMapperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerErrorMapperService.class);

    @Inject
    private List<ClouderaManagerErrorMapper> clouderaManagerErrorMappers;

    public String map(StackDtoDelegate stack, List<CommandDetails> failedCommands, String originalMessage) {
        try {
            for (ClouderaManagerErrorMapper clouderaManagerErrorMapper : clouderaManagerErrorMappers) {
                if (clouderaManagerErrorMapper.canHandle(stack, failedCommands)) {
                    LOGGER.debug("Mapping error {} with {}", originalMessage, clouderaManagerErrorMapper.getClass().getSimpleName());
                    return clouderaManagerErrorMapper.map(stack, failedCommands, originalMessage);
                }
            }
            LOGGER.debug("No error mapper found for {}", originalMessage);
            return originalMessage;
        } catch (Exception e) {
            LOGGER.error("Error while mapping errors for {}", originalMessage, e);
            return originalMessage;
        }
    }
}
