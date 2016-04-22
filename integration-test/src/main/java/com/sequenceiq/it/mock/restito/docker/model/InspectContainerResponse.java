package com.sequenceiq.it.mock.restito.docker.model;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.VolumeBind;
import com.github.dockerjava.api.model.VolumeBinds;
import com.github.dockerjava.api.model.VolumeRW;
import com.github.dockerjava.api.model.VolumesRW;
import com.google.gson.annotations.SerializedName;

public class InspectContainerResponse {

    @SerializedName("Args")
    private String[] args;

    @SerializedName("Config")
    private ContainerConfig config;

    @SerializedName("Created")
    private String created;

    @SerializedName("Driver")
    private String driver;

    @SerializedName("ExecDriver")
    private String execDriver;

    @SerializedName("HostConfig")
    private HostConfig hostConfig;

    @SerializedName("HostnamePath")
    private String hostnamePath;

    @SerializedName("HostsPath")
    private String hostsPath;

    @SerializedName("Id")
    private String id;

    @SerializedName("Image")
    private String imageId;

    @SerializedName("MountLabel")
    private String mountLabel;

    @SerializedName("Name")
    private String name;

    @SerializedName("NetworkSettings")
    private com.github.dockerjava.api.command.InspectContainerResponse.NetworkSettings networkSettings;

    @SerializedName("Path")
    private String path;

    @SerializedName("ProcessLabel")
    private String processLabel;

    @SerializedName("ResolvConfPath")
    private String resolvConfPath;

    @SerializedName("ExecIDs")
    private List<String> execIds;

    @SerializedName("State")
    private ContainerState state;

    @SerializedName("Volumes")
    private VolumeBinds volumes;

    @SerializedName("VolumesRW")
    private VolumesRW volumesRW;

    public String getId() {
        return id;
    }

    public String getCreated() {
        return created;
    }

    public String getPath() {
        return path;
    }

    public String getProcessLabel() {
        return processLabel;
    }

    public String[] getArgs() {
        return args;
    }

    public ContainerConfig getConfig() {
        return config;
    }

    public ContainerState getState() {
        return state;
    }

    public String getImageId() {
        return imageId;
    }

    public com.github.dockerjava.api.command.InspectContainerResponse.NetworkSettings getNetworkSettings() {
        return networkSettings;
    }

    public String getResolvConfPath() {
        return resolvConfPath;
    }

    public VolumeBind[] getVolumes() {
        return volumes.getBinds();
    }

    public VolumeRW[] getVolumesRW() {
        return volumesRW.getVolumesRW();
    }

    public String getHostnamePath() {
        return hostnamePath;
    }

    public String getHostsPath() {
        return hostsPath;
    }

    public String getName() {
        return name;
    }

    public String getDriver() {
        return driver;
    }

    public HostConfig getHostConfig() {
        return hostConfig;
    }

    public String getExecDriver() {
        return execDriver;
    }

    public String getMountLabel() {
        return mountLabel;
    }

    public List<String> getExecIds() {
        return execIds;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setState(ContainerState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public class NetworkSettings {

        @SerializedName("IPAddress")
        private String ipAddress;

        @SerializedName("IPPrefixLen")
        private int ipPrefixLen;

        @SerializedName("Gateway")
        private String gateway;

        @SerializedName("Bridge")
        private String bridge;

        @SerializedName("PortMapping")
        private Map<String, Map<String, String>> portMapping;

        @SerializedName("Ports")
        private Ports ports;

        public String getIpAddress() {
            return ipAddress;
        }

        public int getIpPrefixLen() {
            return ipPrefixLen;
        }

        public String getGateway() {
            return gateway;
        }

        public String getBridge() {
            return bridge;
        }

        public Map<String, Map<String, String>> getPortMapping() {
            return portMapping;
        }

        public Ports getPorts() {
            return ports;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static class ContainerState {

        @SerializedName("Running")
        private boolean running;

        @SerializedName("Paused")
        private boolean paused;

        @SerializedName("Pid")
        private int pid;

        @SerializedName("ExitCode")
        private int exitCode;

        @SerializedName("StartedAt")
        private String startedAt;

        @SerializedName("FinishedAt")
        private String finishedAt;

        public boolean isRunning() {
            return running;
        }

        public boolean isPaused() {
            return paused;
        }

        public int getPid() {
            return pid;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStartedAt() {
            return startedAt;
        }

        public String getFinishedAt() {
            return finishedAt;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
        }

        public void setPid(int pid) {
            this.pid = pid;
        }

        public void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        public void setStartedAt(String startedAt) {
            this.startedAt = startedAt;
        }

        public void setFinishedAt(String finishedAt) {
            this.finishedAt = finishedAt;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

}
