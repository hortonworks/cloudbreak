package com.sequenceiq.flow.domain;

public class RetryableFlow {
    private String name;

    private Long failDate;

    private RetryableFlow(String name, Long failDate) {
        this.name = name;
        this.failDate = failDate;
    }

    public String getName() {
        return name;
    }

    public Long getFailDate() {
        return failDate;
    }

    public static class RetryableFlowBuilder {

        private String name;

        private Long failDate;

        private RetryableFlowBuilder() {

        }

        public static RetryableFlowBuilder builder() {
            return new RetryableFlowBuilder();
        }

        public RetryableFlowBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public RetryableFlowBuilder setFailDate(Long failDate) {
            this.failDate = failDate;
            return this;
        }

        public RetryableFlow build() {
            return new RetryableFlow(name, failDate);
        }
    }
}
