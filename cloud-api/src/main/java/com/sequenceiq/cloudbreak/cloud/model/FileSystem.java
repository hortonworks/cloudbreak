package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class FileSystem extends DynamicModel {

    private String name;

    private String type;

    private boolean defaultFs;

    public FileSystem(String name, String type, boolean defaultFs, Map<String, String> parameters) {
        this.name = name;
        this.type = type;
        this.defaultFs = defaultFs;
        for (Entry<String, String> entry : parameters.entrySet()) {
            putParameter(entry.getKey(), entry.getValue());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDefaultFs() {
        return defaultFs;
    }

    public void setDefaultFs(boolean defaultFs) {
        this.defaultFs = defaultFs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FileSystem{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", defaultFs=").append(defaultFs);
        sb.append('}');
        return sb.toString();
    }
}
