package com.sequenceiq.periscope.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.io.BaseEncoding;

public final class FileReaderUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReaderUtils.class);

    private FileReaderUtils() {
    }

    public static String readFileFromClasspathQuietly(String fileName) {
        try {
            return readFileFromClasspath(fileName);
        } catch (IOException e) {
            LOGGER.error("Failed to load file from classpath. Original message: {}", e.getMessage());
            return null;
        }
    }

    public static String readFileFromClasspath(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ClassPathResource(fileName).getInputStream(), StandardCharsets.UTF_8))) {
            for (int c = br.read(); c != -1; c = br.read()) {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }

    public static String readFileFromPath(String fileName) throws IOException {
        String br = IOUtils.toString(new FileInputStream(fileName));
        return BaseEncoding.base64().encode(br.getBytes());
    }

    public static String readBinaryFileFromPath(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        return BaseEncoding.base64().encode(Files.readAllBytes(path));
    }

    public static String readFileFromPathToString(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        return new String(Files.readAllBytes(path));
    }

}
