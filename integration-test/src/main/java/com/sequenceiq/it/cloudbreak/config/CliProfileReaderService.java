package com.sequenceiq.it.cloudbreak.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
class CliProfileReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CliProfileReaderService.class);

    @Value("${integrationtest.dp.profile:localhost}")
    private String profile;

    Map<String, String> read() throws IOException {
        String userHome = System.getProperty("user.home");
        Path cbProfileLocation = Paths.get(userHome, ".dp", "config");
        if (!Files.exists(cbProfileLocation)) {
            LOGGER.info("Could not find cb profile file at location {}, falling back to application.yml", cbProfileLocation);
            return Map.of();
        }

        byte[] encoded = Files.readAllBytes(Paths.get(userHome, ".dp", "config"));
        String profileString = new String(encoded, Charset.defaultCharset());

        Yaml yaml = new Yaml();
        Map<String, Object> profiles = yaml.load(profileString);

        return (Map<String, String>) profiles.get(profile);
    }
}
