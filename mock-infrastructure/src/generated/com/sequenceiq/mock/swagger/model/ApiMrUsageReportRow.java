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




public class ApiMrUsageReportRow   {
  @JsonProperty("timePeriod")
  private String timePeriod = null;

  @JsonProperty("user")
  private String user = null;

  @JsonProperty("group")
  private String group = null;

  @JsonProperty("cpuSec")
  private Integer cpuSec = null;

  @JsonProperty("memoryBytes")
  private Integer memoryBytes = null;

  @JsonProperty("jobCount")
  private Integer jobCount = null;

  @JsonProperty("taskCount")
  private Integer taskCount = null;

  @JsonProperty("durationSec")
  private Integer durationSec = null;

  @JsonProperty("failedMaps")
  private Integer failedMaps = null;

  @JsonProperty("totalMaps")
  private Integer totalMaps = null;

  @JsonProperty("failedReduces")
  private Integer failedReduces = null;

  @JsonProperty("totalReduces")
  private Integer totalReduces = null;

  @JsonProperty("mapInputBytes")
  private Integer mapInputBytes = null;

  @JsonProperty("mapOutputBytes")
  private Integer mapOutputBytes = null;

  @JsonProperty("hdfsBytesRead")
  private Integer hdfsBytesRead = null;

  @JsonProperty("hdfsBytesWritten")
  private Integer hdfsBytesWritten = null;

  @JsonProperty("localBytesRead")
  private Integer localBytesRead = null;

  @JsonProperty("localBytesWritten")
  private Integer localBytesWritten = null;

  @JsonProperty("dataLocalMaps")
  private Integer dataLocalMaps = null;

  @JsonProperty("rackLocalMaps")
  private Integer rackLocalMaps = null;

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

  public ApiMrUsageReportRow cpuSec(Integer cpuSec) {
    this.cpuSec = cpuSec;
    return this;
  }

  /**
   * Amount of CPU time (in seconds) taken up this user's MapReduce jobs.
   * @return cpuSec
  **/
  @ApiModelProperty(value = "Amount of CPU time (in seconds) taken up this user's MapReduce jobs.")


  public Integer getCpuSec() {
    return cpuSec;
  }

  public void setCpuSec(Integer cpuSec) {
    this.cpuSec = cpuSec;
  }

  public ApiMrUsageReportRow memoryBytes(Integer memoryBytes) {
    this.memoryBytes = memoryBytes;
    return this;
  }

  /**
   * The sum of physical memory used (collected as a snapshot) by this user's MapReduce jobs.
   * @return memoryBytes
  **/
  @ApiModelProperty(value = "The sum of physical memory used (collected as a snapshot) by this user's MapReduce jobs.")


  public Integer getMemoryBytes() {
    return memoryBytes;
  }

  public void setMemoryBytes(Integer memoryBytes) {
    this.memoryBytes = memoryBytes;
  }

  public ApiMrUsageReportRow jobCount(Integer jobCount) {
    this.jobCount = jobCount;
    return this;
  }

  /**
   * Number of jobs.
   * @return jobCount
  **/
  @ApiModelProperty(value = "Number of jobs.")


  public Integer getJobCount() {
    return jobCount;
  }

  public void setJobCount(Integer jobCount) {
    this.jobCount = jobCount;
  }

  public ApiMrUsageReportRow taskCount(Integer taskCount) {
    this.taskCount = taskCount;
    return this;
  }

  /**
   * Number of tasks.
   * @return taskCount
  **/
  @ApiModelProperty(value = "Number of tasks.")


  public Integer getTaskCount() {
    return taskCount;
  }

  public void setTaskCount(Integer taskCount) {
    this.taskCount = taskCount;
  }

  public ApiMrUsageReportRow durationSec(Integer durationSec) {
    this.durationSec = durationSec;
    return this;
  }

  /**
   * Total duration of this user's MapReduce jobs.
   * @return durationSec
  **/
  @ApiModelProperty(value = "Total duration of this user's MapReduce jobs.")


  public Integer getDurationSec() {
    return durationSec;
  }

  public void setDurationSec(Integer durationSec) {
    this.durationSec = durationSec;
  }

  public ApiMrUsageReportRow failedMaps(Integer failedMaps) {
    this.failedMaps = failedMaps;
    return this;
  }

  /**
   * Failed maps of this user's MapReduce jobs. Available since v11.
   * @return failedMaps
  **/
  @ApiModelProperty(value = "Failed maps of this user's MapReduce jobs. Available since v11.")


  public Integer getFailedMaps() {
    return failedMaps;
  }

  public void setFailedMaps(Integer failedMaps) {
    this.failedMaps = failedMaps;
  }

  public ApiMrUsageReportRow totalMaps(Integer totalMaps) {
    this.totalMaps = totalMaps;
    return this;
  }

  /**
   * Total maps of this user's MapReduce jobs. Available since v11.
   * @return totalMaps
  **/
  @ApiModelProperty(value = "Total maps of this user's MapReduce jobs. Available since v11.")


  public Integer getTotalMaps() {
    return totalMaps;
  }

  public void setTotalMaps(Integer totalMaps) {
    this.totalMaps = totalMaps;
  }

  public ApiMrUsageReportRow failedReduces(Integer failedReduces) {
    this.failedReduces = failedReduces;
    return this;
  }

  /**
   * Failed reduces of this user's MapReduce jobs. Available since v11.
   * @return failedReduces
  **/
  @ApiModelProperty(value = "Failed reduces of this user's MapReduce jobs. Available since v11.")


