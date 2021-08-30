package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiParcelUsageParcel;
import com.sequenceiq.mock.swagger.model.ApiParcelUsageRack;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This object provides a complete view of the usage of parcels in a given cluster - particularly which parcels are in use for which roles.
 */
@ApiModel(description = "This object provides a complete view of the usage of parcels in a given cluster - particularly which parcels are in use for which roles.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiParcelUsage   {
  @JsonProperty("racks")
  @Valid
  private List<ApiParcelUsageRack> racks = null;

  @JsonProperty("parcels")
  @Valid
  private List<ApiParcelUsageParcel> parcels = null;

  public ApiParcelUsage racks(List<ApiParcelUsageRack> racks) {
    this.racks = racks;
    return this;
  }

  public ApiParcelUsage addRacksItem(ApiParcelUsageRack racksItem) {
    if (this.racks == null) {
      this.racks = new ArrayList<>();
    }
    this.racks.add(racksItem);
    return this;
  }

  /**
   * The racks that contain hosts that are part of this cluster.
   * @return racks
  **/
  @ApiModelProperty(value = "The racks that contain hosts that are part of this cluster.")

  @Valid

  public List<ApiParcelUsageRack> getRacks() {
    return racks;
  }

  public void setRacks(List<ApiParcelUsageRack> racks) {
    this.racks = racks;
  }

  public ApiParcelUsage parcels(List<ApiParcelUsageParcel> parcels) {
    this.parcels = parcels;
    return this;
  }

  public ApiParcelUsage addParcelsItem(ApiParcelUsageParcel parcelsItem) {
    if (this.parcels == null) {
      this.parcels = new ArrayList<>();
    }
    this.parcels.add(parcelsItem);
    return this;
  }

  /**
   * The parcel's that are activated and/or in-use on this cluster.
   * @return parcels
  **/
  @ApiModelProperty(value = "The parcel's that are activated and/or in-use on this cluster.")

  @Valid

  public List<ApiParcelUsageParcel> getParcels() {
    return parcels;
  }

  public void setParcels(List<ApiParcelUsageParcel> parcels) {
    this.parcels = parcels;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiParcelUsage apiParcelUsage = (ApiParcelUsage) o;
    return Objects.equals(this.racks, apiParcelUsage.racks) &&
        Objects.equals(this.parcels, apiParcelUsage.parcels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(racks, parcels);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiParcelUsage {\n");
    
    sb.append("    racks: ").append(toIndentedString(racks)).append("\n");
    sb.append("    parcels: ").append(toIndentedString(parcels)).append("\n");
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

