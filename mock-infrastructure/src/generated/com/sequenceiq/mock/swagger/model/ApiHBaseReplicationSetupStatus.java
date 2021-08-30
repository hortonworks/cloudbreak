package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.Status;
import com.sequenceiq.mock.swagger.model.SubStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Contains status info about the HBase replication first time setup
 */
@ApiModel(description = "Contains status info about the HBase replication first time setup")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHBaseReplicationSetupStatus   {
  @JsonProperty("status")
  private Status status = null;

  @JsonProperty("subStatus")
  private SubStatus subStatus = null;

  @JsonProperty("mainCommandId")
  private ApiCommand mainCommandId = null;

  @JsonProperty("currentCommandId")
  private ApiCommand currentCommandId = null;

  @JsonProperty("error")
  private String error = null;

  public ApiHBaseReplicationSetupStatus status(Status status) {
    this.status = status;
    return this;
  }

  /**
   * 
   * @return status
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public ApiHBaseReplicationSetupStatus subStatus(SubStatus subStatus) {
    this.subStatus = subStatus;
    return this;
  }

  /**
   * 
   * @return subStatus
  **/
  @ApiModelProperty(value = "")

  @Valid

  public SubStatus getSubStatus() {
    return subStatus;
  }

  public void setSubStatus(SubStatus subStatus) {
    this.subStatus = subStatus;
  }

  public ApiHBaseReplicationSetupStatus mainCommandId(ApiCommand mainCommandId) {
    this.mainCommandId = mainCommandId;
    return this;
  }

  /**
   * 
   * @return mainCommandId
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiCommand getMainCommandId() {
    return mainCommandId;
  }

  public void setMainCommandId(ApiCommand mainCommandId) {
    this.mainCommandId = mainCommandId;
  }

  public ApiHBaseReplicationSetupStatus currentCommandId(ApiCommand currentCommandId) {
    this.currentCommandId = currentCommandId;
    return this;
  }

  /**
   * 
   * @return currentCommandId
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiCommand getCurrentCommandId() {
    return currentCommandId;
  }

  public void setCurrentCommandId(ApiCommand currentCommandId) {
    this.currentCommandId = currentCommandId;
  }

  public ApiHBaseReplicationSetupStatus error(String error) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHBaseReplicationSetupStatus apiHBaseReplicationSetupStatus = (ApiHBaseReplicationSetupStatus) o;
    return Objects.equals(this.status, apiHBaseReplicationSetupStatus.status) &&
        Objects.equals(this.subStatus, apiHBaseReplicationSetupStatus.subStatus) &&
        Objects.equals(this.mainCommandId, apiHBaseReplicationSetupStatus.mainCommandId) &&
        Objects.equals(this.currentCommandId, apiHBaseReplicationSetupStatus.currentCommandId) &&
        Objects.equals(this.error, apiHBaseReplicationSetupStatus.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, subStatus, mainCommandId, currentCommandId, error);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseReplicationSetupStatus {\n");
    
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    subStatus: ").append(toIndentedString(subStatus)).append("\n");
    sb.append("    mainCommandId: ").append(toIndentedString(mainCommandId)).append("\n");
    sb.append("    currentCommandId: ").append(toIndentedString(currentCommandId)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
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

