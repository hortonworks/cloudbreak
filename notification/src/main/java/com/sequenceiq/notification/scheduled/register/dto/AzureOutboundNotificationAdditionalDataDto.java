package com.sequenceiq.notification.scheduled.register.dto;

import java.util.Objects;

public class AzureOutboundNotificationAdditionalDataDto extends BaseNotificationRegisterAdditionalDataDto {

    private final String creator;

    private final String status;

    private final String emailableStatus;

    protected AzureOutboundNotificationAdditionalDataDto(
            String name,
            String resourceCrn,
            String creator,
            String status) {
        super(name, resourceCrn);
        this.creator = creator;
        this.status = status;
        this.emailableStatus = toCamelCase(status);
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
        AzureOutboundNotificationAdditionalDataDto that = (AzureOutboundNotificationAdditionalDataDto) o;
        return super.equals(o) && Objects.equals(creator, that.creator) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), creator, status);
    }

    @Override
    public String toString() {
        return "AzureOutboundNotificationRegisterDto{" +
                "creator='" + creator + '\'' +
                ", status='" + status + '\'' +
                super.toString() +
                '}';
    }

    public static AzureOutboundNotificationAdditionalDataDtoBuilder builder() {
        return new AzureOutboundNotificationAdditionalDataDtoBuilder();
    }

    public static class AzureOutboundNotificationAdditionalDataDtoBuilder {

        private String creator;

        private String status;

        private String name;

        private String crn;

        public AzureOutboundNotificationAdditionalDataDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public AzureOutboundNotificationAdditionalDataDtoBuilder crn(String crn) {
            this.crn = crn;
            return this;
        }

        public AzureOutboundNotificationAdditionalDataDtoBuilder creator(String creator) {
            this.creator = creator;
            return this;
        }

        public AzureOutboundNotificationAdditionalDataDtoBuilder status(String status) {
            this.status = status;
            return this;
        }

        public AzureOutboundNotificationAdditionalDataDto build() {
            return new AzureOutboundNotificationAdditionalDataDto(this.name, this.crn, this.creator, this.status);
        }
    }
}
