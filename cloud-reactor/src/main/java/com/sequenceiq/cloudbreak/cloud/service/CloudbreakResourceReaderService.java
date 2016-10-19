package com.sequenceiq.cloudbreak.cloud.service;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class CloudbreakResourceReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakResourceReaderService.class);

    @Value("${cb.etc.config.dir}")
    private String etcConfigDir;

    public String resourceDefinition(String prefix, String resource) {
        String fileName = prefix + "-" + resource + ".json";
        File customResourceFile = new File(etcConfigDir, fileName);
        try {
            if (customResourceFile.exists() && customResourceFile.isFile()) {
                LOGGER.info("Read of customised resource file: {}", customResourceFile.toPath());
                return FileReaderUtils.readFileFromPath(customResourceFile.toPath());
            }
            LOGGER.info("Customised resource file does not exist: {}", customResourceFile.toPath());
        } catch (IOException e) {
            LOGGER.error("Failed to read file: {}", customResourceFile.toPath(), e);
        }
        LOGGER.info("Read resource file from classpath: {}", "definitions/" + fileName);
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/" + fileName);

    }

}
