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
 * A clusterRef references a cluster. To operate on the cluster object, use the cluster API with the clusterName as the parameter.
 */
@ApiModel(description = "A clusterRef references a cluster. To operate on the cluster object, use the cluster API with the clusterName as the parameter.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterRef   {
  @JsonProperty("clusterName")
  private String clusterName = null;

  @JsonProperty("displayName")
  private String displayName = null;

  public ApiClusterRef clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  /**
   * The name of the cluster, which uniquely identifies it in a CM installation.
   * @return clusterName
  **/
  @ApiModelProperty(value = "The name of the cluster, which uniquely identifies it in a CM installation.")


  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public ApiClusterRef displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * The display name of the cluster. This is available from v30.
   * @return displayName
  **/
  @ApiModelProperty(value = "The display name of the cluster. This is available from v30.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterRef apiClusterRef = (ApiClusterRef) o;
    return Objects.equals(this.clusterName, apiClusterRef.clusterName) &&
        Objects.equals(this.displayName, apiClusterRef.displayName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterName, displayName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterRef {\n");
    
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
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

