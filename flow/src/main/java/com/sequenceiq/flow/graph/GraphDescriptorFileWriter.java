package com.sequenceiq.flow.graph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphDescriptorFileWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphDescriptorFileWriter.class);

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
        String outFile = String.format("%s/%s.dot", destinationDirectory, fileName);
        LOGGER.info("Saving file: {}", outFile);
        Files.writeString(Paths.get(outFile), content);
    }
}
