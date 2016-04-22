package com.sequenceiq.it.mock.restito.docker.model;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.annotations.SerializedName;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Info {

    @SerializedName("Containers")
    private int containers;

    @SerializedName("Debug")
    private boolean debug;

    @SerializedName("DockerRootDir")
    private String DockerRootDir;

    @SerializedName("Driver")
    private String driver;

    @SerializedName("DriverStatus")
    private List<Object> driverStatuses;

    @SerializedName("ExecutionDriver")
    private String executionDriver;

    @SerializedName("ID")
    private String ID;

    @SerializedName("IPv4Forwarding")
    private boolean IPv4Forwarding;

    @SerializedName("Images")
    private int images;

    @SerializedName("IndexServerAddress")
    private String IndexServerAddress;

    @SerializedName("InitPath")
    private String initPath;

    @SerializedName("InitSha1")
    private String initSha1;

    @SerializedName("KernelVersion")
    private String kernelVersion;

    @SerializedName("Labels")
    private String[] Labels;

    @SerializedName("MemoryLimit")
    private boolean memoryLimit;

    @SerializedName("MemTotal")
    private long memTotal;

    @SerializedName("Name")
    private String name;

    @SerializedName("NCPU")
    private int NCPU;

    @SerializedName("NEventsListener")
    private long nEventListener;

    @SerializedName("NFd")
    private int NFd;

    @SerializedName("NGoroutines")
    private int NGoroutines;

    @SerializedName("OperatingSystem")
    private String OperatingSystem;

    @SerializedName("Sockets")
    private String[] sockets;

    @SerializedName("SwapLimit")
    private boolean swapLimit;

    public boolean isDebug() {
        return debug;
    }

    public int getContainers() {
        return containers;
    }

    public String getDockerRootDir() {
        return DockerRootDir;
    }

    public String getDriver() {
        return driver;
    }

    public List<Object> getDriverStatuses() {
        return driverStatuses;
    }

    public int getImages() {
        return images;
    }

    public String getID() {
        return ID;
    }

    public boolean getIPv4Forwarding() {
        return IPv4Forwarding;
    }

    public String getIndexServerAddress() {
        return IndexServerAddress;
    }

    public String getInitPath() {
        return initPath;
    }

    public String getInitSha1() {
        return initSha1;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public String[] getLabels() {
        return Labels;
    }

    public String[] getSockets() {
        return sockets;
    }

    public boolean isMemoryLimit() {
        return memoryLimit;
    }

    public long getnEventListener() {
        return nEventListener;
    }

    public long getMemTotal() {
        return memTotal;
    }

    public String getName() {
        return name;
    }

    public int getNCPU() {
        return NCPU;
    }

    public int getNFd() {
        return NFd;
    }

    public int getNGoroutines() {
        return NGoroutines;
    }

    public String getOperatingSystem() {
        return OperatingSystem;
    }

    public boolean getSwapLimit() {
        return swapLimit;
    }

    public String getExecutionDriver() {
        return executionDriver;
    }

    public void setDriverStatuses(List<Object> driverStatuses) {
        this.driverStatuses = driverStatuses;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
