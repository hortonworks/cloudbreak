package com.sequenceiq.cloudbreak.controller.json;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.controller.validation.TrustedPlugin;

public class PluginJson {

    @Size(max = 100, min = 1, message = "The length of the recipe's name has to be in range of 1 to 100")
    @NotNull
    @TrustedPlugin
    private String url;

    @Size(max = 1000)
    private List<String> parameters;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }
}
