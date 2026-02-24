package com.sequenceiq.notification.scheduled.register.dto.clusterhealth;

import static com.sequenceiq.notification.util.NotificationUtil.toCamelCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDto;

public class ClusterHealthNotificationAdditionalDataDto extends BaseNotificationRegisterAdditionalDataDto {

    private final String creator;

    private final String status;

    private final String emailableStatus;

    private final Set<InstanceStatusDto> instances;

    private final String stackType;

    private final String dateTimeString;

    private final String detailedStatus;

    private final String statusReason;

    private final String operation;

    private final String controlPlaneUrl;

    @SuppressWarnings("ExecutableStatementCount")
    private ClusterHealthNotificationAdditionalDataDto(ClusterHealthNotificationAdditionalDataDtoBuilder builder) {
        super(builder.name, builder.crn);
        this.creator = builder.creator;
        this.status = builder.status;
        this.emailableStatus = toCamelCase(builder.status);
        this.instances = builder.instances;
        this.stackType = builder.stackType;
        this.dateTimeString = builder.dateTimeString;
        this.detailedStatus = builder.detailedStatus;
        this.statusReason = builder.statusReason;
        this.operation = builder.operation;
        this.controlPlaneUrl = builder.controlPlaneUrl;
    }

    public String getCreator() {
        return creator;
    }

    public String getStatus() {
        return status;
    }

    public String getEmailableStatus() {
        return emailableStatus;
    }

    public Set<InstanceStatusDto> getInstances() {
        return instances;
    }

    public String getStackType() {
        return stackType;
    }

    public String getDateTimeString() {
        return dateTimeString;
    }

    public String getDetailedStatus() {
        return detailedStatus;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public String getOperation() {
        return operation;
    }

    public String getControlPlaneUrl() {
        return controlPlaneUrl;
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ClusterHealthNotificationAdditionalDataDto that = (ClusterHealthNotificationAdditionalDataDto) o;
        return Objects.equals(creator, that.creator) &&
                Objects.equals(status, that.status) &&
                Objects.equals(instances, that.instances) &&
                Objects.equals(stackType, that.stackType) &&
                Objects.equals(dateTimeString, that.dateTimeString) &&
                Objects.equals(detailedStatus, that.detailedStatus) &&
                Objects.equals(statusReason, that.statusReason) &&
                Objects.equals(operation, that.operation) &&
                Objects.equals(controlPlaneUrl, that.controlPlaneUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), creator, status, instances, stackType, dateTimeString,
                detailedStatus, statusReason, operation, controlPlaneUrl);
    }

    @Override
    public String toString() {
        return "ClusterHealthNotificationAdditionalDataDto{" +
                "creator='" + creator + '\'' +
                ", status='" + status + '\'' +
                ", instanceStatusDtos=" + instances +
                ", stackType='" + stackType + '\'' +
                ", dateTimeString='" + dateTimeString + '\'' +
                ", detailedStatus='" + detailedStatus + '\'' +
                ", statusReason='" + statusReason + '\'' +
                ", operation='" + operation + '\'' +
                ", controlPlaneUrl='" + controlPlaneUrl + '\'' +
                super.toString() +
                '}';
    }

    public static ClusterHealthNotificationAdditionalDataDtoBuilder builder() {
        return new ClusterHealthNotificationAdditionalDataDtoBuilder();
    }

    public static class ClusterHealthNotificationAdditionalDataDtoBuilder {

        private String creator;

        private String status;

        private String name;

        private String crn;

        private Set<InstanceStatusDto> instances = new HashSet<>();

        private String stackType;

        private String dateTimeString;

        private String detailedStatus;

        private String statusReason;

        private String operation;

        private String controlPlaneUrl;

        public ClusterHealthNotificationAdditionalDataDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder crn(String crn) {
            this.crn = crn;
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder creator(String creator) {
            this.creator = creator;
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder status(String status) {
            this.status = toCamelCase(status);
            this.operation = toCamelCase(status.toLowerCase(Locale.ROOT).split("_")[0]);
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder instances(Set<InstanceStatusDto> instanceStatusDtos) {
            this.instances = instanceStatusDtos;
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder addInstance(InstanceStatusDto instanceStatusDto) {
            this.instances.add(instanceStatusDto);
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder stackType(String stackType) {
            this.stackType = stackType;
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder dateTimeString(String dateTimeString) {
            this.dateTimeString = dateTimeString;
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder detailedStatus(String detailedStatus) {
            this.detailedStatus = toCamelCase(detailedStatus);
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder statusReason(String statusReason) {
            this.statusReason = statusReason;
            return this;
        }

        public ClusterHealthNotificationAdditionalDataDtoBuilder controlPlaneUrl(String controlPlaneUrl) {
            this.controlPlaneUrl = controlPlaneUrl;
            return this;
        }

        protected String toCamelCase(String input) {
            if (input == null || input.isEmpty()) {
                return input;
            }
            String result = Arrays.stream(input.toLowerCase().split("_"))
                    .filter(word -> !word.isEmpty())
                    .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1) + " ")
                    .collect(Collectors.joining());
            return result.trim();
        }

        public ClusterHealthNotificationAdditionalDataDto build() {
            return new ClusterHealthNotificationAdditionalDataDto(this);
        }
    }
}
