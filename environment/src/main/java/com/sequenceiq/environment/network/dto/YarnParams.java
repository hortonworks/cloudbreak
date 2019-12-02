package com.sequenceiq.environment.network.dto;

public class YarnParams {

    private String queue;

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    @Override
    public String toString() {
        return "YarnParams{" +
                "queue='" + queue + '\'' +
                '}';
    }

    public static final class YarnParamsBuilder {
        private String queue;

        private YarnParamsBuilder() {
        }

        public static YarnParamsBuilder anYarnParams() {
            return new YarnParamsBuilder();
        }

        public YarnParamsBuilder withQueue(String queue) {
            this.queue = queue;
            return this;
        }

        public YarnParams build() {
            YarnParams yarnParams = new YarnParams();
            yarnParams.setQueue(queue);
            return yarnParams;
        }
    }
}
