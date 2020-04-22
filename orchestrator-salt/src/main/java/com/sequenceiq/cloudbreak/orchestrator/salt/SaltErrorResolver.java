package com.sequenceiq.cloudbreak.orchestrator.salt;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class SaltErrorResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltErrorResolver.class);

    private Map<String, String> errorMessages;

    private String resolveErrorMessage(String errorMessage) {
        Optional<String> found = errorMessages.keySet().stream().filter(errorMessage::contains).findFirst();
        if (found.isPresent()) {
            return errorMessages.get(found.get());
        } else {
            return "Failed to execute: " + errorMessage;
        }
    }

    private String resolveMessageIfAvailable(String value) {
        Pattern namePattern = Pattern.compile("Name: (.*)");
        Matcher nameMatcher = namePattern.matcher(value);
        if (nameMatcher.find()) {
            return resolveErrorMessage(nameMatcher.group(1));
        } else {
            return value;
        }
    }

    @PostConstruct
    public void init() {
        try {
            String file = FileReaderUtils.readFileFromClasspath("salt/errormessages.yaml");
            errorMessages = new Yaml().load(file);
            LOGGER.info("Error messages for salt: {}", errorMessages);
        } catch (IOException e) {
            throw new RuntimeException("Can't load salt error messsages", e);
        }
    }

    public Multimap<String, String> resolveErrorMessages(Multimap<String, String> missingNodesWithReason) {
        LOGGER.info("Original missing nodes: {}", missingNodesWithReason);
        Multimap<String, String> missingTargetsWithReplacedReasons = ArrayListMultimap.create();
        missingNodesWithReason.entries().forEach(entry -> {
            String value = resolveMessageIfAvailable(entry.getValue());
            missingTargetsWithReplacedReasons.put(entry.getKey(), value);
        });
        LOGGER.info("Missing nodes after replace: {}", missingTargetsWithReplacedReasons);
        return missingTargetsWithReplacedReasons;
    }

}
