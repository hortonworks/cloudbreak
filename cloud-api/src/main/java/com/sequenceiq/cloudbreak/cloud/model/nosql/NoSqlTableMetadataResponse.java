package com.sequenceiq.cloudbreak.cloud.model.nosql;

import java.util.Objects;

import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;

public class NoSqlTableMetadataResponse {

    private ResponseStatus status;

    private String id;

    private String tableStatus;

    public NoSqlTableMetadataResponse() {
    }

    public NoSqlTableMetadataResponse(Builder builder) {
        this.status = builder.status;
        this.id = builder.id;
        this.tableStatus = builder.tableStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTableStatus() {
        return tableStatus;
    }

    public void setTableStatus(String tableStatus) {
        this.tableStatus = tableStatus;
    }

    @Override
    public String toString() {
        return "NoSqlTableMetadataResponse{" +
                "status=" + status +
                ", id='" + id + '\'' +
                ", tableStatus='" + tableStatus + '\'' +
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
        NoSqlTableMetadataResponse that = (NoSqlTableMetadataResponse) o;
        return status == that.status &&
                Objects.equals(id, that.id) &&
                Objects.equals(tableStatus, that.tableStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, id, tableStatus);
    }

    public static class Builder {

        private ResponseStatus status;

        private String id;

        private String tableStatus;

        public Builder withStatus(ResponseStatus status) {
            this.status = status;
            return this;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withTableStatus(String tableStatus) {
            this.tableStatus = tableStatus;
            return this;
        }

        public NoSqlTableMetadataResponse build() {
            return new NoSqlTableMetadataResponse(this);
        }
    }

}
