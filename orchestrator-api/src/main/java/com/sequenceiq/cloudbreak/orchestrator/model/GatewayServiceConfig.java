package com.sequenceiq.cloudbreak.orchestrator.model;

public final class GatewayServiceConfig {

    private final boolean lakeHouseOptimizerSupportHttps;

    public GatewayServiceConfig(boolean lakeHouseOptimizerSupportHttps) {
        this.lakeHouseOptimizerSupportHttps = lakeHouseOptimizerSupportHttps;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean getLakeHouseOptimizerSupportHttps() {
        return lakeHouseOptimizerSupportHttps;
    }

    @Override
    public String toString() {
        return "GatewayConfig{" +
                "lakeHouseOptimizerSupportHttps='" + lakeHouseOptimizerSupportHttps + '\'' +
                '}';
    }

    public static class Builder {

        private boolean lakeHouseOptimizerSupportHttps;

        public Builder() {
        }

        public Builder withLakeHouseOptimizerSupportHttps(boolean lakeHouseOptimizerSupportHttps) {
            this.lakeHouseOptimizerSupportHttps = lakeHouseOptimizerSupportHttps;
            return this;
        }

        public GatewayServiceConfig build() {
            return new GatewayServiceConfig(lakeHouseOptimizerSupportHttps);
        }
    }
}
