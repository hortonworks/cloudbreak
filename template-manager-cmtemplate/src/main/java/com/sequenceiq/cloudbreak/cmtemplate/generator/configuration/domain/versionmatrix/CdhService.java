package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.versionmatrix;

public class CdhService {

    private String name;

    private String version;

    private String displayName;

    private String iconKey;

    private String componentNameInParcel;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public String getComponentNameInParcel() {
        return componentNameInParcel;
    }

    public void setComponentNameInParcel(String componentNameInParcel) {
        this.componentNameInParcel = componentNameInParcel;
    }
}
