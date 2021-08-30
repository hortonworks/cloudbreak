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
 * An HDFS snapshot descriptor.
 */
@ApiModel(description = "An HDFS snapshot descriptor.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsSnapshot   {
  @JsonProperty("path")
  private String path = null;

  @JsonProperty("snapshotName")
  private String snapshotName = null;

  @JsonProperty("snapshotPath")
  private String snapshotPath = null;

  @JsonProperty("creationTime")
  private String creationTime = null;

  public ApiHdfsSnapshot path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Snapshotted path.
   * @return path
  **/
  @ApiModelProperty(value = "Snapshotted path.")


  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public ApiHdfsSnapshot snapshotName(String snapshotName) {
    this.snapshotName = snapshotName;
    return this;
  }

  /**
   * Snapshot name.
   * @return snapshotName
  **/
  @ApiModelProperty(value = "Snapshot name.")


  public String getSnapshotName() {
    return snapshotName;
  }

  public void setSnapshotName(String snapshotName) {
    this.snapshotName = snapshotName;
  }

  public ApiHdfsSnapshot snapshotPath(String snapshotPath) {
    this.snapshotPath = snapshotPath;
    return this;
  }

  /**
   * Read-only. Fully qualified path for the snapshot version of \"path\". <p/> For example, if a snapshot \"s1\" is present at \"/a/.snapshot/s1, then the snapshot path corresponding to \"s1\" for path \"/a/b\" will be \"/a/.snapshot/s1/b\".
   * @return snapshotPath
  **/
  @ApiModelProperty(value = "Read-only. Fully qualified path for the snapshot version of \"path\". <p/> For example, if a snapshot \"s1\" is present at \"/a/.snapshot/s1, then the snapshot path corresponding to \"s1\" for path \"/a/b\" will be \"/a/.snapshot/s1/b\".")


  public String getSnapshotPath() {
    return snapshotPath;
  }

  public void setSnapshotPath(String snapshotPath) {
    this.snapshotPath = snapshotPath;
  }

  public ApiHdfsSnapshot creationTime(String creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  /**
   * Snapshot creation time.
   * @return creationTime
  **/
  @ApiModelProperty(value = "Snapshot creation time.")


  public String getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(String creationTime) {
    this.creationTime = creationTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsSnapshot apiHdfsSnapshot = (ApiHdfsSnapshot) o;
    return Objects.equals(this.path, apiHdfsSnapshot.path) &&
        Objects.equals(this.snapshotName, apiHdfsSnapshot.snapshotName) &&
        Objects.equals(this.snapshotPath, apiHdfsSnapshot.snapshotPath) &&
        Objects.equals(this.creationTime, apiHdfsSnapshot.creationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, snapshotName, snapshotPath, creationTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsSnapshot {\n");
    
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    snapshotName: ").append(toIndentedString(snapshotName)).append("\n");
    sb.append("    snapshotPath: ").append(toIndentedString(snapshotPath)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
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

