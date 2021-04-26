package com.sequenceiq.environment.network.dto;

public class YarnParams {

    private String queue;

    private Integer lifetime;

    private YarnParams(Builder builder) {
        queue = builder.queue;
        lifetime = builder.lifetime;
    }

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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "YarnParams{" +
                "queue='" + queue + "', " +
                "lifetime='" + lifetime + '\'' +
                '}';
    }

    public static final class Builder {

        private String queue;

        private Integer lifetime;

        private Builder() {
        }

        public Builder withQueue(String queue) {
            this.queue = queue;
            return this;
        }

        public Builder withLifetime(Integer lifetime) {
            this.lifetime = lifetime;
            return this;
        }

        public YarnParams build() {
            return new YarnParams(this);
        }
    }
}
