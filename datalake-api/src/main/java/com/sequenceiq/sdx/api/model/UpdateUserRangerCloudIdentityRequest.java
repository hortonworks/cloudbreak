package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUserRangerCloudIdentityRequest {

    private static final String USER_DESCRIPTION = "The user to update ranger cloud identity mapping for";

    private static final String AZURE_USER_VALUE_DESCRIPTION = "The updated value for the user in the ranger azure user mapping. A null / not-present value " +
            "indicates the value will be removed from the azure user mapping";

    @NotNull
    @ApiModelProperty(USER_DESCRIPTION)
    private String user;

    @ApiModelProperty(AZURE_USER_VALUE_DESCRIPTION)
    private String azureUserValue;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAzureUserValue() {
        return azureUserValue;
    }

    public void setAzureUserValue(String azureUserValue) {
        this.azureUserValue = azureUserValue;
    }

    @Override
    public String toString() {
        return "SetSingleRangerCloudIdentityRequest{" +
                "user='" + user + '\'' +
                ", azureUserValue='" + azureUserValue + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdateUserRangerCloudIdentityRequest that = (UpdateUserRangerCloudIdentityRequest) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(azureUserValue, that.azureUserValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, azureUserValue);
    }

}
