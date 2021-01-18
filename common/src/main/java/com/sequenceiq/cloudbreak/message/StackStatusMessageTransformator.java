package com.sequenceiq.cloudbreak.message;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class StackStatusMessageTransformator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusMessageTransformator.class);

    private Map<String, String> statusMessagePatterns;

    @PostConstruct
    public void init() {
        try {
            String file = FileReaderUtils.readFileFromClasspath("messages/stack-status-messages.yaml");
            statusMessagePatterns = new Yaml().load(file);
            LOGGER.info("Status messages loaded for stack: {}", statusMessagePatterns);
        } catch (IOException e) {
            throw new RuntimeException("Can't load stack status messsages", e);
        }
    }

    public String transformMessage(String rawMessage) {
        String result = rawMessage;
        if (isNoneBlank(rawMessage)) {
            Optional<String> patternKey = statusMessagePatterns.keySet().stream().filter(rawMessage::contains).findFirst();
            if (patternKey.isPresent()) {
                result = statusMessagePatterns.get(patternKey.get());
                LOGGER.info("Status message was transformed from '{}' to '{}'", rawMessage, result);
            }
        }
        return result;
    }
}
