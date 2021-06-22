package com.sequenceiq.cloudbreak.audit.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class ResultServiceEventData extends ResultEventData {

    private final List<String> resourceCrns;

    private final String resultDetails;

    public ResultServiceEventData(Builder builder) {
        this.resourceCrns = builder.resourceCrns;
        this.resultDetails = builder.resultDetails;
    }

    public List<String> getResourceCrns() {
        return resourceCrns;
    }

    public String getResultDetails() {
        return resultDetails;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ResultServiceEventData{" +
                "resourceCrns=" + resourceCrns +
                ", resultDetails='" + resultDetails + '\'' +
                "} " + super.toString();
    }

    public static class Builder {

        private List<String> resourceCrns;

        private String resultDetails;

        public Builder withResourceCrns(List<String> resourceCrns) {
            this.resourceCrns = List.copyOf(resourceCrns);
            return this;
        }

        public Builder withResultDetails(String resultDetails) {
            this.resultDetails = resultDetails;
            return this;
        }

        public ResultServiceEventData build() {
            checkArgument(resourceCrns.stream().allMatch(Crn::isCrn), "All CRNs must be valid.");
            checkArgument(StringUtils.isEmpty(resultDetails) || JsonUtil.isValid(resultDetails), "Result Details must be a valid JSON.");
            return new ResultServiceEventData(this);
        }
    }
}
