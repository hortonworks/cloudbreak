package com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DescribeRemoteEnvironmentRequest {

    @JsonProperty
    private String accountId;

    @JsonProperty
    private String environment;

    @JsonProperty
    private OutputView outputView = OutputView.FULL;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public OutputView getOutputView() {
        return outputView;
    }

    public void setOutputView(OutputView outputView) {
        this.outputView = outputView;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DescribeRemoteEnvironmentRequest that = (DescribeRemoteEnvironmentRequest) o;
        return Objects.equals(accountId, that.accountId)
                && Objects.equals(environment, that.environment)
                && outputView == that.outputView;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, environment, outputView);
    }

    @Override
    public String toString() {
        return "DescribeRemoteEnvironmentRequest{" +
                "accountId='" + accountId + '\'' +
                ", environment='" + environment + '\'' +
                ", outputView=" + outputView +
                '}';
    }
}
