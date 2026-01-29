package com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain;

import java.util.StringJoiner;

public class SupportedService {

    private String name;

    private String displayName;

    private String iconKey;

    private String version;

    private String componentNameInParcel;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getComponentNameInParcel() {
        return componentNameInParcel;
    }

    public void setComponentNameInParcel(String componentNameInParcel) {
        this.componentNameInParcel = componentNameInParcel;
    }

    @Override public String toString() {
        return new StringJoiner(", ", SupportedService.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("displayName='" + displayName + "'")
                .add("iconKey='" + iconKey + "'")
                .add("version='" + version + "'")
                .add("componentNameInParcel='" + componentNameInParcel + "'")
                .toString();
    }
}
