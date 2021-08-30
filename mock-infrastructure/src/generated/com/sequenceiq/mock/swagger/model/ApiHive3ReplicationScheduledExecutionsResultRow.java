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




public class ApiHive3ReplicationScheduledExecutionsResultRow   {
  @JsonProperty("executionId")
  private Integer executionId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("queryId")
  private String queryId = null;

  @JsonProperty("state")
  private String state = null;

  @JsonProperty("startTime")
  private String startTime = null;

  @JsonProperty("endTime")
  private String endTime = null;

  @JsonProperty("elapsed")
  private Integer elapsed = null;

  @JsonProperty("errorMessage")
  private String errorMessage = null;

  @JsonProperty("lastUpdateTime")
  private String lastUpdateTime = null;

  public ApiHive3ReplicationScheduledExecutionsResultRow executionId(Integer executionId) {
    this.executionId = executionId;
    return this;
  }

  /**
   * 
   * @return executionId
  **/
  @ApiModelProperty(value = "")


  public Integer getExecutionId() {
    return executionId;
  }

  public void setExecutionId(Integer executionId) {
    this.executionId = executionId;
  }

  public ApiHive3ReplicationScheduledExecutionsResultRow name(String name) {
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

  public ApiHive3ReplicationScheduledExecutionsResultRow queryId(String queryId) {
    this.queryId = queryId;
    return this;
  }

  /**
   * 
   * @return queryId
  **/
  @ApiModelProperty(value = "")


  public String getQueryId() {
    return queryId;
  }

  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }

  public ApiHive3ReplicationScheduledExecutionsResultRow state(String state) {
    this.state = state;
    return this;
  }

  /**
   * 
   * @return state
  **/
  @ApiModelProperty(value = "")


  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public ApiHive3ReplicationScheduledExecutionsResultRow startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * 
   * @return startTime
  **/
  @ApiModelProperty(value = "")


  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public ApiHive3ReplicationScheduledExecutionsResultRow endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * 
   * @return endTime
  **/
  @ApiModelProperty(value = "")


  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public ApiHive3ReplicationScheduledExecutionsResultRow elapsed(Integer elapsed) {
    this.elapsed = elapsed;
    return this;
  }

  /**
   * 
   * @return elapsed
  **/
  @ApiModelProperty(value = "")


  public Integer getElapsed() {
    return elapsed;
  }

  public void setElapsed(Integer elapsed) {
    this.elapsed = elapsed;
  }

  public ApiHive3ReplicationScheduledExecutionsResultRow errorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * 
   * @return errorMessage
  **/
  @ApiModelProperty(value = "")


  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public ApiHive3ReplicationScheduledExecutionsResultRow lastUpdateTime(String lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
    return this;
  }

  /**
   * 
   * @return lastUpdateTime
  **/
  @ApiModelProperty(value = "")


  public String getLastUpdateTime() {
    return lastUpdateTime;
  }

  public void setLastUpdateTime(String lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHive3ReplicationScheduledExecutionsResultRow apiHive3ReplicationScheduledExecutionsResultRow = (ApiHive3ReplicationScheduledExecutionsResultRow) o;
    return Objects.equals(this.executionId, apiHive3ReplicationScheduledExecutionsResultRow.executionId) &&
        Objects.equals(this.name, apiHive3ReplicationScheduledExecutionsResultRow.name) &&
        Objects.equals(this.queryId, apiHive3ReplicationScheduledExecutionsResultRow.queryId) &&
        Objects.equals(this.state, apiHive3ReplicationScheduledExecutionsResultRow.state) &&
        Objects.equals(this.startTime, apiHive3ReplicationScheduledExecutionsResultRow.startTime) &&
        Objects.equals(this.endTime, apiHive3ReplicationScheduledExecutionsResultRow.endTime) &&
        Objects.equals(this.elapsed, apiHive3ReplicationScheduledExecutionsResultRow.elapsed) &&
        Objects.equals(this.errorMessage, apiHive3ReplicationScheduledExecutionsResultRow.errorMessage) &&
        Objects.equals(this.lastUpdateTime, apiHive3ReplicationScheduledExecutionsResultRow.lastUpdateTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(executionId, name, queryId, state, startTime, endTime, elapsed, errorMessage, lastUpdateTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHive3ReplicationScheduledExecutionsResultRow {\n");
    
    sb.append("    executionId: ").append(toIndentedString(executionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    queryId: ").append(toIndentedString(queryId)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    elapsed: ").append(toIndentedString(elapsed)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    lastUpdateTime: ").append(toIndentedString(lastUpdateTime)).append("\n");
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

