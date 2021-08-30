package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * An HDFS snapshot operation error.
 */
@ApiModel(description = "An HDFS snapshot operation error.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsSnapshotError   {
  @JsonProperty("path")
  private String path = null;

  @JsonProperty("snapshotName")
  private String snapshotName = null;

  @JsonProperty("error")
  private String error = null;

  public ApiHdfsSnapshotError path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Path for which the snapshot error occurred.
   * @return path
  **/
  @ApiModelProperty(value = "Path for which the snapshot error occurred.")


  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public ApiHdfsSnapshotError snapshotName(String snapshotName) {
    this.snapshotName = snapshotName;
    return this;
  }

  /**
   * Name of snapshot for which error occurred.
   * @return snapshotName
  **/
  @ApiModelProperty(value = "Name of snapshot for which error occurred.")


  public String getSnapshotName() {
    return snapshotName;
  }

  public void setSnapshotName(String snapshotName) {
    this.snapshotName = snapshotName;
  }

  public ApiHdfsSnapshotError error(String error) {
    this.error = error;
    return this;
  }

  /**
   * Description of the error.
   * @return error
  **/
  @ApiModelProperty(value = "Description of the error.")


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
    ApiHdfsSnapshotError apiHdfsSnapshotError = (ApiHdfsSnapshotError) o;
    return Objects.equals(this.path, apiHdfsSnapshotError.path) &&
        Objects.equals(this.snapshotName, apiHdfsSnapshotError.snapshotName) &&
        Objects.equals(this.error, apiHdfsSnapshotError.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, snapshotName, error);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsSnapshotError {\n");
    
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    snapshotName: ").append(toIndentedString(snapshotName)).append("\n");
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

