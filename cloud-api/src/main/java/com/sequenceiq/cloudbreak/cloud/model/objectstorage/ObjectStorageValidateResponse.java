package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;

import java.util.Objects;

public class ObjectStorageValidateResponse {

    private ResponseStatus status;

    private String error;

    public ObjectStorageValidateResponse() {
    }

    public ObjectStorageValidateResponse(Builder builder) {
        this.status = builder.status;
        this.error = builder.error;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ObjectStorageMetadataResponse{" +
                ", status=" + status +
                ", error=" + error +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        ObjectStorageValidateResponse response = (ObjectStorageValidateResponse) o;
        return Objects.equals(status, response.status) && Objects.equals(error, response.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, error);
    }

    public static class Builder {

        private ResponseStatus status;

        private String error;

        public Builder withStatus(ResponseStatus status) {
            this.status = status;
            return this;
        }

        public Builder withError(String error) {
            this.error = error;
            return this;
        }

        public ObjectStorageValidateResponse build() {
            return new ObjectStorageValidateResponse(this);
        }
    }
}
