package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHdfsReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiHiveReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiHiveTable;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import com.sequenceiq.mock.swagger.model.ReplicationOption;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Replication arguments for Hive services.
 */
@ApiModel(description = "Replication arguments for Hive services.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHiveCloudReplicationArguments extends ApiHiveReplicationArguments  {
  @JsonProperty("sourceAccount")
  private String sourceAccount = null;

  @JsonProperty("destinationAccount")
  private String destinationAccount = null;

  @JsonProperty("cloudRootPath")
  private String cloudRootPath = null;

  @JsonProperty("replicationOption")
  private ReplicationOption replicationOption = null;

  public ApiHiveCloudReplicationArguments sourceAccount(String sourceAccount) {
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

  public ApiHiveCloudReplicationArguments destinationAccount(String destinationAccount) {
    this.destinationAccount = destinationAccount;
    return this;
  }

  /**
   * 
   * @return destinationAccount
  **/
  @ApiModelProperty(value = "")


  public String getDestinationAccount() {
    return destinationAccount;
  }

  public void setDestinationAccount(String destinationAccount) {
    this.destinationAccount = destinationAccount;
  }

  public ApiHiveCloudReplicationArguments cloudRootPath(String cloudRootPath) {
    this.cloudRootPath = cloudRootPath;
    return this;
  }

  /**
   * 
   * @return cloudRootPath
  **/
  @ApiModelProperty(value = "")


  public String getCloudRootPath() {
    return cloudRootPath;
  }

  public void setCloudRootPath(String cloudRootPath) {
    this.cloudRootPath = cloudRootPath;
  }

  public ApiHiveCloudReplicationArguments replicationOption(ReplicationOption replicationOption) {
    this.replicationOption = replicationOption;
    return this;
  }

  /**
   * 
   * @return replicationOption
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ReplicationOption getReplicationOption() {
    return replicationOption;
  }

  public void setReplicationOption(ReplicationOption replicationOption) {
    this.replicationOption = replicationOption;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHiveCloudReplicationArguments apiHiveCloudReplicationArguments = (ApiHiveCloudReplicationArguments) o;
    return Objects.equals(this.sourceAccount, apiHiveCloudReplicationArguments.sourceAccount) &&
        Objects.equals(this.destinationAccount, apiHiveCloudReplicationArguments.destinationAccount) &&
        Objects.equals(this.cloudRootPath, apiHiveCloudReplicationArguments.cloudRootPath) &&
        Objects.equals(this.replicationOption, apiHiveCloudReplicationArguments.replicationOption) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceAccount, destinationAccount, cloudRootPath, replicationOption, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHiveCloudReplicationArguments {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    sourceAccount: ").append(toIndentedString(sourceAccount)).append("\n");
    sb.append("    destinationAccount: ").append(toIndentedString(destinationAccount)).append("\n");
    sb.append("    cloudRootPath: ").append(toIndentedString(cloudRootPath)).append("\n");
    sb.append("    replicationOption: ").append(toIndentedString(replicationOption)).append("\n");
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

