package com.sequenceiq.cloudbreak.cloud.yarn.client.model.core;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonInclude(Include.NON_NULL)
public class ConfigFile implements JsonEntity {
    private String type;

    @JsonProperty("dest_file")
    private String destFile;

    @JsonProperty("src_file")
    private String srcFile;

    private Map<String, String> props;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDestFile() {
        return destFile;
    }

    public void setDestFile(String destFile) {
        this.destFile = destFile;
    }

    public String getSrcFile() {
        return srcFile;
    }

    public void setSrcFile(String srcFile) {
        this.srcFile = srcFile;
    }

    public Map<String, String> getProps() {
        return props;
    }

    public void setProps(Map<String, String> props) {
        this.props = props;
    }
}
