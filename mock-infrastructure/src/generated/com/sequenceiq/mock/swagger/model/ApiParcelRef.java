package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A parcelRef references a parcel. Each parcel is identified by its \&quot;parcelName\&quot; and \&quot;parcelVersion\&quot;, and the \&quot;clusterName\&quot; of the cluster that is using it. To operate on the parcel object, use the API with the those fields as parameters.
 */
@ApiModel(description = "A parcelRef references a parcel. Each parcel is identified by its \"parcelName\" and \"parcelVersion\", and the \"clusterName\" of the cluster that is using it. To operate on the parcel object, use the API with the those fields as parameters.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiParcelRef   {
  @JsonProperty("clusterName")
  private String clusterName = null;

  @JsonProperty("parcelName")
  private String parcelName = null;

  @JsonProperty("parcelVersion")
  private String parcelVersion = null;

  @JsonProperty("parcelDisplayName")
  private String parcelDisplayName = null;

  public ApiParcelRef clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  /**
   * The name of the cluster that the parcel is used by.
   * @return clusterName
  **/
  @ApiModelProperty(value = "The name of the cluster that the parcel is used by.")


  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public ApiParcelRef parcelName(String parcelName) {
    this.parcelName = parcelName;
    return this;
  }

  /**
   * The name of the parcel.
   * @return parcelName
  **/
  @ApiModelProperty(value = "The name of the parcel.")


  public String getParcelName() {
    return parcelName;
  }

  public void setParcelName(String parcelName) {
    this.parcelName = parcelName;
  }

  public ApiParcelRef parcelVersion(String parcelVersion) {
    this.parcelVersion = parcelVersion;
    return this;
  }

  /**
   * The version of the parcel.
   * @return parcelVersion
  **/
  @ApiModelProperty(value = "The version of the parcel.")


  public String getParcelVersion() {
    return parcelVersion;
  }

  public void setParcelVersion(String parcelVersion) {
    this.parcelVersion = parcelVersion;
  }

  public ApiParcelRef parcelDisplayName(String parcelDisplayName) {
    this.parcelDisplayName = parcelDisplayName;
    return this;
  }

  /**
   * The display name of the parcel. If set, available since v40.
   * @return parcelDisplayName
  **/
  @ApiModelProperty(value = "The display name of the parcel. If set, available since v40.")


  public String getParcelDisplayName() {
    return parcelDisplayName;
  }

  public void setParcelDisplayName(String parcelDisplayName) {
    this.parcelDisplayName = parcelDisplayName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiParcelRef apiParcelRef = (ApiParcelRef) o;
    return Objects.equals(this.clusterName, apiParcelRef.clusterName) &&
        Objects.equals(this.parcelName, apiParcelRef.parcelName) &&
        Objects.equals(this.parcelVersion, apiParcelRef.parcelVersion) &&
        Objects.equals(this.parcelDisplayName, apiParcelRef.parcelDisplayName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterName, parcelName, parcelVersion, parcelDisplayName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiParcelRef {\n");
    
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    parcelName: ").append(toIndentedString(parcelName)).append("\n");
    sb.append("    parcelVersion: ").append(toIndentedString(parcelVersion)).append("\n");
    sb.append("    parcelDisplayName: ").append(toIndentedString(parcelDisplayName)).append("\n");
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

