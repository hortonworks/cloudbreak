package com.sequenceiq.cloudbreak.template.model;

import java.util.List;

public class TemplateFiles {

    private List<String> files;

    private TemplateFiles(List<String> files) {
        this.files = files;
    }

    public List<String> getFiles() {
        return files;
    }

    public static TemplateFiles templateFiles(List<String> files) {
        return new TemplateFiles(files);
    }
}
