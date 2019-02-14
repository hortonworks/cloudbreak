package com.sequenceiq.cloudbreak.clusterdefinition.testrepeater;

import java.nio.file.Path;

public class TestFile {

    private final String fileName;

    private final String filePath;

    private final String fileContent;

    public TestFile(String fileName, String filePath, String fileContent) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileContent = fileContent;
    }

    public TestFile(Path filePath, String fileContent) {
        fileName = filePath.toFile().getName();
        this.fileContent = fileContent;
        this.filePath = filePath.toAbsolutePath().toString();
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileContent() {
        return fileContent;
    }

    @Override
    public String toString() {
        return String.join("TestFile{fileName='%s', filePath='%s'}", fileName, filePath);
    }
}
