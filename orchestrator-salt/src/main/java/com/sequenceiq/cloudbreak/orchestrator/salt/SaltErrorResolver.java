package com.sequenceiq.cloudbreak.orchestrator.salt;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private List<String> commandsWithStderrFailures;

    private String resolveErrorMessage(Map<String, String> errors) {
        String name = errors.get("Name");
        Optional<String> found = errorMessages.keySet().stream().filter(name::contains).findFirst();
        if (found.isPresent()) {
            return errorMessages.get(found.get());
        } else {
            found = commandsWithStderrFailures.stream().filter(name::contains).findFirst();
            if (found.isPresent() && errors.containsKey("Stderr")) {
                return errors.get("Stderr");
            }
        }
        return "Failed to execute: " + errors;
    }

    private String resolveMessageIfAvailable(Map<String, String> value) {
        if (value.containsKey("Name")) {
            return resolveErrorMessage(value);
        }
        if (value.size() == 1) {
            return value.values().iterator().next();
        }
        return value.toString();
    }

    @PostConstruct
    public void init() {
        try {
            String file = FileReaderUtils.readFileFromClasspath("salt/errormessages.yaml");
            errorMessages = new Yaml().load(file);
            LOGGER.info("Error messages for salt: {}", errorMessages);
            file = FileReaderUtils.readFileFromClasspath("salt/stderrcommands.yaml");
            commandsWithStderrFailures = new Yaml().load(file);
            LOGGER.info("Salt commands that will pull the failure from stderr: {}", commandsWithStderrFailures);
        } catch (IOException e) {
            throw new RuntimeException("Can't load salt error messsages", e);
        }
    }

    public Multimap<String, String> resolveErrorMessages(Multimap<String, Map<String, String>> missingNodesWithReason) {
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
