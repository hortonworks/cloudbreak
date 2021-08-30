package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterRef;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiHBaseReplicationInfo;
import com.sequenceiq.mock.swagger.model.ApiHdfsReplicationResult;
import com.sequenceiq.mock.swagger.model.ApiHiveOnTezReplicationResult;
import com.sequenceiq.mock.swagger.model.ApiHiveReplicationResult;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiRoleRef;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Information about a replication command. &lt;p/&gt; This object holds all the information a regular ApiCommand object provides, and adds specific information about the results of a replication command. &lt;p/&gt; Depending on the type of the service where the replication was run, a different result property will be populated.
 */
@ApiModel(description = "Information about a replication command. <p/> This object holds all the information a regular ApiCommand object provides, and adds specific information about the results of a replication command. <p/> Depending on the type of the service where the replication was run, a different result property will be populated.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiReplicationCommand extends ApiCommand  {
  @JsonProperty("hdfsResult")
  private ApiHdfsReplicationResult hdfsResult = null;

  @JsonProperty("hiveResult")
  private ApiHiveReplicationResult hiveResult = null;

  @JsonProperty("hiveOnTezResult")
  private ApiHiveOnTezReplicationResult hiveOnTezResult = null;

  @JsonProperty("hbaseInfo")
  private ApiHBaseReplicationInfo hbaseInfo = null;

  public ApiReplicationCommand hdfsResult(ApiHdfsReplicationResult hdfsResult) {
    this.hdfsResult = hdfsResult;
    return this;
  }

  /**
   * Results for replication commands on HDFS services.
   * @return hdfsResult
  **/
  @ApiModelProperty(value = "Results for replication commands on HDFS services.")

  @Valid

  public ApiHdfsReplicationResult getHdfsResult() {
    return hdfsResult;
  }

  public void setHdfsResult(ApiHdfsReplicationResult hdfsResult) {
    this.hdfsResult = hdfsResult;
  }

  public ApiReplicationCommand hiveResult(ApiHiveReplicationResult hiveResult) {
    this.hiveResult = hiveResult;
    return this;
  }

  /**
   * Results for replication commands on Hive services.
   * @return hiveResult
  **/
  @ApiModelProperty(value = "Results for replication commands on Hive services.")

  @Valid

  public ApiHiveReplicationResult getHiveResult() {
    return hiveResult;
  }

  public void setHiveResult(ApiHiveReplicationResult hiveResult) {
    this.hiveResult = hiveResult;
  }

  public ApiReplicationCommand hiveOnTezResult(ApiHiveOnTezReplicationResult hiveOnTezResult) {
    this.hiveOnTezResult = hiveOnTezResult;
    return this;
  }

  /**
   * Results for replication commands on HiveOnTez services.
   * @return hiveOnTezResult
  **/
  @ApiModelProperty(value = "Results for replication commands on HiveOnTez services.")

  @Valid

  public ApiHiveOnTezReplicationResult getHiveOnTezResult() {
    return hiveOnTezResult;
  }

  public void setHiveOnTezResult(ApiHiveOnTezReplicationResult hiveOnTezResult) {
    this.hiveOnTezResult = hiveOnTezResult;
  }

  public ApiReplicationCommand hbaseInfo(ApiHBaseReplicationInfo hbaseInfo) {
    this.hbaseInfo = hbaseInfo;
    return this;
  }

  /**
   * 
   * @return hbaseInfo
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHBaseReplicationInfo getHbaseInfo() {
    return hbaseInfo;
  }

  public void setHbaseInfo(ApiHBaseReplicationInfo hbaseInfo) {
    this.hbaseInfo = hbaseInfo;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiReplicationCommand apiReplicationCommand = (ApiReplicationCommand) o;
    return Objects.equals(this.hdfsResult, apiReplicationCommand.hdfsResult) &&
        Objects.equals(this.hiveResult, apiReplicationCommand.hiveResult) &&
        Objects.equals(this.hiveOnTezResult, apiReplicationCommand.hiveOnTezResult) &&
        Objects.equals(this.hbaseInfo, apiReplicationCommand.hbaseInfo) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hdfsResult, hiveResult, hiveOnTezResult, hbaseInfo, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiReplicationCommand {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    hdfsResult: ").append(toIndentedString(hdfsResult)).append("\n");
    sb.append("    hiveResult: ").append(toIndentedString(hiveResult)).append("\n");
    sb.append("    hiveOnTezResult: ").append(toIndentedString(hiveOnTezResult)).append("\n");
    sb.append("    hbaseInfo: ").append(toIndentedString(hbaseInfo)).append("\n");
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

