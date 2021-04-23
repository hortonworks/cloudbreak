package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * 
 */
@ApiModel(description = "")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiHBaseReplicationInfo   {
  @JsonProperty("replicationOperations")
  @Valid
  private List<String> replicationOperations = null;

  public ApiHBaseReplicationInfo replicationOperations(List<String> replicationOperations) {
    this.replicationOperations = replicationOperations;
    return this;
  }

  public ApiHBaseReplicationInfo addReplicationOperationsItem(String replicationOperationsItem) {
    if (this.replicationOperations == null) {
      this.replicationOperations = new ArrayList<>();
    }
    this.replicationOperations.add(replicationOperationsItem);
    return this;
  }

  /**
   * 
   * @return replicationOperations
  **/
  @ApiModelProperty(value = "")


  public List<String> getReplicationOperations() {
    return replicationOperations;
  }

  public void setReplicationOperations(List<String> replicationOperations) {
    this.replicationOperations = replicationOperations;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHBaseReplicationInfo apiHBaseReplicationInfo = (ApiHBaseReplicationInfo) o;
    return Objects.equals(this.replicationOperations, apiHBaseReplicationInfo.replicationOperations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(replicationOperations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseReplicationInfo {\n");
    
    sb.append("    replicationOperations: ").append(toIndentedString(replicationOperations)).append("\n");
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

