package com.sequenceiq.environment.api.v1.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationParameters {

    private String distributionListId;

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getDistributionListId() {
        return distributionListId;
    }

    public void setDistributionListId(String distributionListId) {
        this.distributionListId = distributionListId;
    }

    @Override
    public String toString() {
        return "NotificationParameters{" +
                "distributionListId='" + distributionListId + '\'' +
                '}';
    }

    public static class Builder {

        private String distributionListId;

        public Builder withDistributionListId(String distributionListId) {
            this.distributionListId = distributionListId;
            return this;
        }

        public NotificationParameters build() {
            NotificationParameters notificationParameters = new NotificationParameters();
            notificationParameters.setDistributionListId(distributionListId);
            return notificationParameters;
        }
    }
}