  public Integer getFailedReduces() {
    return failedReduces;
  }

  public void setFailedReduces(Integer failedReduces) {
    this.failedReduces = failedReduces;
  }

  public ApiMrUsageReportRow totalReduces(Integer totalReduces) {
    this.totalReduces = totalReduces;
    return this;
  }

  /**
   * Total reduces of this user's MapReduce jobs. Available since v11.
   * @return totalReduces
  **/
  @ApiModelProperty(value = "Total reduces of this user's MapReduce jobs. Available since v11.")


  public Integer getTotalReduces() {
    return totalReduces;
  }

  public void setTotalReduces(Integer totalReduces) {
    this.totalReduces = totalReduces;
  }

  public ApiMrUsageReportRow mapInputBytes(Integer mapInputBytes) {
    this.mapInputBytes = mapInputBytes;
    return this;
  }

  /**
   * Map input bytes of this user's MapReduce jobs. Available since v11.
   * @return mapInputBytes
  **/
  @ApiModelProperty(value = "Map input bytes of this user's MapReduce jobs. Available since v11.")


  public Integer getMapInputBytes() {
    return mapInputBytes;
  }

  public void setMapInputBytes(Integer mapInputBytes) {
    this.mapInputBytes = mapInputBytes;
  }

  public ApiMrUsageReportRow mapOutputBytes(Integer mapOutputBytes) {
    this.mapOutputBytes = mapOutputBytes;
    return this;
  }

  /**
   * Map output bytes of this user's MapReduce jobs. Available since v11.
   * @return mapOutputBytes
  **/
  @ApiModelProperty(value = "Map output bytes of this user's MapReduce jobs. Available since v11.")


  public Integer getMapOutputBytes() {
    return mapOutputBytes;
  }

  public void setMapOutputBytes(Integer mapOutputBytes) {
    this.mapOutputBytes = mapOutputBytes;
  }

  public ApiMrUsageReportRow hdfsBytesRead(Integer hdfsBytesRead) {
    this.hdfsBytesRead = hdfsBytesRead;
    return this;
  }

  /**
   * HDFS bytes read of this user's MapReduce jobs. Available since v11.
   * @return hdfsBytesRead
  **/
  @ApiModelProperty(value = "HDFS bytes read of this user's MapReduce jobs. Available since v11.")


  public Integer getHdfsBytesRead() {
    return hdfsBytesRead;
  }

  public void setHdfsBytesRead(Integer hdfsBytesRead) {
    this.hdfsBytesRead = hdfsBytesRead;
  }

  public ApiMrUsageReportRow hdfsBytesWritten(Integer hdfsBytesWritten) {
    this.hdfsBytesWritten = hdfsBytesWritten;
    return this;
  }

  /**
   * HDFS bytes written of this user's MapReduce jobs. Available since v11.
   * @return hdfsBytesWritten
  **/
  @ApiModelProperty(value = "HDFS bytes written of this user's MapReduce jobs. Available since v11.")


  public Integer getHdfsBytesWritten() {
    return hdfsBytesWritten;
  }

  public void setHdfsBytesWritten(Integer hdfsBytesWritten) {
    this.hdfsBytesWritten = hdfsBytesWritten;
  }

  public ApiMrUsageReportRow localBytesRead(Integer localBytesRead) {
    this.localBytesRead = localBytesRead;
    return this;
  }

  /**
   * Local bytes read of this user's MapReduce jobs. Available since v11.
   * @return localBytesRead
  **/
  @ApiModelProperty(value = "Local bytes read of this user's MapReduce jobs. Available since v11.")


  public Integer getLocalBytesRead() {
    return localBytesRead;
  }

  public void setLocalBytesRead(Integer localBytesRead) {
    this.localBytesRead = localBytesRead;
  }

  public ApiMrUsageReportRow localBytesWritten(Integer localBytesWritten) {
    this.localBytesWritten = localBytesWritten;
    return this;
  }

  /**
   * Local bytes written of this user's MapReduce jobs. Available since v11.
   * @return localBytesWritten
  **/
  @ApiModelProperty(value = "Local bytes written of this user's MapReduce jobs. Available since v11.")


  public Integer getLocalBytesWritten() {
    return localBytesWritten;
  }

  public void setLocalBytesWritten(Integer localBytesWritten) {
    this.localBytesWritten = localBytesWritten;
  }

  public ApiMrUsageReportRow dataLocalMaps(Integer dataLocalMaps) {
    this.dataLocalMaps = dataLocalMaps;
    return this;
  }

  /**
   * Data local maps of this user's MapReduce jobs. Available since v11.
   * @return dataLocalMaps
  **/
  @ApiModelProperty(value = "Data local maps of this user's MapReduce jobs. Available since v11.")


  public Integer getDataLocalMaps() {
    return dataLocalMaps;
  }

  public void setDataLocalMaps(Integer dataLocalMaps) {
    this.dataLocalMaps = dataLocalMaps;
  }

  public ApiMrUsageReportRow rackLocalMaps(Integer rackLocalMaps) {
    this.rackLocalMaps = rackLocalMaps;
    return this;
  }

  /**
   * Rack local maps of this user's MapReduce jobs. Available since v11.
   * @return rackLocalMaps
  **/
  @ApiModelProperty(value = "Rack local maps of this user's MapReduce jobs. Available since v11.")


  public Integer getRackLocalMaps() {
    return rackLocalMaps;
  }

  public void setRackLocalMaps(Integer rackLocalMaps) {
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

