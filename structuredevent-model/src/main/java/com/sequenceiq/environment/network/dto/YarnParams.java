package com.sequenceiq.environment.network.dto;

public class YarnParams {

    private String queue;

    private Integer lifetime;

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    public String toString() {
        return "YarnParams{" +
                "queue='" + queue + "', " +
                "lifetime='" + lifetime + '\'' +
                '}';
    }

    public static final class YarnParamsBuilder {

        private String queue;

        private Integer lifetime;

        private YarnParamsBuilder() {
        }

        public static YarnParamsBuilder anYarnParams() {
            return new YarnParamsBuilder();
        }

        public YarnParamsBuilder withQueue(String queue) {
            this.queue = queue;
            return this;
        }

        public YarnParamsBuilder withLifetime(Integer lifetime) {
            this.lifetime = lifetime;
            return this;
        }

        public YarnParams build() {
            YarnParams yarnParams = new YarnParams();
            yarnParams.setQueue(queue);
            yarnParams.setLifetime(lifetime);
            return yarnParams;
        }
    }
}
