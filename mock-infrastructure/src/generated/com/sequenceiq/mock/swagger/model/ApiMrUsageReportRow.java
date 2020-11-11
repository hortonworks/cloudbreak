package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * 
 */
@ApiModel(description = "")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiMrUsageReportRow   {
  @JsonProperty("timePeriod")
  private String timePeriod = null;

  @JsonProperty("user")
  private String user = null;

  @JsonProperty("group")
  private String group = null;

  @JsonProperty("cpuSec")
  private BigDecimal cpuSec = null;

  @JsonProperty("memoryBytes")
  private BigDecimal memoryBytes = null;

  @JsonProperty("jobCount")
  private BigDecimal jobCount = null;

  @JsonProperty("taskCount")
  private BigDecimal taskCount = null;

  @JsonProperty("durationSec")
  private BigDecimal durationSec = null;

  @JsonProperty("failedMaps")
  private BigDecimal failedMaps = null;

  @JsonProperty("totalMaps")
  private BigDecimal totalMaps = null;

  @JsonProperty("failedReduces")
  private BigDecimal failedReduces = null;

  @JsonProperty("totalReduces")
  private BigDecimal totalReduces = null;

  @JsonProperty("mapInputBytes")
  private BigDecimal mapInputBytes = null;

  @JsonProperty("mapOutputBytes")
  private BigDecimal mapOutputBytes = null;

  @JsonProperty("hdfsBytesRead")
  private BigDecimal hdfsBytesRead = null;

  @JsonProperty("hdfsBytesWritten")
  private BigDecimal hdfsBytesWritten = null;

  @JsonProperty("localBytesRead")
  private BigDecimal localBytesRead = null;

  @JsonProperty("localBytesWritten")
  private BigDecimal localBytesWritten = null;

  @JsonProperty("dataLocalMaps")
  private BigDecimal dataLocalMaps = null;

  @JsonProperty("rackLocalMaps")
  private BigDecimal rackLocalMaps = null;

  public ApiMrUsageReportRow timePeriod(String timePeriod) {
    this.timePeriod = timePeriod;
    return this;
  }

  /**
   * The time period over which this report is generated.
   * @return timePeriod
  **/
  @ApiModelProperty(value = "The time period over which this report is generated.")


  public String getTimePeriod() {
    return timePeriod;
  }

  public void setTimePeriod(String timePeriod) {
    this.timePeriod = timePeriod;
  }

  public ApiMrUsageReportRow user(String user) {
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

  public ApiMrUsageReportRow group(String group) {
    this.group = group;
    return this;
  }

  /**
   * The group this user belongs to.
   * @return group
  **/
  @ApiModelProperty(value = "The group this user belongs to.")


  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public ApiMrUsageReportRow cpuSec(BigDecimal cpuSec) {
    this.cpuSec = cpuSec;
    return this;
  }

  /**
   * Amount of CPU time (in seconds) taken up this user's MapReduce jobs.
   * @return cpuSec
  **/
  @ApiModelProperty(value = "Amount of CPU time (in seconds) taken up this user's MapReduce jobs.")

  @Valid

  public BigDecimal getCpuSec() {
    return cpuSec;
  }

  public void setCpuSec(BigDecimal cpuSec) {
    this.cpuSec = cpuSec;
  }

  public ApiMrUsageReportRow memoryBytes(BigDecimal memoryBytes) {
    this.memoryBytes = memoryBytes;
    return this;
  }

  /**
   * The sum of physical memory used (collected as a snapshot) by this user's MapReduce jobs.
   * @return memoryBytes
  **/
  @ApiModelProperty(value = "The sum of physical memory used (collected as a snapshot) by this user's MapReduce jobs.")

  @Valid

  public BigDecimal getMemoryBytes() {
    return memoryBytes;
  }

  public void setMemoryBytes(BigDecimal memoryBytes) {
    this.memoryBytes = memoryBytes;
  }

  public ApiMrUsageReportRow jobCount(BigDecimal jobCount) {
    this.jobCount = jobCount;
    return this;
  }

  /**
   * Number of jobs.
   * @return jobCount
  **/
  @ApiModelProperty(value = "Number of jobs.")

  @Valid

  public BigDecimal getJobCount() {
    return jobCount;
  }

  public void setJobCount(BigDecimal jobCount) {
    this.jobCount = jobCount;
  }

  public ApiMrUsageReportRow taskCount(BigDecimal taskCount) {
    this.taskCount = taskCount;
    return this;
  }

  /**
   * Number of tasks.
   * @return taskCount
  **/
  @ApiModelProperty(value = "Number of tasks.")

  @Valid

  public BigDecimal getTaskCount() {
    return taskCount;
  }

  public void setTaskCount(BigDecimal taskCount) {
    this.taskCount = taskCount;
  }

  public ApiMrUsageReportRow durationSec(BigDecimal durationSec) {
    this.durationSec = durationSec;
    return this;
  }

  /**
   * Total duration of this user's MapReduce jobs.
   * @return durationSec
  **/
  @ApiModelProperty(value = "Total duration of this user's MapReduce jobs.")

  @Valid

  public BigDecimal getDurationSec() {
    return durationSec;
  }

  public void setDurationSec(BigDecimal durationSec) {
    this.durationSec = durationSec;
  }

  public ApiMrUsageReportRow failedMaps(BigDecimal failedMaps) {
    this.failedMaps = failedMaps;
    return this;
  }

  /**
   * Failed maps of this user's MapReduce jobs. Available since v11.
   * @return failedMaps
  **/
  @ApiModelProperty(value = "Failed maps of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getFailedMaps() {
    return failedMaps;
  }

  public void setFailedMaps(BigDecimal failedMaps) {
    this.failedMaps = failedMaps;
  }

  public ApiMrUsageReportRow totalMaps(BigDecimal totalMaps) {
    this.totalMaps = totalMaps;
    return this;
  }

  /**
   * Total maps of this user's MapReduce jobs. Available since v11.
   * @return totalMaps
  **/
  @ApiModelProperty(value = "Total maps of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getTotalMaps() {
    return totalMaps;
  }

  public void setTotalMaps(BigDecimal totalMaps) {
    this.totalMaps = totalMaps;
  }

  public ApiMrUsageReportRow failedReduces(BigDecimal failedReduces) {
    this.failedReduces = failedReduces;
    return this;
  }

  /**
   * Failed reduces of this user's MapReduce jobs. Available since v11.
   * @return failedReduces
  **/
  @ApiModelProperty(value = "Failed reduces of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getFailedReduces() {
    return failedReduces;
  }

  public void setFailedReduces(BigDecimal failedReduces) {
    this.failedReduces = failedReduces;
  }

  public ApiMrUsageReportRow totalReduces(BigDecimal totalReduces) {
    this.totalReduces = totalReduces;
    return this;
  }

  /**
   * Total reduces of this user's MapReduce jobs. Available since v11.
   * @return totalReduces
  **/
  @ApiModelProperty(value = "Total reduces of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getTotalReduces() {
    return totalReduces;
  }

  public void setTotalReduces(BigDecimal totalReduces) {
    this.totalReduces = totalReduces;
  }

  public ApiMrUsageReportRow mapInputBytes(BigDecimal mapInputBytes) {
    this.mapInputBytes = mapInputBytes;
    return this;
  }

  /**
   * Map input bytes of this user's MapReduce jobs. Available since v11.
   * @return mapInputBytes
  **/
  @ApiModelProperty(value = "Map input bytes of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getMapInputBytes() {
    return mapInputBytes;
  }

  public void setMapInputBytes(BigDecimal mapInputBytes) {
    this.mapInputBytes = mapInputBytes;
  }

  public ApiMrUsageReportRow mapOutputBytes(BigDecimal mapOutputBytes) {
    this.mapOutputBytes = mapOutputBytes;
    return this;
  }

  /**
   * Map output bytes of this user's MapReduce jobs. Available since v11.
   * @return mapOutputBytes
  **/
  @ApiModelProperty(value = "Map output bytes of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getMapOutputBytes() {
    return mapOutputBytes;
  }

  public void setMapOutputBytes(BigDecimal mapOutputBytes) {
    this.mapOutputBytes = mapOutputBytes;
  }

  public ApiMrUsageReportRow hdfsBytesRead(BigDecimal hdfsBytesRead) {
    this.hdfsBytesRead = hdfsBytesRead;
    return this;
  }

  /**
   * HDFS bytes read of this user's MapReduce jobs. Available since v11.
   * @return hdfsBytesRead
  **/
  @ApiModelProperty(value = "HDFS bytes read of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getHdfsBytesRead() {
    return hdfsBytesRead;
  }

  public void setHdfsBytesRead(BigDecimal hdfsBytesRead) {
    this.hdfsBytesRead = hdfsBytesRead;
  }

  public ApiMrUsageReportRow hdfsBytesWritten(BigDecimal hdfsBytesWritten) {
    this.hdfsBytesWritten = hdfsBytesWritten;
    return this;
  }

  /**
   * HDFS bytes written of this user's MapReduce jobs. Available since v11.
   * @return hdfsBytesWritten
  **/
  @ApiModelProperty(value = "HDFS bytes written of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getHdfsBytesWritten() {
    return hdfsBytesWritten;
  }

  public void setHdfsBytesWritten(BigDecimal hdfsBytesWritten) {
    this.hdfsBytesWritten = hdfsBytesWritten;
  }

  public ApiMrUsageReportRow localBytesRead(BigDecimal localBytesRead) {
    this.localBytesRead = localBytesRead;
    return this;
  }

  /**
   * Local bytes read of this user's MapReduce jobs. Available since v11.
   * @return localBytesRead
  **/
  @ApiModelProperty(value = "Local bytes read of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getLocalBytesRead() {
    return localBytesRead;
  }

  public void setLocalBytesRead(BigDecimal localBytesRead) {
    this.localBytesRead = localBytesRead;
  }

  public ApiMrUsageReportRow localBytesWritten(BigDecimal localBytesWritten) {
    this.localBytesWritten = localBytesWritten;
    return this;
  }

  /**
   * Local bytes written of this user's MapReduce jobs. Available since v11.
   * @return localBytesWritten
  **/
  @ApiModelProperty(value = "Local bytes written of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getLocalBytesWritten() {
    return localBytesWritten;
  }

  public void setLocalBytesWritten(BigDecimal localBytesWritten) {
    this.localBytesWritten = localBytesWritten;
  }

  public ApiMrUsageReportRow dataLocalMaps(BigDecimal dataLocalMaps) {
    this.dataLocalMaps = dataLocalMaps;
    return this;
  }

  /**
   * Data local maps of this user's MapReduce jobs. Available since v11.
   * @return dataLocalMaps
  **/
  @ApiModelProperty(value = "Data local maps of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getDataLocalMaps() {
    return dataLocalMaps;
  }

  public void setDataLocalMaps(BigDecimal dataLocalMaps) {
    this.dataLocalMaps = dataLocalMaps;
  }

  public ApiMrUsageReportRow rackLocalMaps(BigDecimal rackLocalMaps) {
    this.rackLocalMaps = rackLocalMaps;
    return this;
  }

  /**
   * Rack local maps of this user's MapReduce jobs. Available since v11.
   * @return rackLocalMaps
  **/
  @ApiModelProperty(value = "Rack local maps of this user's MapReduce jobs. Available since v11.")

  @Valid

  public BigDecimal getRackLocalMaps() {
    return rackLocalMaps;
  }

  public void setRackLocalMaps(BigDecimal rackLocalMaps) {
    this.rackLocalMaps = rackLocalMaps;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiMrUsageReportRow apiMrUsageReportRow = (ApiMrUsageReportRow) o;
    return Objects.equals(this.timePeriod, apiMrUsageReportRow.timePeriod) &&
        Objects.equals(this.user, apiMrUsageReportRow.user) &&
        Objects.equals(this.group, apiMrUsageReportRow.group) &&
        Objects.equals(this.cpuSec, apiMrUsageReportRow.cpuSec) &&
        Objects.equals(this.memoryBytes, apiMrUsageReportRow.memoryBytes) &&
        Objects.equals(this.jobCount, apiMrUsageReportRow.jobCount) &&
        Objects.equals(this.taskCount, apiMrUsageReportRow.taskCount) &&
        Objects.equals(this.durationSec, apiMrUsageReportRow.durationSec) &&
        Objects.equals(this.failedMaps, apiMrUsageReportRow.failedMaps) &&
        Objects.equals(this.totalMaps, apiMrUsageReportRow.totalMaps) &&
        Objects.equals(this.failedReduces, apiMrUsageReportRow.failedReduces) &&
        Objects.equals(this.totalReduces, apiMrUsageReportRow.totalReduces) &&
        Objects.equals(this.mapInputBytes, apiMrUsageReportRow.mapInputBytes) &&
        Objects.equals(this.mapOutputBytes, apiMrUsageReportRow.mapOutputBytes) &&
        Objects.equals(this.hdfsBytesRead, apiMrUsageReportRow.hdfsBytesRead) &&
        Objects.equals(this.hdfsBytesWritten, apiMrUsageReportRow.hdfsBytesWritten) &&
        Objects.equals(this.localBytesRead, apiMrUsageReportRow.localBytesRead) &&
        Objects.equals(this.localBytesWritten, apiMrUsageReportRow.localBytesWritten) &&
        Objects.equals(this.dataLocalMaps, apiMrUsageReportRow.dataLocalMaps) &&
        Objects.equals(this.rackLocalMaps, apiMrUsageReportRow.rackLocalMaps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timePeriod, user, group, cpuSec, memoryBytes, jobCount, taskCount, durationSec, failedMaps, totalMaps, failedReduces, totalReduces, mapInputBytes, mapOutputBytes, hdfsBytesRead, hdfsBytesWritten, localBytesRead, localBytesWritten, dataLocalMaps, rackLocalMaps);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiMrUsageReportRow {\n");
    
    sb.append("    timePeriod: ").append(toIndentedString(timePeriod)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    group: ").append(toIndentedString(group)).append("\n");
    sb.append("    cpuSec: ").append(toIndentedString(cpuSec)).append("\n");
    sb.append("    memoryBytes: ").append(toIndentedString(memoryBytes)).append("\n");
    sb.append("    jobCount: ").append(toIndentedString(jobCount)).append("\n");
    sb.append("    taskCount: ").append(toIndentedString(taskCount)).append("\n");
    sb.append("    durationSec: ").append(toIndentedString(durationSec)).append("\n");
    sb.append("    failedMaps: ").append(toIndentedString(failedMaps)).append("\n");
    sb.append("    totalMaps: ").append(toIndentedString(totalMaps)).append("\n");
    sb.append("    failedReduces: ").append(toIndentedString(failedReduces)).append("\n");
    sb.append("    totalReduces: ").append(toIndentedString(totalReduces)).append("\n");
    sb.append("    mapInputBytes: ").append(toIndentedString(mapInputBytes)).append("\n");
    sb.append("    mapOutputBytes: ").append(toIndentedString(mapOutputBytes)).append("\n");
    sb.append("    hdfsBytesRead: ").append(toIndentedString(hdfsBytesRead)).append("\n");
    sb.append("    hdfsBytesWritten: ").append(toIndentedString(hdfsBytesWritten)).append("\n");
    sb.append("    localBytesRead: ").append(toIndentedString(localBytesRead)).append("\n");
    sb.append("    localBytesWritten: ").append(toIndentedString(localBytesWritten)).append("\n");
    sb.append("    dataLocalMaps: ").append(toIndentedString(dataLocalMaps)).append("\n");
    sb.append("    rackLocalMaps: ").append(toIndentedString(rackLocalMaps)).append("\n");
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

