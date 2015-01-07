package com.sequenceiq.cloudbreak.controller.json;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class PluginJson {

    @Size(max = 100, min = 1, message = "The length of the recipe's name has to be in range of 1 to 100")
    @NotNull
    @Pattern(regexp = "https://github.com/.*(consul-plugins-)[a-z]*.git",
            message = "Only plugins from Github are allowed, and the repository's name must follow the naming convention: consul-plugins-[a-z]*")
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
