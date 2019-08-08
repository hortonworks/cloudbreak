package com.sequenceiq.cloudbreak.clusterproxy;

public class ConfigRegistrationResponse {
    private String result;

    private String x509Unwrapped;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getX509Unwrapped() {
        return x509Unwrapped;
    }

    public void setX509Unwrapped(String x509Unwrapped) {
        this.x509Unwrapped = x509Unwrapped;
    }

    @Override
    public String toString() {
        return "ConfigRegistrationResponse{result='" + result + '\'' + ", x509Unwrapped='" + x509Unwrapped + '\'' + '}';
    }
}
