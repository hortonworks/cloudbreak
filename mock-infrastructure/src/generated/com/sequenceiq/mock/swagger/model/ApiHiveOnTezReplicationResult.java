package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.Origin;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Detailed information about a Hive replication job.
 */
@ApiModel(description = "Detailed information about a Hive replication job.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHiveOnTezReplicationResult   {
  @JsonProperty("type")
  private String type = null;

  @JsonProperty("status")
  private String status = null;

  @JsonProperty("error")
  private String error = null;

  @JsonProperty("tablesCurrent")
  private Integer tablesCurrent = null;

  @JsonProperty("tablesTotal")
  private Integer tablesTotal = null;

  @JsonProperty("functionsCurrent")
  private Integer functionsCurrent = null;

  @JsonProperty("functionsTotal")
  private Integer functionsTotal = null;

  @JsonProperty("eventsCurrent")
  private Integer eventsCurrent = null;

  @JsonProperty("eventsTotal")
  private Integer eventsTotal = null;

  @JsonProperty("policiesCurrent")
  private Integer policiesCurrent = null;

  @JsonProperty("policiesTotal")
  private Integer policiesTotal = null;

  @JsonProperty("entitiesCurrent")
  private Integer entitiesCurrent = null;

  @JsonProperty("entitiesTotal")
  private Integer entitiesTotal = null;

  @JsonProperty("origin")
  private Origin origin = null;

  public ApiHiveOnTezReplicationResult type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Type of replication. <p/> BOOTSTRAP or INCREMENTAL
   * @return type
  **/
  @ApiModelProperty(value = "Type of replication. <p/> BOOTSTRAP or INCREMENTAL")


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ApiHiveOnTezReplicationResult status(String status) {
    this.status = status;
    return this;
  }

  /**
   * 
   * @return status
  **/
  @ApiModelProperty(value = "")


  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public ApiHiveOnTezReplicationResult error(String error) {
    this.error = error;
    return this;
  }

  /**
   * 
   * @return error
  **/
  @ApiModelProperty(value = "")


  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public ApiHiveOnTezReplicationResult tablesCurrent(Integer tablesCurrent) {
    this.tablesCurrent = tablesCurrent;
    return this;
  }

  /**
   * 
   * @return tablesCurrent
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getTablesCurrent() {
    return tablesCurrent;
  }

  public void setTablesCurrent(Integer tablesCurrent) {
    this.tablesCurrent = tablesCurrent;
  }

  public ApiHiveOnTezReplicationResult tablesTotal(Integer tablesTotal) {
    this.tablesTotal = tablesTotal;
    return this;
  }

  /**
   * 
   * @return tablesTotal
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getTablesTotal() {
    return tablesTotal;
  }

  public void setTablesTotal(Integer tablesTotal) {
    this.tablesTotal = tablesTotal;
  }

  public ApiHiveOnTezReplicationResult functionsCurrent(Integer functionsCurrent) {
    this.functionsCurrent = functionsCurrent;
    return this;
  }

  /**
   * 
   * @return functionsCurrent
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getFunctionsCurrent() {
    return functionsCurrent;
  }

  public void setFunctionsCurrent(Integer functionsCurrent) {
    this.functionsCurrent = functionsCurrent;
  }

  public ApiHiveOnTezReplicationResult functionsTotal(Integer functionsTotal) {
    this.functionsTotal = functionsTotal;
    return this;
  }

  /**
   * 
   * @return functionsTotal
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getFunctionsTotal() {
    return functionsTotal;
  }

  public void setFunctionsTotal(Integer functionsTotal) {
    this.functionsTotal = functionsTotal;
  }

  public ApiHiveOnTezReplicationResult eventsCurrent(Integer eventsCurrent) {
    this.eventsCurrent = eventsCurrent;
    return this;
  }

  /**
   * 
   * @return eventsCurrent
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getEventsCurrent() {
    return eventsCurrent;
  }

  public void setEventsCurrent(Integer eventsCurrent) {
    this.eventsCurrent = eventsCurrent;
  }

  public ApiHiveOnTezReplicationResult eventsTotal(Integer eventsTotal) {
    this.eventsTotal = eventsTotal;
    return this;
  }

  /**
   * 
   * @return eventsTotal
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getEventsTotal() {
    return eventsTotal;
  }

  public void setEventsTotal(Integer eventsTotal) {
    this.eventsTotal = eventsTotal;
  }

  public ApiHiveOnTezReplicationResult policiesCurrent(Integer policiesCurrent) {
    this.policiesCurrent = policiesCurrent;
    return this;
  }

  /**
   * 
   * @return policiesCurrent
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getPoliciesCurrent() {
    return policiesCurrent;
  }

  public void setPoliciesCurrent(Integer policiesCurrent) {
    this.policiesCurrent = policiesCurrent;
  }

  public ApiHiveOnTezReplicationResult policiesTotal(Integer policiesTotal) {
    this.policiesTotal = policiesTotal;
    return this;
  }

  /**
   * 
   * @return policiesTotal
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getPoliciesTotal() {
    return policiesTotal;
  }

  public void setPoliciesTotal(Integer policiesTotal) {
    this.policiesTotal = policiesTotal;
  }

  public ApiHiveOnTezReplicationResult entitiesCurrent(Integer entitiesCurrent) {
    this.entitiesCurrent = entitiesCurrent;
    return this;
  }

  /**
   * 
   * @return entitiesCurrent
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getEntitiesCurrent() {
    return entitiesCurrent;
  }

  public void setEntitiesCurrent(Integer entitiesCurrent) {
    this.entitiesCurrent = entitiesCurrent;
  }

  public ApiHiveOnTezReplicationResult entitiesTotal(Integer entitiesTotal) {
    this.entitiesTotal = entitiesTotal;
    return this;
  }

  /**
   * 
   * @return entitiesTotal
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getEntitiesTotal() {
    return entitiesTotal;
  }

  public void setEntitiesTotal(Integer entitiesTotal) {
    this.entitiesTotal = entitiesTotal;
  }

  public ApiHiveOnTezReplicationResult origin(Origin origin) {
    this.origin = origin;
    return this;
  }

  /**
   * 
   * @return origin
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Origin getOrigin() {
    return origin;
  }

  public void setOrigin(Origin origin) {
    this.origin = origin;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHiveOnTezReplicationResult apiHiveOnTezReplicationResult = (ApiHiveOnTezReplicationResult) o;
    return Objects.equals(this.type, apiHiveOnTezReplicationResult.type) &&
        Objects.equals(this.status, apiHiveOnTezReplicationResult.status) &&
        Objects.equals(this.error, apiHiveOnTezReplicationResult.error) &&
        Objects.equals(this.tablesCurrent, apiHiveOnTezReplicationResult.tablesCurrent) &&
        Objects.equals(this.tablesTotal, apiHiveOnTezReplicationResult.tablesTotal) &&
        Objects.equals(this.functionsCurrent, apiHiveOnTezReplicationResult.functionsCurrent) &&
        Objects.equals(this.functionsTotal, apiHiveOnTezReplicationResult.functionsTotal) &&
        Objects.equals(this.eventsCurrent, apiHiveOnTezReplicationResult.eventsCurrent) &&
        Objects.equals(this.eventsTotal, apiHiveOnTezReplicationResult.eventsTotal) &&
        Objects.equals(this.policiesCurrent, apiHiveOnTezReplicationResult.policiesCurrent) &&
        Objects.equals(this.policiesTotal, apiHiveOnTezReplicationResult.policiesTotal) &&
        Objects.equals(this.entitiesCurrent, apiHiveOnTezReplicationResult.entitiesCurrent) &&
        Objects.equals(this.entitiesTotal, apiHiveOnTezReplicationResult.entitiesTotal) &&
        Objects.equals(this.origin, apiHiveOnTezReplicationResult.origin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, status, error, tablesCurrent, tablesTotal, functionsCurrent, functionsTotal, eventsCurrent, eventsTotal, policiesCurrent, policiesTotal, entitiesCurrent, entitiesTotal, origin);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHiveOnTezReplicationResult {\n");
    
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    tablesCurrent: ").append(toIndentedString(tablesCurrent)).append("\n");
    sb.append("    tablesTotal: ").append(toIndentedString(tablesTotal)).append("\n");
    sb.append("    functionsCurrent: ").append(toIndentedString(functionsCurrent)).append("\n");
    sb.append("    functionsTotal: ").append(toIndentedString(functionsTotal)).append("\n");
    sb.append("    eventsCurrent: ").append(toIndentedString(eventsCurrent)).append("\n");
    sb.append("    eventsTotal: ").append(toIndentedString(eventsTotal)).append("\n");
    sb.append("    policiesCurrent: ").append(toIndentedString(policiesCurrent)).append("\n");
    sb.append("    policiesTotal: ").append(toIndentedString(policiesTotal)).append("\n");
    sb.append("    entitiesCurrent: ").append(toIndentedString(entitiesCurrent)).append("\n");
    sb.append("    entitiesTotal: ").append(toIndentedString(entitiesTotal)).append("\n");
    sb.append("    origin: ").append(toIndentedString(origin)).append("\n");
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

