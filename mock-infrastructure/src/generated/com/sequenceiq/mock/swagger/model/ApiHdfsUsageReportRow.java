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
 * 
 */
@ApiModel(description = "")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsUsageReportRow   {
  @JsonProperty("date")
  private String date = null;

  @JsonProperty("user")
  private String user = null;

  @JsonProperty("size")
  private Integer size = null;

  @JsonProperty("rawSize")
  private Integer rawSize = null;

  @JsonProperty("numFiles")
  private Integer numFiles = null;

  public ApiHdfsUsageReportRow date(String date) {
    this.date = date;
    return this;
  }

  /**
   * The date of the report row data.
   * @return date
  **/
  @ApiModelProperty(value = "The date of the report row data.")


  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public ApiHdfsUsageReportRow user(String user) {
    this.user = user;
    return this;
  }

  /**
   * The user being reported.
   * @return user
  **/
  @ApiModelProperty(value = "The user being reported.")


  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public ApiHdfsUsageReportRow size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * Total size (in bytes) of the files owned by this user. This does not include replication in HDFS.
   * @return size
  **/
  @ApiModelProperty(value = "Total size (in bytes) of the files owned by this user. This does not include replication in HDFS.")


  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public ApiHdfsUsageReportRow rawSize(Integer rawSize) {
    this.rawSize = rawSize;
    return this;
  }

  /**
   * Total size (in bytes) of all the replicas of all the files owned by this user.
   * @return rawSize
  **/
  @ApiModelProperty(value = "Total size (in bytes) of all the replicas of all the files owned by this user.")


  public Integer getRawSize() {
    return rawSize;
  }

  public void setRawSize(Integer rawSize) {
    this.rawSize = rawSize;
  }

  public ApiHdfsUsageReportRow numFiles(Integer numFiles) {
    this.numFiles = numFiles;
    return this;
  }

  /**
   * Number of files owned by this user.
   * @return numFiles
  **/
  @ApiModelProperty(value = "Number of files owned by this user.")


  public Integer getNumFiles() {
    return numFiles;
  }

  public void setNumFiles(Integer numFiles) {
    this.numFiles = numFiles;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsUsageReportRow apiHdfsUsageReportRow = (ApiHdfsUsageReportRow) o;
    return Objects.equals(this.date, apiHdfsUsageReportRow.date) &&
        Objects.equals(this.user, apiHdfsUsageReportRow.user) &&
        Objects.equals(this.size, apiHdfsUsageReportRow.size) &&
        Objects.equals(this.rawSize, apiHdfsUsageReportRow.rawSize) &&
        Objects.equals(this.numFiles, apiHdfsUsageReportRow.numFiles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, user, size, rawSize, numFiles);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsUsageReportRow {\n");
    
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    rawSize: ").append(toIndentedString(rawSize)).append("\n");
    sb.append("    numFiles: ").append(toIndentedString(numFiles)).append("\n");
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

