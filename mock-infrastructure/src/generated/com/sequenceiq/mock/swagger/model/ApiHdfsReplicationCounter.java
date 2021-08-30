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
 * A counter in an HDFS replication job.
 */
@ApiModel(description = "A counter in an HDFS replication job.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsReplicationCounter   {
  @JsonProperty("group")
  private String group = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("value")
  private Integer value = null;

  public ApiHdfsReplicationCounter group(String group) {
    this.group = group;
    return this;
  }

  /**
   * 
   * @return group
  **/
  @ApiModelProperty(value = "")


  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public ApiHdfsReplicationCounter name(String name) {
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

  public ApiHdfsReplicationCounter value(Integer value) {
    this.value = value;
    return this;
  }

  /**
   * 
   * @return value
  **/
  @ApiModelProperty(value = "")


  public Integer getValue() {
    return value;
  }

  public void setValue(Integer value) {
    this.value = value;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsReplicationCounter apiHdfsReplicationCounter = (ApiHdfsReplicationCounter) o;
    return Objects.equals(this.group, apiHdfsReplicationCounter.group) &&
        Objects.equals(this.name, apiHdfsReplicationCounter.name) &&
        Objects.equals(this.value, apiHdfsReplicationCounter.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(group, name, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsReplicationCounter {\n");
    
    sb.append("    group: ").append(toIndentedString(group)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

