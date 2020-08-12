package com.sequenceiq.cloudbreak.cloud.azure;

public class AzureImage {

    private String id;

    private String name;

    private boolean alreadyExists;

    public AzureImage(String id, String name, boolean alreadyExists) {
        this.id = id;
        this.name = name;
        this.alreadyExists = alreadyExists;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getAlreadyExists() {
        return alreadyExists;
    }

    public void setAlreadyExists(boolean alreadyExists) {
        this.alreadyExists = alreadyExists;
    }

    @Override
    public String toString() {
        return "AzureImage{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", alreadyExists=" + alreadyExists +
                '}';
    }
}
