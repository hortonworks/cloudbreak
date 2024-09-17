package com.sequenceiq.cloudbreak.orchestrator.model;

public class BootstrapParams {

    public static final int DEFAULT_WORKER_THREADS = 5;

    public static final int WORKER_THREAD_REDUCTION_COUNT = 4;

    private String cloud;

    private String os;

    private boolean saltBootstrapFpSupported;

    private boolean restartNeededFlagSupported;

    private boolean restartNeeded;

    private Integer masterWorkerThreads = DEFAULT_WORKER_THREADS;

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public boolean isSaltBootstrapFpSupported() {
        return saltBootstrapFpSupported;
    }

    public void setSaltBootstrapFpSupported(boolean saltBootstrapFpSupported) {
        this.saltBootstrapFpSupported = saltBootstrapFpSupported;
    }

    public boolean isRestartNeededFlagSupported() {
        return restartNeededFlagSupported;
    }

    public void setRestartNeededFlagSupported(boolean restartNeededFlagSupported) {
        this.restartNeededFlagSupported = restartNeededFlagSupported;
    }

    public boolean isRestartNeeded() {
        return restartNeeded;
    }

    public void setRestartNeeded(boolean restartNeeded) {
        this.restartNeeded = restartNeeded;
    }

    public Integer getMasterWorkerThreads() {
        return masterWorkerThreads;
    }

    public void setMasterWorkerThreads(Integer masterWorkerThreads) {
        this.masterWorkerThreads = masterWorkerThreads;
    }

    @Override
    public String toString() {
        return "BootstrapParams{" +
                "cloud='" + cloud + '\'' +
                ", os='" + os + '\'' +
                ", saltBootstrapFpSupported=" + saltBootstrapFpSupported +
                ", restartNeededFlagSupported=" + restartNeededFlagSupported +
                ", restartNeeded=" + restartNeeded +
                ", masterWorkerThreads=" + masterWorkerThreads +
                '}';
    }
}
