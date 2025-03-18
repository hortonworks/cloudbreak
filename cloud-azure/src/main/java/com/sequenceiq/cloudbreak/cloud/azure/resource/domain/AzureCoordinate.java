package com.sequenceiq.cloudbreak.cloud.azure.resource.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.common.domain.CdpSupportedServices;

public class AzureCoordinate extends Coordinate {

    public AzureCoordinate(AzureCoordinateBuilder builder) {
        super(
                builder.longitude,
                builder.latitude,
                builder.displayName,
                builder.key,
                builder.k8sSupported,
                builder.entitlements,
                builder.defaultDbVmType,
                null,
                builder.cdpSupportedServices);
    }

    public static AzureCoordinate coordinate(AzureCoordinateBuilder builder) {
        return new AzureCoordinate(builder);
    }

    public static final class AzureCoordinateBuilder {

        private Double longitude;

        private Double latitude;

        private String displayName;

        private String key;

        private boolean k8sSupported;

        private List<String> entitlements = new ArrayList<>();

        private String defaultDbVmType;

        private Set<CdpSupportedServices> cdpSupportedServices = new HashSet<>();

        public AzureCoordinateBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public AzureCoordinateBuilder longitude(String longitude) {
            this.longitude = Double.valueOf(longitude);
            return this;
        }

        public AzureCoordinateBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public AzureCoordinateBuilder latitude(String latitude) {
            this.latitude = Double.valueOf(latitude);
            return this;
        }

        public AzureCoordinateBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public AzureCoordinateBuilder key(String key) {
            this.key = key;
            return this;
        }

        public AzureCoordinateBuilder k8sSupported(boolean k8sSupported) {
            this.k8sSupported = k8sSupported;
            return this;
        }

        public AzureCoordinateBuilder entitlements(List<String> entitlements) {
            this.entitlements = entitlements;
            return this;
        }

        public AzureCoordinateBuilder defaultDbVmType(String defaultDbVmType) {
            this.defaultDbVmType = defaultDbVmType;
            return this;
        }

        public AzureCoordinateBuilder cdpSupportedServices(Set<CdpSupportedServices> cdpSupportedServices) {
            this.cdpSupportedServices = cdpSupportedServices;
            return this;
        }

        public static AzureCoordinateBuilder builder() {
            return new AzureCoordinateBuilder();
        }

        public AzureCoordinate build() {
            return new AzureCoordinate(this);
        }

        public static AzureCoordinateBuilder defaultBuilder() {
            return new AzureCoordinateBuilder()
                    .longitude(Double.parseDouble("36.7477169"))
                    .latitude(Double.parseDouble("-119.7729841"))
                    .displayName("California (West US)")
                    .key("us-west-1")
                    .k8sSupported(false)
                    .defaultDbVmType(null)
                    .entitlements(new ArrayList<>())
                    .cdpSupportedServices(new HashSet<>());
        }
    }

}
