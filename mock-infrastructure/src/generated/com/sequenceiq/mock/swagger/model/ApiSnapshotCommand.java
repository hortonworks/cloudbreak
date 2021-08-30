package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterRef;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiHBaseSnapshotResult;
import com.sequenceiq.mock.swagger.model.ApiHdfsSnapshotResult;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiRoleRef;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Information about snapshot commands. &lt;p/&gt; This object holds all the information a regular ApiCommand object provides, and adds specific information about the results of a snapshot command. &lt;p/&gt; Depending on the type of the service where the snapshot command was run, a different result property will be populated.
 */
@ApiModel(description = "Information about snapshot commands. <p/> This object holds all the information a regular ApiCommand object provides, and adds specific information about the results of a snapshot command. <p/> Depending on the type of the service where the snapshot command was run, a different result property will be populated.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiSnapshotCommand extends ApiCommand  {
  @JsonProperty("hbaseResult")
  private ApiHBaseSnapshotResult hbaseResult = null;

  @JsonProperty("hdfsResult")
  private ApiHdfsSnapshotResult hdfsResult = null;

  public ApiSnapshotCommand hbaseResult(ApiHBaseSnapshotResult hbaseResult) {
    this.hbaseResult = hbaseResult;
    return this;
  }

  /**
   * Results for snapshot commands on HBase services.
   * @return hbaseResult
  **/
  @ApiModelProperty(value = "Results for snapshot commands on HBase services.")

  @Valid

  public ApiHBaseSnapshotResult getHbaseResult() {
    return hbaseResult;
  }

  public void setHbaseResult(ApiHBaseSnapshotResult hbaseResult) {
    this.hbaseResult = hbaseResult;
  }

  public ApiSnapshotCommand hdfsResult(ApiHdfsSnapshotResult hdfsResult) {
    this.hdfsResult = hdfsResult;
    return this;
  }

  /**
   * Results for snapshot commands on Hdfs services.
   * @return hdfsResult
  **/
  @ApiModelProperty(value = "Results for snapshot commands on Hdfs services.")

  @Valid

  public ApiHdfsSnapshotResult getHdfsResult() {
    return hdfsResult;
  }

  public void setHdfsResult(ApiHdfsSnapshotResult hdfsResult) {
    this.hdfsResult = hdfsResult;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiSnapshotCommand apiSnapshotCommand = (ApiSnapshotCommand) o;
    return Objects.equals(this.hbaseResult, apiSnapshotCommand.hbaseResult) &&
        Objects.equals(this.hdfsResult, apiSnapshotCommand.hdfsResult) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hbaseResult, hdfsResult, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiSnapshotCommand {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    hbaseResult: ").append(toIndentedString(hbaseResult)).append("\n");
    sb.append("    hdfsResult: ").append(toIndentedString(hdfsResult)).append("\n");
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

