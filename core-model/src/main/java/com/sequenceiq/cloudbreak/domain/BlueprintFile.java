package com.sequenceiq.cloudbreak.domain;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.common.json.Json;

public class BlueprintFile {

    private String name;

    private String blueprintText;

    private String defaultBlueprintText;

    private String stackName;

    private String description;

    private int hostGroupCount;

    private String stackType;

    private String stackVersion;

    private BlueprintUpgradeOption blueprintUpgradeOption;

    private BlueprintHybridOption hybridOption;

    private Json tags;

    private BlueprintFile(String name, String blueprintText, String defaultBlueprintText, String stackName, String description, int hostGroupCount,
            String stackType, String stackVersion, BlueprintUpgradeOption blueprintUpgradeOption, BlueprintHybridOption hybridOption, Json tags) {
        this.name = name;
        this.blueprintText = blueprintText;
        this.defaultBlueprintText = defaultBlueprintText;
        this.stackName = stackName;
        this.description = description;
        this.hostGroupCount = hostGroupCount;
        this.stackType = stackType;
        this.stackVersion = stackVersion;
        this.blueprintUpgradeOption = blueprintUpgradeOption;
        this.hybridOption = hybridOption;
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getStackName() {
        return stackName;
    }

    public int getHostGroupCount() {
        return hostGroupCount;
    }

    public String getStackType() {
        return stackType;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public String getBlueprintText() {
        return blueprintText;
    }

    public String getDefaultBlueprintText() {
        return defaultBlueprintText;
    }

    public BlueprintUpgradeOption getBlueprintUpgradeOption() {
        return blueprintUpgradeOption;
    }

    public BlueprintHybridOption getHybridOption() {
        return hybridOption;
    }

    public Json getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return "Blueprint{" +
                ", name='" + name + '\'' +
                ", stackName='" + stackName + '\'' +
                ", description='" + description + '\'' +
                ", hostGroupCount=" + hostGroupCount +
                ", stackType='" + stackType + '\'' +
                ", stackVersion='" + stackVersion + '\'' +
                ", blueprintUpgradeOption=" + blueprintUpgradeOption +
                ", hybridOption=" + hybridOption +
                ", tags=" + tags +
                '}';
    }

    public static class Builder {
        private String name;

        private String blueprintText;

        private String defaultBlueprintText;

        private String stackName;

        private String description;

        private int hostGroupCount;

        private String stackType;

        private String stackVersion;

        private BlueprintUpgradeOption blueprintUpgradeOption;

        private BlueprintHybridOption hybridOption;

        private Json tags;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder blueprintText(String blueprintText) {
            this.blueprintText = blueprintText;
            return this;
        }

        public Builder defaultBlueprintText(String defaultBlueprintText) {
            this.defaultBlueprintText = defaultBlueprintText;
            return this;
        }

        public Builder stackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder stackType(String stackType) {
            this.stackType = stackType;
            return this;
        }

        public Builder stackVersion(String stackVersion) {
            this.stackVersion = stackVersion;
            return this;
        }

        public Builder hostGroupCount(int hostGroupCount) {
            this.hostGroupCount = hostGroupCount;
            return this;
        }

        public Builder tags(Json tags) {
            this.tags = tags;
            return this;
        }

        public Builder blueprintUpgradeOption(BlueprintUpgradeOption blueprintUpgradeOption) {
            this.blueprintUpgradeOption = blueprintUpgradeOption;
            return this;
        }

        public Builder hybridOption(BlueprintHybridOption hybridOption) {
            this.hybridOption = hybridOption;
            return this;
        }

        public BlueprintFile build() {
            Preconditions.checkNotNull(name);
            Preconditions.checkNotNull(blueprintText);
            Preconditions.checkNotNull(stackName);
            Preconditions.checkNotNull(stackVersion);
            Preconditions.checkNotNull(stackType);
            return new BlueprintFile(name, blueprintText, defaultBlueprintText, stackName, description, hostGroupCount, stackType, stackVersion,
                    blueprintUpgradeOption, hybridOption, tags);
        }
    }
}
