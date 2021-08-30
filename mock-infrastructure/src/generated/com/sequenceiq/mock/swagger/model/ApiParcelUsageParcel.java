package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiParcelRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This object is used to represent a parcel within an ApiParcelUsage.
 */
@ApiModel(description = "This object is used to represent a parcel within an ApiParcelUsage.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiParcelUsageParcel   {
  @JsonProperty("parcelRef")
  private ApiParcelRef parcelRef = null;

  @JsonProperty("processCount")
  private Integer processCount = null;

  @JsonProperty("activated")
  private Boolean activated = null;

  public ApiParcelUsageParcel parcelRef(ApiParcelRef parcelRef) {
    this.parcelRef = parcelRef;
    return this;
  }

  /**
   * Reference to the corresponding Parcel object.
   * @return parcelRef
  **/
  @ApiModelProperty(value = "Reference to the corresponding Parcel object.")

  @Valid

  public ApiParcelRef getParcelRef() {
    return parcelRef;
  }

  public void setParcelRef(ApiParcelRef parcelRef) {
    this.parcelRef = parcelRef;
  }

  public ApiParcelUsageParcel processCount(Integer processCount) {
    this.processCount = processCount;
    return this;
  }

  /**
   * How many running processes on the cluster are using the parcel.
   * @return processCount
  **/
  @ApiModelProperty(value = "How many running processes on the cluster are using the parcel.")


  public Integer getProcessCount() {
    return processCount;
  }

  public void setProcessCount(Integer processCount) {
    this.processCount = processCount;
  }

  public ApiParcelUsageParcel activated(Boolean activated) {
    this.activated = activated;
    return this;
  }

  /**
   * Is this parcel currently activated on the cluster.
   * @return activated
  **/
  @ApiModelProperty(value = "Is this parcel currently activated on the cluster.")


  public Boolean isActivated() {
    return activated;
  }

  public void setActivated(Boolean activated) {
    this.activated = activated;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiParcelUsageParcel apiParcelUsageParcel = (ApiParcelUsageParcel) o;
    return Objects.equals(this.parcelRef, apiParcelUsageParcel.parcelRef) &&
        Objects.equals(this.processCount, apiParcelUsageParcel.processCount) &&
        Objects.equals(this.activated, apiParcelUsageParcel.activated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parcelRef, processCount, activated);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiParcelUsageParcel {\n");
    
    sb.append("    parcelRef: ").append(toIndentedString(parcelRef)).append("\n");
    sb.append("    processCount: ").append(toIndentedString(processCount)).append("\n");
    sb.append("    activated: ").append(toIndentedString(activated)).append("\n");
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

