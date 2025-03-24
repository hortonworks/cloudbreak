package com.sequenceiq.flow.graph;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GraphDescriptorFileWriter {

    private GraphDescriptorFileWriter() {
    }

    public static void saveToFile(String destinationDirectory, String fileName, String content) throws IOException {
        File destinationDir = new File(destinationDirectory);
        if (!destinationDir.exists()) {
            boolean success = destinationDir.mkdirs();
            if (!success) {
                throw new IOException("Unable to create directories: " + destinationDir.getAbsolutePath());
            }
        }
        Files.write(Paths.get(String.format("%s/%s.dot", destinationDirectory, fileName)), content.getBytes(StandardCharsets.UTF_8));
    }
}
