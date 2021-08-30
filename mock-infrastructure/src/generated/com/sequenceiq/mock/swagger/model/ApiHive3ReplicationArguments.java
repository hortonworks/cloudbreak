package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import com.sequenceiq.mock.swagger.model.PolicyStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Replication Arguments for Hive3 schedules / policies
 */
@ApiModel(description = "Replication Arguments for Hive3 schedules / policies")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHive3ReplicationArguments   {
  @JsonProperty("sourceHiveService")
  private ApiServiceRef sourceHiveService = null;

  @JsonProperty("status")
  private PolicyStatus status = null;

  @JsonProperty("rangerReplication")
  private Boolean rangerReplication = null;

  @JsonProperty("atlasReplication")
  private Boolean atlasReplication = null;

  @JsonProperty("externalTableReplication")
  private Boolean externalTableReplication = null;

  @JsonProperty("externalTableBaseDir")
  private String externalTableBaseDir = null;

  @JsonProperty("distcpOnTarget")
  private Boolean distcpOnTarget = null;

  @JsonProperty("numMaps")
  private Integer numMaps = null;

  @JsonProperty("bandwidthPerMap")
  private Integer bandwidthPerMap = null;

  @JsonProperty("policyOptions")
  @Valid
  private Map<String, String> policyOptions = null;

  @JsonProperty("sourceDbName")
  private String sourceDbName = null;

  @JsonProperty("targetDbName")
  private String targetDbName = null;

  @JsonProperty("policyName")
  private String policyName = null;

  @JsonProperty("scheduleClause")
  private String scheduleClause = null;

  @JsonProperty("runAs")
  private String runAs = null;

  @JsonProperty("hiveOp")
  private String hiveOp = null;

  @JsonProperty("hiveUpdateOp")
  private String hiveUpdateOp = null;

  @JsonProperty("excludeSource")
  private Boolean excludeSource = null;

  @JsonProperty("excludeTarget")
  private Boolean excludeTarget = null;

  @JsonProperty("failoverStatus")
  private String failoverStatus = null;

  public ApiHive3ReplicationArguments sourceHiveService(ApiServiceRef sourceHiveService) {
    this.sourceHiveService = sourceHiveService;
    return this;
  }

  /**
   * 
   * @return sourceHiveService
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiServiceRef getSourceHiveService() {
    return sourceHiveService;
  }

  public void setSourceHiveService(ApiServiceRef sourceHiveService) {
    this.sourceHiveService = sourceHiveService;
  }

  public ApiHive3ReplicationArguments status(PolicyStatus status) {
    this.status = status;
    return this;
  }

  /**
   * 
   * @return status
  **/
  @ApiModelProperty(value = "")

  @Valid

  public PolicyStatus getStatus() {
    return status;
  }

  public void setStatus(PolicyStatus status) {
    this.status = status;
  }

  public ApiHive3ReplicationArguments rangerReplication(Boolean rangerReplication) {
    this.rangerReplication = rangerReplication;
    return this;
  }

  /**
   * 
   * @return rangerReplication
  **/
  @ApiModelProperty(value = "")


  public Boolean isRangerReplication() {
    return rangerReplication;
  }

  public void setRangerReplication(Boolean rangerReplication) {
    this.rangerReplication = rangerReplication;
  }

  public ApiHive3ReplicationArguments atlasReplication(Boolean atlasReplication) {
    this.atlasReplication = atlasReplication;
    return this;
  }

  /**
   * 
   * @return atlasReplication
  **/
  @ApiModelProperty(value = "")


  public Boolean isAtlasReplication() {
    return atlasReplication;
  }

  public void setAtlasReplication(Boolean atlasReplication) {
    this.atlasReplication = atlasReplication;
  }

  public ApiHive3ReplicationArguments externalTableReplication(Boolean externalTableReplication) {
    this.externalTableReplication = externalTableReplication;
    return this;
  }

  /**
   * 
   * @return externalTableReplication
  **/
  @ApiModelProperty(value = "")


  public Boolean isExternalTableReplication() {
    return externalTableReplication;
  }

  public void setExternalTableReplication(Boolean externalTableReplication) {
    this.externalTableReplication = externalTableReplication;
  }

  public ApiHive3ReplicationArguments externalTableBaseDir(String externalTableBaseDir) {
    this.externalTableBaseDir = externalTableBaseDir;
    return this;
  }

  /**
   * 
   * @return externalTableBaseDir
  **/
  @ApiModelProperty(value = "")


  public String getExternalTableBaseDir() {
    return externalTableBaseDir;
  }

  public void setExternalTableBaseDir(String externalTableBaseDir) {
    this.externalTableBaseDir = externalTableBaseDir;
  }

  public ApiHive3ReplicationArguments distcpOnTarget(Boolean distcpOnTarget) {
    this.distcpOnTarget = distcpOnTarget;
    return this;
  }

  /**
   * 
   * @return distcpOnTarget
  **/
  @ApiModelProperty(value = "")


  public Boolean isDistcpOnTarget() {
    return distcpOnTarget;
  }

  public void setDistcpOnTarget(Boolean distcpOnTarget) {
    this.distcpOnTarget = distcpOnTarget;
  }

  public ApiHive3ReplicationArguments numMaps(Integer numMaps) {
    this.numMaps = numMaps;
    return this;
  }

  /**
   * 
   * @return numMaps
  **/
  @ApiModelProperty(value = "")


  public Integer getNumMaps() {
    return numMaps;
  }

  public void setNumMaps(Integer numMaps) {
    this.numMaps = numMaps;
  }

  public ApiHive3ReplicationArguments bandwidthPerMap(Integer bandwidthPerMap) {
    this.bandwidthPerMap = bandwidthPerMap;
    return this;
  }

  /**
   * 
   * @return bandwidthPerMap
  **/
  @ApiModelProperty(value = "")


  public Integer getBandwidthPerMap() {
    return bandwidthPerMap;
  }

  public void setBandwidthPerMap(Integer bandwidthPerMap) {
    this.bandwidthPerMap = bandwidthPerMap;
  }

  public ApiHive3ReplicationArguments policyOptions(Map<String, String> policyOptions) {
    this.policyOptions = policyOptions;
    return this;
  }

  public ApiHive3ReplicationArguments putPolicyOptionsItem(String key, String policyOptionsItem) {
    if (this.policyOptions == null) {
      this.policyOptions = new HashMap<>();
    }
    this.policyOptions.put(key, policyOptionsItem);
    return this;
  }

  /**
   * 
   * @return policyOptions
  **/
  @ApiModelProperty(value = "")


  public Map<String, String> getPolicyOptions() {
    return policyOptions;
  }

  public void setPolicyOptions(Map<String, String> policyOptions) {
    this.policyOptions = policyOptions;
  }

  public ApiHive3ReplicationArguments sourceDbName(String sourceDbName) {
    this.sourceDbName = sourceDbName;
    return this;
  }

  /**
   * 
   * @return sourceDbName
  **/
  @ApiModelProperty(value = "")


  public String getSourceDbName() {
    return sourceDbName;
  }

  public void setSourceDbName(String sourceDbName) {
    this.sourceDbName = sourceDbName;
  }

  public ApiHive3ReplicationArguments targetDbName(String targetDbName) {
    this.targetDbName = targetDbName;
    return this;
  }

  /**
   * 
   * @return targetDbName
  **/
  @ApiModelProperty(value = "")


  public String getTargetDbName() {
    return targetDbName;
  }

  public void setTargetDbName(String targetDbName) {
    this.targetDbName = targetDbName;
  }

  public ApiHive3ReplicationArguments policyName(String policyName) {
    this.policyName = policyName;
    return this;
  }

  /**
   * 
   * @return policyName
  **/
  @ApiModelProperty(value = "")


  public String getPolicyName() {
    return policyName;
  }

  public void setPolicyName(String policyName) {
    this.policyName = policyName;
  }

  public ApiHive3ReplicationArguments scheduleClause(String scheduleClause) {
    this.scheduleClause = scheduleClause;
    return this;
  }

  /**
   * 
   * @return scheduleClause
  **/
  @ApiModelProperty(value = "")


  public String getScheduleClause() {
    return scheduleClause;
  }

  public void setScheduleClause(String scheduleClause) {
    this.scheduleClause = scheduleClause;
  }

  public ApiHive3ReplicationArguments runAs(String runAs) {
    this.runAs = runAs;
    return this;
  }

  /**
   * 
   * @return runAs
  **/
  @ApiModelProperty(value = "")


  public String getRunAs() {
    return runAs;
  }

  public void setRunAs(String runAs) {
    this.runAs = runAs;
  }

  public ApiHive3ReplicationArguments hiveOp(String hiveOp) {
    this.hiveOp = hiveOp;
    return this;
  }

  /**
   * 
   * @return hiveOp
  **/
  @ApiModelProperty(value = "")


  public String getHiveOp() {
    return hiveOp;
  }

  public void setHiveOp(String hiveOp) {
    this.hiveOp = hiveOp;
  }

  public ApiHive3ReplicationArguments hiveUpdateOp(String hiveUpdateOp) {
    this.hiveUpdateOp = hiveUpdateOp;
    return this;
  }

  /**
   * 
   * @return hiveUpdateOp
  **/
  @ApiModelProperty(value = "")


  public String getHiveUpdateOp() {
    return hiveUpdateOp;
  }

  public void setHiveUpdateOp(String hiveUpdateOp) {
    this.hiveUpdateOp = hiveUpdateOp;
  }

  public ApiHive3ReplicationArguments excludeSource(Boolean excludeSource) {
    this.excludeSource = excludeSource;
    return this;
  }

  /**
   * 
   * @return excludeSource
  **/
  @ApiModelProperty(value = "")


  public Boolean isExcludeSource() {
    return excludeSource;
  }

  public void setExcludeSource(Boolean excludeSource) {
    this.excludeSource = excludeSource;
  }

  public ApiHive3ReplicationArguments excludeTarget(Boolean excludeTarget) {
    this.excludeTarget = excludeTarget;
    return this;
  }

  /**
   * 
   * @return excludeTarget
  **/
  @ApiModelProperty(value = "")


  public Boolean isExcludeTarget() {
    return excludeTarget;
  }

  public void setExcludeTarget(Boolean excludeTarget) {
    this.excludeTarget = excludeTarget;
  }

  public ApiHive3ReplicationArguments failoverStatus(String failoverStatus) {
    this.failoverStatus = failoverStatus;
    return this;
  }

  /**
   * 
   * @return failoverStatus
  **/
  @ApiModelProperty(value = "")


  public String getFailoverStatus() {
    return failoverStatus;
  }

  public void setFailoverStatus(String failoverStatus) {
    this.failoverStatus = failoverStatus;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHive3ReplicationArguments apiHive3ReplicationArguments = (ApiHive3ReplicationArguments) o;
    return Objects.equals(this.sourceHiveService, apiHive3ReplicationArguments.sourceHiveService) &&
        Objects.equals(this.status, apiHive3ReplicationArguments.status) &&
        Objects.equals(this.rangerReplication, apiHive3ReplicationArguments.rangerReplication) &&
        Objects.equals(this.atlasReplication, apiHive3ReplicationArguments.atlasReplication) &&
        Objects.equals(this.externalTableReplication, apiHive3ReplicationArguments.externalTableReplication) &&
        Objects.equals(this.externalTableBaseDir, apiHive3ReplicationArguments.externalTableBaseDir) &&
        Objects.equals(this.distcpOnTarget, apiHive3ReplicationArguments.distcpOnTarget) &&
        Objects.equals(this.numMaps, apiHive3ReplicationArguments.numMaps) &&
        Objects.equals(this.bandwidthPerMap, apiHive3ReplicationArguments.bandwidthPerMap) &&
        Objects.equals(this.policyOptions, apiHive3ReplicationArguments.policyOptions) &&
        Objects.equals(this.sourceDbName, apiHive3ReplicationArguments.sourceDbName) &&
        Objects.equals(this.targetDbName, apiHive3ReplicationArguments.targetDbName) &&
        Objects.equals(this.policyName, apiHive3ReplicationArguments.policyName) &&
        Objects.equals(this.scheduleClause, apiHive3ReplicationArguments.scheduleClause) &&
        Objects.equals(this.runAs, apiHive3ReplicationArguments.runAs) &&
        Objects.equals(this.hiveOp, apiHive3ReplicationArguments.hiveOp) &&
        Objects.equals(this.hiveUpdateOp, apiHive3ReplicationArguments.hiveUpdateOp) &&
        Objects.equals(this.excludeSource, apiHive3ReplicationArguments.excludeSource) &&
        Objects.equals(this.excludeTarget, apiHive3ReplicationArguments.excludeTarget) &&
        Objects.equals(this.failoverStatus, apiHive3ReplicationArguments.failoverStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceHiveService, status, rangerReplication, atlasReplication, externalTableReplication, externalTableBaseDir, distcpOnTarget, numMaps, bandwidthPerMap, policyOptions, sourceDbName, targetDbName, policyName, scheduleClause, runAs, hiveOp, hiveUpdateOp, excludeSource, excludeTarget, failoverStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHive3ReplicationArguments {\n");
    
    sb.append("    sourceHiveService: ").append(toIndentedString(sourceHiveService)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    rangerReplication: ").append(toIndentedString(rangerReplication)).append("\n");
    sb.append("    atlasReplication: ").append(toIndentedString(atlasReplication)).append("\n");
    sb.append("    externalTableReplication: ").append(toIndentedString(externalTableReplication)).append("\n");
    sb.append("    externalTableBaseDir: ").append(toIndentedString(externalTableBaseDir)).append("\n");
    sb.append("    distcpOnTarget: ").append(toIndentedString(distcpOnTarget)).append("\n");
    sb.append("    numMaps: ").append(toIndentedString(numMaps)).append("\n");
    sb.append("    bandwidthPerMap: ").append(toIndentedString(bandwidthPerMap)).append("\n");
    sb.append("    policyOptions: ").append(toIndentedString(policyOptions)).append("\n");
    sb.append("    sourceDbName: ").append(toIndentedString(sourceDbName)).append("\n");
    sb.append("    targetDbName: ").append(toIndentedString(targetDbName)).append("\n");
    sb.append("    policyName: ").append(toIndentedString(policyName)).append("\n");
    sb.append("    scheduleClause: ").append(toIndentedString(scheduleClause)).append("\n");
    sb.append("    runAs: ").append(toIndentedString(runAs)).append("\n");
    sb.append("    hiveOp: ").append(toIndentedString(hiveOp)).append("\n");
    sb.append("    hiveUpdateOp: ").append(toIndentedString(hiveUpdateOp)).append("\n");
    sb.append("    excludeSource: ").append(toIndentedString(excludeSource)).append("\n");
    sb.append("    excludeTarget: ").append(toIndentedString(excludeTarget)).append("\n");
    sb.append("    failoverStatus: ").append(toIndentedString(failoverStatus)).append("\n");
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

