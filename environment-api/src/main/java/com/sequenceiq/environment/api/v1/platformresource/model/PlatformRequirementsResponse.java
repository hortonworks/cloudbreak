package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.platformresource.model.support.DataHubSupportRequirements;
import com.sequenceiq.environment.api.v1.platformresource.model.support.DataLakeSupportRequirements;
import com.sequenceiq.environment.api.v1.platformresource.model.support.DatabaseSupportRequirements;
import com.sequenceiq.environment.api.v1.platformresource.model.support.FreeIpaSupportRequirements;
import com.sequenceiq.environment.api.v1.platformresource.model.support.SupportStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformRequirementsResponse implements Serializable {

    private FreeIpaSupportRequirements freeIpaSupportRequirements;

    private DatabaseSupportRequirements databaseSupportRequirements;

    private DataHubSupportRequirements dataHubSupportRequirements;

    private DataLakeSupportRequirements dataLakeSupportRequirements;

    private SupportStatus supportStatus;

    public FreeIpaSupportRequirements getFreeIpaSupportRequirements() {
        return freeIpaSupportRequirements;
    }

    public void setFreeIpaSupportRequirements(FreeIpaSupportRequirements freeIpaSupportRequirements) {
        this.freeIpaSupportRequirements = freeIpaSupportRequirements;
    }

    public DatabaseSupportRequirements getDatabaseSupportRequirements() {
        return databaseSupportRequirements;
    }

    public void setDatabaseSupportRequirements(DatabaseSupportRequirements databaseSupportRequirements) {
        this.databaseSupportRequirements = databaseSupportRequirements;
    }

    public DataHubSupportRequirements getDataHubSupportRequirements() {
        return dataHubSupportRequirements;
    }

    public void setDataHubSupportRequirements(DataHubSupportRequirements dataHubSupportRequirements) {
        this.dataHubSupportRequirements = dataHubSupportRequirements;
    }

    public DataLakeSupportRequirements getDataLakeSupportRequirements() {
        return dataLakeSupportRequirements;
    }

    public void setDataLakeSupportRequirements(DataLakeSupportRequirements dataLakeSupportRequirements) {
        this.dataLakeSupportRequirements = dataLakeSupportRequirements;
    }

    public SupportStatus getSupportStatus() {
        return supportStatus;
    }

    public void setSupportStatus(SupportStatus supportStatus) {
        this.supportStatus = supportStatus;
    }

    @Override
    public String toString() {
        return "PlatformRequirementsResponse{" +
                "freeIpa=" + freeIpaSupportRequirements +
                ", database=" + databaseSupportRequirements +
                ", dataHub=" + dataHubSupportRequirements +
                ", dataLake=" + dataLakeSupportRequirements +
                ", supportStatus=" + supportStatus +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private FreeIpaSupportRequirements freeIpa;

        private DatabaseSupportRequirements database;

        private DataHubSupportRequirements dataHub;

        private DataLakeSupportRequirements dataLake;

        private Set<String> instanceTypesInRegion = new HashSet<>();

        private Set<String> freeIpaInstanceTypesInRegion = new HashSet<>();

        private Set<String> databaseInstanceTypesInRegion = new HashSet<>();

        public Builder withInstanceTypesInRegion(Set<String> instanceTypesInRegion) {
            this.instanceTypesInRegion = instanceTypesInRegion;
            return this;
        }

        public Builder withFreeIpaInstanceTypesInRegion(Set<String> freeIpaInstanceTypesInRegion) {
            this.freeIpaInstanceTypesInRegion = freeIpaInstanceTypesInRegion;
            return this;
        }

        public Builder withDatabaseInstanceTypesInRegion(Set<String> databaseInstanceTypesInRegion) {
            this.databaseInstanceTypesInRegion = databaseInstanceTypesInRegion;
            return this;
        }

        public Builder withFreeIpa(FreeIpaSupportRequirements freeIpa) {
            this.freeIpa = freeIpa;
            return this;
        }

        public Builder withDatabase(DatabaseSupportRequirements database) {
            this.database = database;
            return this;
        }

        public Builder withDataHub(DataHubSupportRequirements dataHub) {
            this.dataHub = dataHub;
            return this;
        }

        public Builder withDataLake(DataLakeSupportRequirements dataLake) {
            this.dataLake = dataLake;
            return this;
        }

        private SupportStatus supportStatus() {
            boolean everythingIsSupported = vmListsAreNotEmpty()
                    && freeIpaCanBeValidated()
                    && dataHubCanBeValidated()
                    && dataLakeCanBeValidated()
                    && databaseCanBeValidated();
            return everythingIsSupported ? SupportStatus.SUPPORTED : SupportStatus.NOT_SUPPORTED;
        }

        private boolean freeIpaCanBeValidated() {
            return this.freeIpa != null
                    && this.freeIpa.getMissingDefaultX86Instances().isEmpty()
                    && this.freeIpa.getMissingDefaultX86Instances().isEmpty();
        }

        private boolean databaseCanBeValidated() {
            return this.database != null
                    && this.database.getMissingDefaultArmInstanceTypes().isEmpty()
                    && this.database.getMissingDefaultX86Instances().isEmpty();
        }

        private boolean dataLakeCanBeValidated() {
            return this.dataLake != null
                    && this.dataLake.getMissingDefaultArmInstanceTypes().isEmpty()
                    && this.dataLake.getMissingDefaultX86Instances().isEmpty();
        }

        private boolean dataHubCanBeValidated() {
            return this.dataHub != null
                    && this.dataHub.getMissingDefaultArmInstanceTypes().isEmpty()
                    && this.dataHub.getMissingDefaultX86Instances().isEmpty();
        }

        private boolean vmListsAreNotEmpty() {
            return !instanceTypesInRegion.isEmpty()
                    && !databaseInstanceTypesInRegion.isEmpty()
                    && !freeIpaInstanceTypesInRegion.isEmpty();
        }

        public PlatformRequirementsResponse build() {
            PlatformRequirementsResponse response = new PlatformRequirementsResponse();
            response.setFreeIpaSupportRequirements(this.freeIpa);
            response.setDatabaseSupportRequirements(this.database);
            response.setDataHubSupportRequirements(this.dataHub);
            response.setDataLakeSupportRequirements(this.dataLake);
            response.setSupportStatus(supportStatus());
            return response;
        }
    }

}
