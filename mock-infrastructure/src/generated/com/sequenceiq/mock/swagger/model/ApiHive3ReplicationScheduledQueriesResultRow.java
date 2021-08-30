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




public class ApiHive3ReplicationScheduledQueriesResultRow   {
  @JsonProperty("queryId")
  private Integer queryId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("enabled")
  private Boolean enabled = null;

  @JsonProperty("nameSpace")
  private String nameSpace = null;

  @JsonProperty("schedule")
  private String schedule = null;

  @JsonProperty("user")
  private String user = null;

  @JsonProperty("query")
  private String query = null;

  @JsonProperty("nextExecution")
  private String nextExecution = null;

  @JsonProperty("executionId")
  private Integer executionId = null;

  public ApiHive3ReplicationScheduledQueriesResultRow queryId(Integer queryId) {
    this.queryId = queryId;
    return this;
  }

  /**
   * 
   * @return queryId
  **/
  @ApiModelProperty(value = "")


  public Integer getQueryId() {
    return queryId;
  }

  public void setQueryId(Integer queryId) {
    this.queryId = queryId;
  }

  public ApiHive3ReplicationScheduledQueriesResultRow name(String name) {
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

  public ApiHive3ReplicationScheduledQueriesResultRow enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * 
   * @return enabled
  **/
  @ApiModelProperty(value = "")


  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ApiHive3ReplicationScheduledQueriesResultRow nameSpace(String nameSpace) {
    this.nameSpace = nameSpace;
    return this;
  }

  /**
   * 
   * @return nameSpace
  **/
  @ApiModelProperty(value = "")


  public String getNameSpace() {
    return nameSpace;
  }

  public void setNameSpace(String nameSpace) {
    this.nameSpace = nameSpace;
  }

  public ApiHive3ReplicationScheduledQueriesResultRow schedule(String schedule) {
    this.schedule = schedule;
    return this;
  }

  /**
   * 
   * @return schedule
  **/
  @ApiModelProperty(value = "")


  public String getSchedule() {
    return schedule;
  }

  public void setSchedule(String schedule) {
    this.schedule = schedule;
  }

  public ApiHive3ReplicationScheduledQueriesResultRow user(String user) {
    this.user = user;
    return this;
  }

  /**
   * 
   * @return user
  **/
  @ApiModelProperty(value = "")


  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public ApiHive3ReplicationScheduledQueriesResultRow query(String query) {
    this.query = query;
    return this;
  }

  /**
   * 
   * @return query
  **/
  @ApiModelProperty(value = "")


  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public ApiHive3ReplicationScheduledQueriesResultRow nextExecution(String nextExecution) {
    this.nextExecution = nextExecution;
    return this;
  }

  /**
   * 
   * @return nextExecution
  **/
  @ApiModelProperty(value = "")


  public String getNextExecution() {
    return nextExecution;
  }

  public void setNextExecution(String nextExecution) {
    this.nextExecution = nextExecution;
  }

  public ApiHive3ReplicationScheduledQueriesResultRow executionId(Integer executionId) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHive3ReplicationScheduledQueriesResultRow apiHive3ReplicationScheduledQueriesResultRow = (ApiHive3ReplicationScheduledQueriesResultRow) o;
    return Objects.equals(this.queryId, apiHive3ReplicationScheduledQueriesResultRow.queryId) &&
        Objects.equals(this.name, apiHive3ReplicationScheduledQueriesResultRow.name) &&
        Objects.equals(this.enabled, apiHive3ReplicationScheduledQueriesResultRow.enabled) &&
        Objects.equals(this.nameSpace, apiHive3ReplicationScheduledQueriesResultRow.nameSpace) &&
        Objects.equals(this.schedule, apiHive3ReplicationScheduledQueriesResultRow.schedule) &&
        Objects.equals(this.user, apiHive3ReplicationScheduledQueriesResultRow.user) &&
        Objects.equals(this.query, apiHive3ReplicationScheduledQueriesResultRow.query) &&
        Objects.equals(this.nextExecution, apiHive3ReplicationScheduledQueriesResultRow.nextExecution) &&
        Objects.equals(this.executionId, apiHive3ReplicationScheduledQueriesResultRow.executionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(queryId, name, enabled, nameSpace, schedule, user, query, nextExecution, executionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHive3ReplicationScheduledQueriesResultRow {\n");
    
    sb.append("    queryId: ").append(toIndentedString(queryId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    nameSpace: ").append(toIndentedString(nameSpace)).append("\n");
    sb.append("    schedule: ").append(toIndentedString(schedule)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
    sb.append("    nextExecution: ").append(toIndentedString(nextExecution)).append("\n");
    sb.append("    executionId: ").append(toIndentedString(executionId)).append("\n");
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

