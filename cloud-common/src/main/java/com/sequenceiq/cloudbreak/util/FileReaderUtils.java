package com.sequenceiq.cloudbreak.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
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
            LOGGER.error("Failed to load file from classpath", e);
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

    public static String readFileFromPathBase64(String fileName) throws IOException {
        String br = IOUtils.toString(new FileInputStream(fileName));
        return BaseEncoding.base64().encode(br.getBytes());
    }

    public static String readBinaryFileFromPath(Path path) throws IOException {
        if (path == null) {
            throw new IOException("File path must not be null");
        }
        return BaseEncoding.base64().encode(Files.readAllBytes(path));
    }

    public static String readFileFromPath(Path path) throws IOException {
        if (path == null) {
            throw new IOException("File path must not be null");
        }
        return new String(Files.readAllBytes(path));
    }

    public static String readFileFromCustomPath(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.isFile()) {
            throw new IOException("Given path should be a file");
        }
        if (!file.exists()) {
            throw new IOException("File must be exists");
        }
        return FileUtils.readFileToString(file);
    }

    public static File getDirFromClasspath(String dirPath) throws IOException {
        String path = dirPath;
        if (!path.startsWith(File.separator)) {
            path = File.separator + path;
        }
        File dir = new ClassPathResource(path).getFile();
        if (!dir.exists()) {
            throw new IOException("Dir does not exists");
        }
        if (!dir.isDirectory()) {
            throw new IOException(dirPath + " is not a directory.");
        }
        return dir;
    }

    public static List<String> getFileNamesRecursivelyFromClasspathByDirPath(String dirPath, FilenameFilter filter) throws IOException {
        File dir = getDirFromClasspath(dirPath);
        return Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .flatMap(f -> {
                    if (f.isDirectory() && f.listFiles() != null) {
                        try {
                            return getFileNamesRecursivelyFromClasspathByDirPath(dirPath + File.separator + f.getName(), filter).stream();
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    } else if (f.isFile() && filter != null && filter.accept(f.getParentFile(), f.getName())) {
                        return Stream.of(dirPath + File.separator + f.getName());
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());
    }
}
