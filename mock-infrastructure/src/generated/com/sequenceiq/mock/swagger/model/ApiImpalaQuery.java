package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Represents an Impala Query.
 */
@ApiModel(description = "Represents an Impala Query.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiImpalaQuery   {
  @JsonProperty("queryId")
  private String queryId = null;

  @JsonProperty("statement")
  private String statement = null;

  @JsonProperty("queryType")
  private String queryType = null;

  @JsonProperty("queryState")
  private String queryState = null;

  @JsonProperty("startTime")
  private String startTime = null;

  @JsonProperty("endTime")
  private String endTime = null;

  @JsonProperty("rowsProduced")
  private Integer rowsProduced = null;

  @JsonProperty("attributes")
  @Valid
  private Map<String, String> attributes = null;

  @JsonProperty("user")
  private String user = null;

  @JsonProperty("coordinator")
  private ApiHostRef coordinator = null;

  @JsonProperty("detailsAvailable")
  private Boolean detailsAvailable = null;

  @JsonProperty("database")
  private String database = null;

  @JsonProperty("durationMillis")
  private Integer durationMillis = null;

  public ApiImpalaQuery queryId(String queryId) {
    this.queryId = queryId;
    return this;
  }

  /**
   * The query id.
   * @return queryId
  **/
  @ApiModelProperty(value = "The query id.")


  public String getQueryId() {
    return queryId;
  }

  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }

  public ApiImpalaQuery statement(String statement) {
    this.statement = statement;
    return this;
  }

  /**
   * The SQL statement for the query.
   * @return statement
  **/
  @ApiModelProperty(value = "The SQL statement for the query.")


  public String getStatement() {
    return statement;
  }

  public void setStatement(String statement) {
    this.statement = statement;
  }

  public ApiImpalaQuery queryType(String queryType) {
    this.queryType = queryType;
    return this;
  }

  /**
   * The query type. The possible values are: DML, DDL, QUERY and UNKNOWN. See the Impala documentation for more details.
   * @return queryType
  **/
  @ApiModelProperty(value = "The query type. The possible values are: DML, DDL, QUERY and UNKNOWN. See the Impala documentation for more details.")


  public String getQueryType() {
    return queryType;
  }

  public void setQueryType(String queryType) {
    this.queryType = queryType;
  }

  public ApiImpalaQuery queryState(String queryState) {
    this.queryState = queryState;
    return this;
  }

  /**
   * The query state. The possible values are: CREATED, INITIALIZED, COMPILED, RUNNING, FINISHED, EXCEPTION, and UNKNOWN. See the Impala documentation for more details.
   * @return queryState
  **/
  @ApiModelProperty(value = "The query state. The possible values are: CREATED, INITIALIZED, COMPILED, RUNNING, FINISHED, EXCEPTION, and UNKNOWN. See the Impala documentation for more details.")


  public String getQueryState() {
    return queryState;
  }

  public void setQueryState(String queryState) {
    this.queryState = queryState;
  }

  public ApiImpalaQuery startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * The time the query was issued.
   * @return startTime
  **/
  @ApiModelProperty(value = "The time the query was issued.")


  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public ApiImpalaQuery endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * The time the query finished. If the query hasn't finished then this will return null.
   * @return endTime
  **/
  @ApiModelProperty(value = "The time the query finished. If the query hasn't finished then this will return null.")


  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public ApiImpalaQuery rowsProduced(Integer rowsProduced) {
    this.rowsProduced = rowsProduced;
    return this;
  }

  /**
   * The number of rows produced by the query. If the query hasn't completed this will return null.
   * @return rowsProduced
  **/
  @ApiModelProperty(value = "The number of rows produced by the query. If the query hasn't completed this will return null.")


  public Integer getRowsProduced() {
    return rowsProduced;
  }

  public void setRowsProduced(Integer rowsProduced) {
    this.rowsProduced = rowsProduced;
  }

  public ApiImpalaQuery attributes(Map<String, String> attributes) {
    this.attributes = attributes;
    return this;
  }

  public ApiImpalaQuery putAttributesItem(String key, String attributesItem) {
    if (this.attributes == null) {
      this.attributes = new HashMap<>();
    }
    this.attributes.put(key, attributesItem);
    return this;
  }

  /**
   * A map of additional query attributes which is generated by Cloudera Manager.
   * @return attributes
  **/
  @ApiModelProperty(value = "A map of additional query attributes which is generated by Cloudera Manager.")


  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public ApiImpalaQuery user(String user) {
    this.user = user;
    return this;
  }

  /**
   * The user who issued this query.
   * @return user
  **/
  @ApiModelProperty(value = "The user who issued this query.")


  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public ApiImpalaQuery coordinator(ApiHostRef coordinator) {
    this.coordinator = coordinator;
    return this;
  }

  /**
   * The host of the Impala Daemon coordinating the query
   * @return coordinator
  **/
  @ApiModelProperty(value = "The host of the Impala Daemon coordinating the query")

  @Valid

  public ApiHostRef getCoordinator() {
    return coordinator;
  }

  public void setCoordinator(ApiHostRef coordinator) {
    this.coordinator = coordinator;
  }

  public ApiImpalaQuery detailsAvailable(Boolean detailsAvailable) {
    this.detailsAvailable = detailsAvailable;
    return this;
  }

  /**
   * Whether we have a detailed runtime profile available for the query. This profile is available at the endpoint /queries/{QUERY_ID}.
   * @return detailsAvailable
  **/
  @ApiModelProperty(value = "Whether we have a detailed runtime profile available for the query. This profile is available at the endpoint /queries/{QUERY_ID}.")


  public Boolean isDetailsAvailable() {
    return detailsAvailable;
  }

  public void setDetailsAvailable(Boolean detailsAvailable) {
    this.detailsAvailable = detailsAvailable;
  }

  public ApiImpalaQuery database(String database) {
    this.database = database;
    return this;
  }

  /**
   * The database on which this query was issued.
   * @return database
  **/
  @ApiModelProperty(value = "The database on which this query was issued.")


  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public ApiImpalaQuery durationMillis(Integer durationMillis) {
    this.durationMillis = durationMillis;
    return this;
  }

  /**
   * The duration of the query in milliseconds. If the query hasn't completed then this will return null.
   * @return durationMillis
  **/
  @ApiModelProperty(value = "The duration of the query in milliseconds. If the query hasn't completed then this will return null.")


  public Integer getDurationMillis() {
    return durationMillis;
  }

  public void setDurationMillis(Integer durationMillis) {
    this.durationMillis = durationMillis;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiImpalaQuery apiImpalaQuery = (ApiImpalaQuery) o;
    return Objects.equals(this.queryId, apiImpalaQuery.queryId) &&
        Objects.equals(this.statement, apiImpalaQuery.statement) &&
        Objects.equals(this.queryType, apiImpalaQuery.queryType) &&
        Objects.equals(this.queryState, apiImpalaQuery.queryState) &&
        Objects.equals(this.startTime, apiImpalaQuery.startTime) &&
        Objects.equals(this.endTime, apiImpalaQuery.endTime) &&
        Objects.equals(this.rowsProduced, apiImpalaQuery.rowsProduced) &&
        Objects.equals(this.attributes, apiImpalaQuery.attributes) &&
        Objects.equals(this.user, apiImpalaQuery.user) &&
        Objects.equals(this.coordinator, apiImpalaQuery.coordinator) &&
        Objects.equals(this.detailsAvailable, apiImpalaQuery.detailsAvailable) &&
        Objects.equals(this.database, apiImpalaQuery.database) &&
        Objects.equals(this.durationMillis, apiImpalaQuery.durationMillis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(queryId, statement, queryType, queryState, startTime, endTime, rowsProduced, attributes, user, coordinator, detailsAvailable, database, durationMillis);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaQuery {\n");
    
    sb.append("    queryId: ").append(toIndentedString(queryId)).append("\n");
    sb.append("    statement: ").append(toIndentedString(statement)).append("\n");
    sb.append("    queryType: ").append(toIndentedString(queryType)).append("\n");
    sb.append("    queryState: ").append(toIndentedString(queryState)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    rowsProduced: ").append(toIndentedString(rowsProduced)).append("\n");
    sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    coordinator: ").append(toIndentedString(coordinator)).append("\n");
    sb.append("    detailsAvailable: ").append(toIndentedString(detailsAvailable)).append("\n");
    sb.append("    database: ").append(toIndentedString(database)).append("\n");
    sb.append("    durationMillis: ").append(toIndentedString(durationMillis)).append("\n");
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

