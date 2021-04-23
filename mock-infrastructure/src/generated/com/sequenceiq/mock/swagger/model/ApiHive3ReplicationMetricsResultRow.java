package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationMetricsMetadata;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationMetricsProgress;
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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiHive3ReplicationMetricsResultRow   {
  @JsonProperty("scheduledExecutionId")
  private Integer scheduledExecutionId = null;

  @JsonProperty("policy")
  private String policy = null;

  @JsonProperty("dumpExecutionId")
  private Integer dumpExecutionId = null;

  @JsonProperty("metadata")
  private ApiHive3ReplicationMetricsMetadata metadata = null;

  @JsonProperty("progress")
  private ApiHive3ReplicationMetricsProgress progress = null;

  @JsonProperty("startDate")
  private String startDate = null;

  @JsonProperty("endDate")
  private String endDate = null;

  @JsonProperty("errorMessage")
  private String errorMessage = null;

  public ApiHive3ReplicationMetricsResultRow scheduledExecutionId(Integer scheduledExecutionId) {
    this.scheduledExecutionId = scheduledExecutionId;
    return this;
  }

  /**
   * 
   * @return scheduledExecutionId
  **/
  @ApiModelProperty(value = "")


  public Integer getScheduledExecutionId() {
    return scheduledExecutionId;
  }

  public void setScheduledExecutionId(Integer scheduledExecutionId) {
    this.scheduledExecutionId = scheduledExecutionId;
  }

  public ApiHive3ReplicationMetricsResultRow policy(String policy) {
    this.policy = policy;
    return this;
  }

  /**
   * 
   * @return policy
  **/
  @ApiModelProperty(value = "")


  public String getPolicy() {
    return policy;
  }

  public void setPolicy(String policy) {
    this.policy = policy;
  }

  public ApiHive3ReplicationMetricsResultRow dumpExecutionId(Integer dumpExecutionId) {
    this.dumpExecutionId = dumpExecutionId;
    return this;
  }

  /**
   * 
   * @return dumpExecutionId
  **/
  @ApiModelProperty(value = "")


  public Integer getDumpExecutionId() {
    return dumpExecutionId;
  }

  public void setDumpExecutionId(Integer dumpExecutionId) {
    this.dumpExecutionId = dumpExecutionId;
  }

  public ApiHive3ReplicationMetricsResultRow metadata(ApiHive3ReplicationMetricsMetadata metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * 
   * @return metadata
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHive3ReplicationMetricsMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ApiHive3ReplicationMetricsMetadata metadata) {
    this.metadata = metadata;
  }

  public ApiHive3ReplicationMetricsResultRow progress(ApiHive3ReplicationMetricsProgress progress) {
    this.progress = progress;
    return this;
  }

  /**
   * 
   * @return progress
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHive3ReplicationMetricsProgress getProgress() {
    return progress;
  }

  public void setProgress(ApiHive3ReplicationMetricsProgress progress) {
    this.progress = progress;
  }

  public ApiHive3ReplicationMetricsResultRow startDate(String startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * 
   * @return startDate
  **/
  @ApiModelProperty(value = "")


  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public ApiHive3ReplicationMetricsResultRow endDate(String endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * 
   * @return endDate
  **/
  @ApiModelProperty(value = "")


  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  public ApiHive3ReplicationMetricsResultRow errorMessage(String errorMessage) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHive3ReplicationMetricsResultRow apiHive3ReplicationMetricsResultRow = (ApiHive3ReplicationMetricsResultRow) o;
    return Objects.equals(this.scheduledExecutionId, apiHive3ReplicationMetricsResultRow.scheduledExecutionId) &&
        Objects.equals(this.policy, apiHive3ReplicationMetricsResultRow.policy) &&
        Objects.equals(this.dumpExecutionId, apiHive3ReplicationMetricsResultRow.dumpExecutionId) &&
        Objects.equals(this.metadata, apiHive3ReplicationMetricsResultRow.metadata) &&
        Objects.equals(this.progress, apiHive3ReplicationMetricsResultRow.progress) &&
        Objects.equals(this.startDate, apiHive3ReplicationMetricsResultRow.startDate) &&
        Objects.equals(this.endDate, apiHive3ReplicationMetricsResultRow.endDate) &&
        Objects.equals(this.errorMessage, apiHive3ReplicationMetricsResultRow.errorMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scheduledExecutionId, policy, dumpExecutionId, metadata, progress, startDate, endDate, errorMessage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHive3ReplicationMetricsResultRow {\n");
    
    sb.append("    scheduledExecutionId: ").append(toIndentedString(scheduledExecutionId)).append("\n");
    sb.append("    policy: ").append(toIndentedString(policy)).append("\n");
    sb.append("    dumpExecutionId: ").append(toIndentedString(dumpExecutionId)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    progress: ").append(toIndentedString(progress)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
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

