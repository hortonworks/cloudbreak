package com.sequenceiq.cloudbreak.service.cluster;

import java.util.List;

public class Package {
    private String name;

    private List<PackageName> pkg;

    private String command;

    private boolean prewarmed;

    private String grain;

    private String commandVersionPattern;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PackageName> getPkg() {
        return pkg;
    }

    public void setPkg(List<PackageName> pkg) {
        this.pkg = pkg;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isPrewarmed() {
        return prewarmed;
    }

    public void setPrewarmed(boolean prewarmed) {
        this.prewarmed = prewarmed;
    }

    public String getGrain() {
        return grain;
    }

    public void setGrain(String grain) {
        this.grain = grain;
    }

    public String getCommandVersionPattern() {
        return commandVersionPattern;
    }

    public void setCommandVersionPattern(String commandVersionPattern) {
        this.commandVersionPattern = commandVersionPattern;
    }

    @Override
    public String toString() {
        return "Package{" +
                "name='" + name + '\'' +
                ", pkg=" + pkg +
                ", command='" + command + '\'' +
                ", prewarmed=" + prewarmed +
                ", grain='" + grain + '\'' +
                ", commandVersionPattern='" + commandVersionPattern + '\'' +
                '}';
    }
}
