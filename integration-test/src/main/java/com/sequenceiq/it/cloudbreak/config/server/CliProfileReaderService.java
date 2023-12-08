package com.sequenceiq.it.cloudbreak.config.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

@Service
public class CliProfileReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CliProfileReaderService.class);

    @Value("${integrationtest.dp.profile:localhost}")
    private String profile;

    public Optional<CloudbreakUser> getProfileUser() {
        String accessKey;
        String secretKey;
        Map<String, String> profileData;

        try {
            profileData = read();
            accessKey = profileData.get("apikeyid");
            secretKey = profileData.get("privatekey");
        } catch (Exception e) {
            LOGGER.warn("There is no credential in dp cli config to the profile {}", profile);
            return Optional.empty();
        }

        if (accessKey == null || "".equals(accessKey)) {
            LOGGER.warn("The profile {} is exist,  but there is no credential set in dp cli", profile);
            return Optional.empty();
        }

        return Optional.of(new CloudbreakUser(accessKey, secretKey));
    }

    public Map<String, String> read() throws IOException {
        String userHome = System.getProperty("user.home");
        Path cbProfileLocation = Paths.get(userHome, ".dp", "config");
        if (!Files.exists(cbProfileLocation)) {
            LOGGER.info("Could not find cb profile file at location {}, falling back to application.yml", cbProfileLocation);
            return Map.of();
        }
        LOGGER.info("At {} profile is found.", cbProfileLocation);

        byte[] encoded = Files.readAllBytes(Paths.get(userHome, ".dp", "config"));
        String profileString = new String(encoded, Charset.defaultCharset());

        Yaml yaml = new Yaml();
        Map<String, Object> profiles = yaml.load(profileString);

        return (Map<String, String>) profiles.get(profile);
    }
}
