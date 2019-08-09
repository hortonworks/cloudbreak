package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigRegistrationResponse that = (ConfigRegistrationResponse) o;

        return Objects.equals(result, that.result) &&
                Objects.equals(x509Unwrapped, that.x509Unwrapped);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, x509Unwrapped);
    }

    @Override
    public String toString() {
        return "ConfigRegistrationResponse{result='" + result + '\'' + ", x509Unwrapped='" + x509Unwrapped + '\'' + '}';
    }
}
