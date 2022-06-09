package com.sequenceiq.cloudbreak.service.upgrade.validation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CmManifestFile {

    private String product;

    private String version;

    private String gbn;

    private List<String> files = new ArrayList<>();

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGbn() {
        return gbn;
    }

    public void setGbn(String gbn) {
        this.gbn = gbn;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    @Override
    public String toString() {
        return "CmManifestFile{" +
                "product='" + product + '\'' +
                ", version='" + version + '\'' +
                ", gbn='" + gbn + '\'' +
                ", files=" + files +
                '}';
    }
}
