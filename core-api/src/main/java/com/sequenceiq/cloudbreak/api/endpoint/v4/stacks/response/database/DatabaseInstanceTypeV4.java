package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseInstanceTypeV4 {

    @Schema(description = "Instance type identifier, e.g. db.m5.large")
    private String name;

    @Schema(description = "Number of virtual CPUs")
    private Integer cpu;

    @Schema(description = "Memory in gigabytes")
    private Float memoryInGb;

    @Schema(description = "CPU architecture, e.g. x86_64 or arm64")
    private String architecture;

    @Schema(description = "Whether this is the default instance type for the region")
    private boolean defaultType;

    public DatabaseInstanceTypeV4() {
    }

    public DatabaseInstanceTypeV4(String name, Integer cpu, Float memoryInGb, String architecture, boolean defaultType) {
        this.name = name;
        this.cpu = cpu;
        this.memoryInGb = memoryInGb;
        this.architecture = architecture;
        this.defaultType = defaultType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCpu() {
        return cpu;
    }

    public void setCpu(Integer cpu) {
        this.cpu = cpu;
    }

    public Float getMemoryInGb() {
        return memoryInGb;
    }

    public void setMemoryInGb(Float memoryInGb) {
        this.memoryInGb = memoryInGb;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public boolean isDefaultType() {
        return defaultType;
    }

    public void setDefaultType(boolean defaultType) {
        this.defaultType = defaultType;
    }

    @Override
    public String toString() {
        return "DatabaseInstanceTypeV4{" +
                "name='" + name + '\'' +
                ", cpu=" + cpu +
                ", memoryInGb=" + memoryInGb +
                ", architecture='" + architecture + '\'' +
                ", defaultType=" + defaultType +
                '}';
    }
}
