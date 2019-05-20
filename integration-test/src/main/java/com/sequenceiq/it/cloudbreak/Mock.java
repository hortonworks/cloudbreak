package com.sequenceiq.it.cloudbreak;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

public class Mock {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mock.class);

    private static final String MOCKRESPONSE = "/mockresponse/";

    private static final Gson GSON = new Gson();

    private Mock() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static String responseFromJsonFile(String path) {
        try (InputStream inputStream = ITResponse.class.getResourceAsStream(MOCKRESPONSE + path)) {
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            LOGGER.error("can't read file from path", e);
            return "";
        }
    }

    public static File createTempFileFromClasspath(String file) {
        try {
            InputStream sshPemInputStream = new ClassPathResource(file).getInputStream();
            File tempKeystoreFile = File.createTempFile(file, ".tmp");
            try (OutputStream outputStream = new FileOutputStream(tempKeystoreFile)) {
                IOUtils.copy(sshPemInputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("can't write " + file, e);
            }
            return tempKeystoreFile;
        } catch (IOException e) {
            throw new RuntimeException(file + " not found", e);
        }
    }
}
