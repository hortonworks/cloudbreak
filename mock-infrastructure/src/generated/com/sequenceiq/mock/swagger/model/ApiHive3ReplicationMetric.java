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
 * 
 */
@ApiModel(description = "")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHive3ReplicationMetric   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("currentCount")
  private Integer currentCount = null;

  @JsonProperty("totalCount")
  private Integer totalCount = null;

  public ApiHive3ReplicationMetric name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 
   * @return name
  **/
  @ApiModelProperty(value = "")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiHive3ReplicationMetric currentCount(Integer currentCount) {
    this.currentCount = currentCount;
    return this;
  }

  /**
   * 
   * @return currentCount
  **/
  @ApiModelProperty(value = "")


  public Integer getCurrentCount() {
    return currentCount;
  }

  public void setCurrentCount(Integer currentCount) {
    this.currentCount = currentCount;
  }

  public ApiHive3ReplicationMetric totalCount(Integer totalCount) {
    this.totalCount = totalCount;
    return this;
  }

  /**
   * 
   * @return totalCount
  **/
  @ApiModelProperty(value = "")


  public Integer getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Integer totalCount) {
    this.totalCount = totalCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHive3ReplicationMetric apiHive3ReplicationMetric = (ApiHive3ReplicationMetric) o;
    return Objects.equals(this.name, apiHive3ReplicationMetric.name) &&
        Objects.equals(this.currentCount, apiHive3ReplicationMetric.currentCount) &&
        Objects.equals(this.totalCount, apiHive3ReplicationMetric.totalCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, currentCount, totalCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHive3ReplicationMetric {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    currentCount: ").append(toIndentedString(currentCount)).append("\n");
    sb.append("    totalCount: ").append(toIndentedString(totalCount)).append("\n");
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

