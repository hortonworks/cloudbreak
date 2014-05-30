package com.sequenceiq.provisioning.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.core.io.ClassPathResource;

public class FileReaderUtils {

    public static final String readFileFromClasspath(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(new ClassPathResource(fileName).getInputStream(), "UTF-8"));
        for (int c = br.read(); c != -1; c = br.read()) {
            sb.append((char) c);
        }
        return sb.toString();
    }

}
