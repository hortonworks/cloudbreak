package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Set;

import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentNetworkV1Response")
public class EnvironmentNetworkResponse extends EnvironmentNetworkRequest {
    @ApiModelProperty(ModelDescriptions.ID)
    private String id;

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static final class EnvironmentNetworkResponseBuilder {
        private String id;

        private String name;

        private Set<String> subnetIds;

        private EnvironmentNetworkAwsParams aws;

        private EnvironmentNetworkAzureParams azure;

        private EnvironmentNetworkResponseBuilder() {
        }

        public static EnvironmentNetworkResponseBuilder anEnvironmentNetworkResponse() {
            return new EnvironmentNetworkResponseBuilder();
        }

        public EnvironmentNetworkResponseBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withSubnetIds(Set<String> subnetIds) {
            this.subnetIds = subnetIds;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withAws(EnvironmentNetworkAwsParams aws) {
            this.aws = aws;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withAzure(EnvironmentNetworkAzureParams azure) {
            this.azure = azure;
            return this;
        }

        public EnvironmentNetworkResponse build() {
            EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
            environmentNetworkResponse.setId(id);
            environmentNetworkResponse.setName(name);
            environmentNetworkResponse.setSubnetIds(subnetIds);
            environmentNetworkResponse.setAws(aws);
            environmentNetworkResponse.setAzure(azure);
            return environmentNetworkResponse;
        }
    }
}
