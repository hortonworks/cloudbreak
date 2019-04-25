package com.sequenceiq.cloudbreak.template.views;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

public class BlueprintView {

    private String blueprintText;

    private String version;

    private String type;

    private Set<String> components;

    private BlueprintTextProcessor processor;

    public BlueprintView(String blueprintText, String version, String type, BlueprintTextProcessor processor) {
        this.blueprintText = blueprintText;
        this.type = type;
        this.version = version;
        this.processor = processor;
        components = prepareComponents(blueprintText);
    }

    private Set<String> prepareComponents(String blueprintText) {
        Set<String> result = new HashSet<>();
        try {
            Map<String, Set<String>> componentsByHostGroup = processor.getComponentsByHostGroup();
            componentsByHostGroup.values().forEach(result::addAll);
        } catch (BlueprintProcessingException exception) {
            result = new HashSet<>();
        }
        return result;
    }

    public String getVersion() {
        return version;
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = blueprintText;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public boolean isHdf() {
        return "HDF".equalsIgnoreCase(type);
    }

    public String getBlueprintText() {
        return blueprintText;
    }

    public Set<String> getComponents() {
        return components;
    }

    public void setComponents(Set<String> components) {
        this.components = components;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlueprintView)) {
            return false;
        }
        BlueprintView that = (BlueprintView) o;
        return Objects.equals(blueprintText, that.blueprintText)
                && Objects.equals(version, that.version)
                && Objects.equals(type, that.type)
                && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blueprintText, version, type, components);
    }

}
