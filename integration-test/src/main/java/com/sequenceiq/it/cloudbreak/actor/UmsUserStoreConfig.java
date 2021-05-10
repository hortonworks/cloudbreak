package com.sequenceiq.it.cloudbreak.actor;

import java.nio.file.Path;

public class UmsUserStoreConfig {

    private boolean atCustomPath;

    private boolean filePresent;

    private String classFilePath;

    private Path customFilePath;

    private String fetchedFilePath;

    private String workspace;

    public void setAtCustomPath(boolean atCustomPath) {
        this.atCustomPath = atCustomPath;
    }

    public boolean getAtCustomPath() {
        return atCustomPath;
    }

    public void setFilePresent(boolean filePresent) {
        this.filePresent = filePresent;
    }

    public boolean getFilePresent() {
        return filePresent;
    }

    public void setClassFilePath(String classFilePath) {
        this.classFilePath = classFilePath;
    }

    public String getClassFilePath() {
        return classFilePath;
    }

    public void setCustomFilePath(Path customFilePath) {
        this.customFilePath = customFilePath;
    }

    public Path getCustomFilePath() {
        return customFilePath;
    }

    public void setFetchedFilePath(String fetchedFilePath) {
        this.fetchedFilePath = fetchedFilePath;
    }

    public String getFetchedFilePath() {
        return fetchedFilePath;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getWorkspace() {
        return workspace;
    }

    @Override
    public String toString() {
        return "UmsUserStoreConfig{" +
                "atCustomPath=" + atCustomPath +
                ", filePresent=" + filePresent +
                ", classFilePath='" + classFilePath + '\'' +
                ", customFilePath='" + customFilePath + '\'' +
                ", fetchedFilePath='" + fetchedFilePath + '\'' +
                ", workspace='" + workspace + '\'' +
                '}';
    }
}
