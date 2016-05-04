package com.sequenceiq.it.mock.restito.docker.model;

import com.google.gson.annotations.SerializedName;

public class InspectContainerResponse {

    @SerializedName("Id")
    private String id;

    @SerializedName("State")
    private ContainerState state;

    public void setId(String id) {
        this.id = id;
    }

    public void setState(ContainerState state) {
        this.state = state;
    }

    public static class ContainerState {

        @SerializedName("Running")
        private boolean running;

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }
    }

}
