package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class SubnetSelectionResult {

    private List<CloudSubnet> result = Collections.emptyList();

    private String errorMessage;

    public SubnetSelectionResult(List<CloudSubnet> result) {
        this.result = result;
    }

    public SubnetSelectionResult(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean hasError() {
        return StringUtils.isNotEmpty(errorMessage);
    }

    public boolean hasResult() {
        return StringUtils.isEmpty(errorMessage) && !result.isEmpty();
    }

    public List<CloudSubnet> getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "SubnetSelectionResult{" +
                "result=" + result +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
