package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ReplicationType;
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




public class ApiHive3ReplicationMetricsMetadata   {
  @JsonProperty("dbName")
  private String dbName = null;

  @JsonProperty("replicationType")
  private ReplicationType replicationType = null;

  @JsonProperty("stagingDir")
  private String stagingDir = null;

  @JsonProperty("lastReplId")
  private Integer lastReplId = null;

  public ApiHive3ReplicationMetricsMetadata dbName(String dbName) {
    this.dbName = dbName;
    return this;
  }

  /**
   * 
   * @return dbName
  **/
  @ApiModelProperty(value = "")


  public String getDbName() {
    return dbName;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  public ApiHive3ReplicationMetricsMetadata replicationType(ReplicationType replicationType) {
    this.replicationType = replicationType;
    return this;
  }

  /**
   * 
   * @return replicationType
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ReplicationType getReplicationType() {
    return replicationType;
  }

  public void setReplicationType(ReplicationType replicationType) {
    this.replicationType = replicationType;
  }

  public ApiHive3ReplicationMetricsMetadata stagingDir(String stagingDir) {
    this.stagingDir = stagingDir;
    return this;
  }

  /**
   * 
   * @return stagingDir
  **/
  @ApiModelProperty(value = "")


  public String getStagingDir() {
    return stagingDir;
  }

  public void setStagingDir(String stagingDir) {
    this.stagingDir = stagingDir;
  }

  public ApiHive3ReplicationMetricsMetadata lastReplId(Integer lastReplId) {
    this.lastReplId = lastReplId;
    return this;
  }

  /**
   * 
   * @return lastReplId
  **/
  @ApiModelProperty(value = "")


  public Integer getLastReplId() {
    return lastReplId;
  }

  public void setLastReplId(Integer lastReplId) {
    this.lastReplId = lastReplId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHive3ReplicationMetricsMetadata apiHive3ReplicationMetricsMetadata = (ApiHive3ReplicationMetricsMetadata) o;
    return Objects.equals(this.dbName, apiHive3ReplicationMetricsMetadata.dbName) &&
        Objects.equals(this.replicationType, apiHive3ReplicationMetricsMetadata.replicationType) &&
        Objects.equals(this.stagingDir, apiHive3ReplicationMetricsMetadata.stagingDir) &&
        Objects.equals(this.lastReplId, apiHive3ReplicationMetricsMetadata.lastReplId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dbName, replicationType, stagingDir, lastReplId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHive3ReplicationMetricsMetadata {\n");
    
    sb.append("    dbName: ").append(toIndentedString(dbName)).append("\n");
    sb.append("    replicationType: ").append(toIndentedString(replicationType)).append("\n");
    sb.append("    stagingDir: ").append(toIndentedString(stagingDir)).append("\n");
    sb.append("    lastReplId: ").append(toIndentedString(lastReplId)).append("\n");
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

