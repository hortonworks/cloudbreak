package com.sequenceiq.cloudbreak.structuredevent.service.file;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.structuredevent.conf.StructuredEventEnablementConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;

@Component
public class LegacyFileStructuredEventHandler<T extends StructuredEvent> implements EventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyFileStructuredEventHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private StructuredEventEnablementConfig structuredEventEnablementConfig;

    @Override
    public String selector() {
        return LegacyAsyncFileStructuredEventSender.SAVE_STRUCTURED_EVENT_TO_FILE;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        File file = new File(structuredEventEnablementConfig.getAuditFilePath());
        try {
            String structuredEventAsJson = objectMapper.writeValueAsString(structuredEvent);
            FileUtils.writeStringToFile(file, structuredEventAsJson + '\n', StandardCharsets.UTF_8, true);
            LOGGER.trace("Structured event\n{}\nhas been sent to file: {}", structuredEventAsJson, file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Can not write structured event to file " + file.getAbsolutePath(), e);
        }
    }
}