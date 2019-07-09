package com.sequenceiq.cloudbreak.orchestrator.model;

public class KeytabModel {

    private final String service;

    private final String path;

    private final String fileName;

    private final String principal;

    private final byte[] keytab;

    public KeytabModel(String service, String path, String fileName, String principal, byte[] keytab) {
        this.service = service;
        this.path = path;
        this.fileName = fileName;
        this.principal = principal;
        this.keytab = keytab;
    }

    public String getService() {
        return service;
    }

    public String getPath() {
        return path;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPrincipal() {
        return principal;
    }

    public byte[] getKeytab() {
        return keytab;
    }

    @Override
    public String toString() {
        return "KeytabModel{"
                + "service='" + service + '\''
                + ", path='" + path + '\''
                + '}';
    }
}