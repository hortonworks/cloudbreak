package com.sequenceiq.cloudbreak;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

public class VersionedApplication {

    public static final String LONG_VERSION = "--version";

    public static final String SHORT_VERSION = "-v";

    private VersionedApplication() {

    }

    public boolean showVersionInfo(String[] args) {
        if (checkIfParamVersion(args)) {
            try {
                if (LONG_VERSION.equals(args[0])) {
                    System.out.println("The application info is: \n" + readVersionFromClasspath("application.properties", false));
                } else if (SHORT_VERSION.equals(args[0])) {
                    System.out.println(readVersionFromClasspath("application.properties", true));
                }
            } catch (IOException ignored) {
                System.out.println("The application.properties file not found version is undefined.");
            }
            return true;
        }
        return false;
    }

    private boolean checkIfParamVersion(String[] args) {
        return args.length == 1 && (LONG_VERSION.equals(args[0]) || SHORT_VERSION.equals(args[0]));
    }

    private String readVersionFromClasspath(String fileName, boolean onlyVersion) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Reader reader = new InputStreamReader(new ClassPathResource(fileName).getInputStream(), StandardCharsets.UTF_8)) {
            try (BufferedReader br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (onlyVersion) {
                        if (line.startsWith("info.app.version=")) {
                            line = line.replaceAll("info.app.version=", "");
                            return line;
                        }
                    } else {
                        sb.append(line).append('\n');
                    }
                }
            }
        }
        return sb.toString();
    }

    public static VersionedApplication versionedApplication() {
        return new VersionedApplication();
    }
}
