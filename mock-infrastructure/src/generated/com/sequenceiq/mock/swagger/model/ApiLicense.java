package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiLicenseFeature;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Information about the Cloudera Manager license.
 */
@ApiModel(description = "Information about the Cloudera Manager license.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiLicense   {
  @JsonProperty("owner")
  private String owner = null;

  @JsonProperty("uuid")
  private String uuid = null;

  @JsonProperty("expiration")
  private String expiration = null;

  @JsonProperty("features")
  @Valid
  private List<ApiLicenseFeature> features = null;

  @JsonProperty("deactivationDate")
  private String deactivationDate = null;

  @JsonProperty("startDate")
  private String startDate = null;

  public ApiLicense owner(String owner) {
    this.owner = owner;
    return this;
  }

  /**
   * The owner (organization name) of the license.
   * @return owner
  **/
  @ApiModelProperty(value = "The owner (organization name) of the license.")


  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public ApiLicense uuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * A UUID of this license.
   * @return uuid
  **/
  @ApiModelProperty(value = "A UUID of this license.")


  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public ApiLicense expiration(String expiration) {
    this.expiration = expiration;
    return this;
  }

  /**
   * The expiration date.
   * @return expiration
  **/
  @ApiModelProperty(value = "The expiration date.")


  public String getExpiration() {
    return expiration;
  }

  public void setExpiration(String expiration) {
    this.expiration = expiration;
  }

  public ApiLicense features(List<ApiLicenseFeature> features) {
    this.features = features;
    return this;
  }

  public ApiLicense addFeaturesItem(ApiLicenseFeature featuresItem) {
    if (this.features == null) {
      this.features = new ArrayList<>();
    }
    this.features.add(featuresItem);
    return this;
  }

  /**
   * Returns the list of available features as per the license
   * @return features
  **/
  @ApiModelProperty(value = "Returns the list of available features as per the license")

  @Valid

  public List<ApiLicenseFeature> getFeatures() {
    return features;
  }

  public void setFeatures(List<ApiLicenseFeature> features) {
    this.features = features;
  }

  public ApiLicense deactivationDate(String deactivationDate) {
    this.deactivationDate = deactivationDate;
    return this;
  }

  /**
   * The deactivation date.
   * @return deactivationDate
  **/
  @ApiModelProperty(value = "The deactivation date.")


  public String getDeactivationDate() {
    return deactivationDate;
  }

  public void setDeactivationDate(String deactivationDate) {
    this.deactivationDate = deactivationDate;
  }

  public ApiLicense startDate(String startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * The start date.
   * @return startDate
  **/
  @ApiModelProperty(value = "The start date.")


  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiLicense apiLicense = (ApiLicense) o;
    return Objects.equals(this.owner, apiLicense.owner) &&
        Objects.equals(this.uuid, apiLicense.uuid) &&
        Objects.equals(this.expiration, apiLicense.expiration) &&
        Objects.equals(this.features, apiLicense.features) &&
        Objects.equals(this.deactivationDate, apiLicense.deactivationDate) &&
        Objects.equals(this.startDate, apiLicense.startDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(owner, uuid, expiration, features, deactivationDate, startDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiLicense {\n");
    
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    expiration: ").append(toIndentedString(expiration)).append("\n");
    sb.append("    features: ").append(toIndentedString(features)).append("\n");
    sb.append("    deactivationDate: ").append(toIndentedString(deactivationDate)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

