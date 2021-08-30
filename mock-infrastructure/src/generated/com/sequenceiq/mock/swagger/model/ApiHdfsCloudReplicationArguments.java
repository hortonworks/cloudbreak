package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHdfsReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import com.sequenceiq.mock.swagger.model.ReplicationStrategy;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Replication arguments for HDFS.
 */
@ApiModel(description = "Replication arguments for HDFS.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsCloudReplicationArguments extends ApiHdfsReplicationArguments  {
  @JsonProperty("sourceAccount")
  private String sourceAccount = null;

  @JsonProperty("destinationAccount")
  private String destinationAccount = null;

  public ApiHdfsCloudReplicationArguments sourceAccount(String sourceAccount) {
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

  public ApiHdfsCloudReplicationArguments destinationAccount(String destinationAccount) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsCloudReplicationArguments apiHdfsCloudReplicationArguments = (ApiHdfsCloudReplicationArguments) o;
    return Objects.equals(this.sourceAccount, apiHdfsCloudReplicationArguments.sourceAccount) &&
        Objects.equals(this.destinationAccount, apiHdfsCloudReplicationArguments.destinationAccount) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceAccount, destinationAccount, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsCloudReplicationArguments {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    sourceAccount: ").append(toIndentedString(sourceAccount)).append("\n");
    sb.append("    destinationAccount: ").append(toIndentedString(destinationAccount)).append("\n");
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

