package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import com.sequenceiq.mock.swagger.model.HBasePeerState;
import com.sequenceiq.mock.swagger.model.HBaseTableArgs;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Replication arguments for HBase service
 */
@ApiModel(description = "Replication arguments for HBase service")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHBaseReplicationArguments   {
  @JsonProperty("sourceHBaseService")
  private ApiServiceRef sourceHBaseService = null;

  @JsonProperty("peerState")
  private HBasePeerState peerState = null;

  @JsonProperty("hbaseClusterKey")
  private String hbaseClusterKey = null;

  @JsonProperty("endPointClassName")
  private String endPointClassName = null;

  @JsonProperty("tables")
  @Valid
  private List<HBaseTableArgs> tables = null;

  @JsonProperty("replicationProperties")
  @Valid
  private Map<String, String> replicationProperties = null;

  @JsonProperty("sourceAccount")
  private String sourceAccount = null;

  @JsonProperty("sourceSchedulerPool")
  private String sourceSchedulerPool = null;

  @JsonProperty("numMappers")
  private Integer numMappers = null;

  public ApiHBaseReplicationArguments sourceHBaseService(ApiServiceRef sourceHBaseService) {
    this.sourceHBaseService = sourceHBaseService;
    return this;
  }

  /**
   * 
   * @return sourceHBaseService
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiServiceRef getSourceHBaseService() {
    return sourceHBaseService;
  }

  public void setSourceHBaseService(ApiServiceRef sourceHBaseService) {
    this.sourceHBaseService = sourceHBaseService;
  }

  public ApiHBaseReplicationArguments peerState(HBasePeerState peerState) {
    this.peerState = peerState;
    return this;
  }

  /**
   * 
   * @return peerState
  **/
  @ApiModelProperty(value = "")

  @Valid

  public HBasePeerState getPeerState() {
    return peerState;
  }

  public void setPeerState(HBasePeerState peerState) {
    this.peerState = peerState;
  }

  public ApiHBaseReplicationArguments hbaseClusterKey(String hbaseClusterKey) {
    this.hbaseClusterKey = hbaseClusterKey;
    return this;
  }

  /**
   * 
   * @return hbaseClusterKey
  **/
  @ApiModelProperty(value = "")


  public String getHbaseClusterKey() {
    return hbaseClusterKey;
  }

  public void setHbaseClusterKey(String hbaseClusterKey) {
    this.hbaseClusterKey = hbaseClusterKey;
  }

  public ApiHBaseReplicationArguments endPointClassName(String endPointClassName) {
    this.endPointClassName = endPointClassName;
    return this;
  }

  /**
   * 
   * @return endPointClassName
  **/
  @ApiModelProperty(value = "")


  public String getEndPointClassName() {
    return endPointClassName;
  }

  public void setEndPointClassName(String endPointClassName) {
    this.endPointClassName = endPointClassName;
  }

  public ApiHBaseReplicationArguments tables(List<HBaseTableArgs> tables) {
    this.tables = tables;
    return this;
  }

  public ApiHBaseReplicationArguments addTablesItem(HBaseTableArgs tablesItem) {
    if (this.tables == null) {
      this.tables = new ArrayList<>();
    }
    this.tables.add(tablesItem);
    return this;
  }

  /**
   * 
   * @return tables
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<HBaseTableArgs> getTables() {
    return tables;
  }

  public void setTables(List<HBaseTableArgs> tables) {
    this.tables = tables;
  }

  public ApiHBaseReplicationArguments replicationProperties(Map<String, String> replicationProperties) {
    this.replicationProperties = replicationProperties;
    return this;
  }

  public ApiHBaseReplicationArguments putReplicationPropertiesItem(String key, String replicationPropertiesItem) {
    if (this.replicationProperties == null) {
      this.replicationProperties = new HashMap<>();
    }
    this.replicationProperties.put(key, replicationPropertiesItem);
    return this;
  }

  /**
   * 
   * @return replicationProperties
  **/
  @ApiModelProperty(value = "")


  public Map<String, String> getReplicationProperties() {
    return replicationProperties;
  }

  public void setReplicationProperties(Map<String, String> replicationProperties) {
    this.replicationProperties = replicationProperties;
  }

  public ApiHBaseReplicationArguments sourceAccount(String sourceAccount) {
    this.sourceAccount = sourceAccount;
    return this;
  }

  /**
   * 
   * @return sourceAccount
  **/
  @ApiModelProperty(value = "")


  public String getSourceAccount() {
    return sourceAccount;
  }

  public void setSourceAccount(String sourceAccount) {
    this.sourceAccount = sourceAccount;
  }

  public ApiHBaseReplicationArguments sourceSchedulerPool(String sourceSchedulerPool) {
    this.sourceSchedulerPool = sourceSchedulerPool;
    return this;
  }

  /**
   * 
   * @return sourceSchedulerPool
  **/
  @ApiModelProperty(value = "")


  public String getSourceSchedulerPool() {
    return sourceSchedulerPool;
  }

  public void setSourceSchedulerPool(String sourceSchedulerPool) {
    this.sourceSchedulerPool = sourceSchedulerPool;
  }

  public ApiHBaseReplicationArguments numMappers(Integer numMappers) {
    this.numMappers = numMappers;
    return this;
  }

  /**
   * 
   * @return numMappers
  **/
  @ApiModelProperty(value = "")


  public Integer getNumMappers() {
    return numMappers;
  }

  public void setNumMappers(Integer numMappers) {
    this.numMappers = numMappers;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHBaseReplicationArguments apiHBaseReplicationArguments = (ApiHBaseReplicationArguments) o;
    return Objects.equals(this.sourceHBaseService, apiHBaseReplicationArguments.sourceHBaseService) &&
        Objects.equals(this.peerState, apiHBaseReplicationArguments.peerState) &&
        Objects.equals(this.hbaseClusterKey, apiHBaseReplicationArguments.hbaseClusterKey) &&
        Objects.equals(this.endPointClassName, apiHBaseReplicationArguments.endPointClassName) &&
        Objects.equals(this.tables, apiHBaseReplicationArguments.tables) &&
        Objects.equals(this.replicationProperties, apiHBaseReplicationArguments.replicationProperties) &&
        Objects.equals(this.sourceAccount, apiHBaseReplicationArguments.sourceAccount) &&
        Objects.equals(this.sourceSchedulerPool, apiHBaseReplicationArguments.sourceSchedulerPool) &&
        Objects.equals(this.numMappers, apiHBaseReplicationArguments.numMappers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceHBaseService, peerState, hbaseClusterKey, endPointClassName, tables, replicationProperties, sourceAccount, sourceSchedulerPool, numMappers);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseReplicationArguments {\n");
    
    sb.append("    sourceHBaseService: ").append(toIndentedString(sourceHBaseService)).append("\n");
    sb.append("    peerState: ").append(toIndentedString(peerState)).append("\n");
    sb.append("    hbaseClusterKey: ").append(toIndentedString(hbaseClusterKey)).append("\n");
    sb.append("    endPointClassName: ").append(toIndentedString(endPointClassName)).append("\n");
    sb.append("    tables: ").append(toIndentedString(tables)).append("\n");
    sb.append("    replicationProperties: ").append(toIndentedString(replicationProperties)).append("\n");
    sb.append("    sourceAccount: ").append(toIndentedString(sourceAccount)).append("\n");
    sb.append("    sourceSchedulerPool: ").append(toIndentedString(sourceSchedulerPool)).append("\n");
    sb.append("    numMappers: ").append(toIndentedString(numMappers)).append("\n");
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

