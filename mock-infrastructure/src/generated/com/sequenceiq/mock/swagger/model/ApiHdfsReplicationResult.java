package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHdfsReplicationCounter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Detailed information about an HDFS replication job.
 */
@ApiModel(description = "Detailed information about an HDFS replication job.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiHdfsReplicationResult   {
  @JsonProperty("progress")
  private BigDecimal progress = null;

  @JsonProperty("throughput")
  private BigDecimal throughput = null;

  @JsonProperty("remainingTime")
  private BigDecimal remainingTime = null;

  @JsonProperty("estimatedCompletionTime")
  private String estimatedCompletionTime = null;

  @JsonProperty("counters")
  @Valid
  private List<ApiHdfsReplicationCounter> counters = null;

  @JsonProperty("numFilesDryRun")
  private BigDecimal numFilesDryRun = null;

  @JsonProperty("numBytesDryRun")
  private BigDecimal numBytesDryRun = null;

  @JsonProperty("numFilesExpected")
  private BigDecimal numFilesExpected = null;

  @JsonProperty("numBytesExpected")
  private BigDecimal numBytesExpected = null;

  @JsonProperty("numFilesCopied")
  private BigDecimal numFilesCopied = null;

  @JsonProperty("numBytesCopied")
  private BigDecimal numBytesCopied = null;

  @JsonProperty("numFilesSkipped")
  private BigDecimal numFilesSkipped = null;

  @JsonProperty("numBytesSkipped")
  private BigDecimal numBytesSkipped = null;

  @JsonProperty("numFilesDeleted")
  private BigDecimal numFilesDeleted = null;

  @JsonProperty("numFilesCopyFailed")
  private BigDecimal numFilesCopyFailed = null;

  @JsonProperty("numBytesCopyFailed")
  private BigDecimal numBytesCopyFailed = null;

  @JsonProperty("setupError")
  private String setupError = null;

  @JsonProperty("jobId")
  private String jobId = null;

  @JsonProperty("jobDetailsUri")
  private String jobDetailsUri = null;

  @JsonProperty("dryRun")
  private Boolean dryRun = null;

  @JsonProperty("snapshottedDirs")
  @Valid
  private List<String> snapshottedDirs = null;

  @JsonProperty("runAsUser")
  private String runAsUser = null;

  @JsonProperty("runOnSourceAsUser")
  private String runOnSourceAsUser = null;

  @JsonProperty("logPath")
  private String logPath = null;

  @JsonProperty("failedFiles")
  @Valid
  private List<String> failedFiles = null;

  public ApiHdfsReplicationResult progress(BigDecimal progress) {
    this.progress = progress;
    return this;
  }

  /**
   * The file copy progress percentage.
   * @return progress
  **/
  @ApiModelProperty(value = "The file copy progress percentage.")

  @Valid

  public BigDecimal getProgress() {
    return progress;
  }

  public void setProgress(BigDecimal progress) {
    this.progress = progress;
  }

  public ApiHdfsReplicationResult throughput(BigDecimal throughput) {
    this.throughput = throughput;
    return this;
  }

  /**
   * The data throughput in KB/s.
   * @return throughput
  **/
  @ApiModelProperty(value = "The data throughput in KB/s.")

  @Valid

  public BigDecimal getThroughput() {
    return throughput;
  }

  public void setThroughput(BigDecimal throughput) {
    this.throughput = throughput;
  }

  public ApiHdfsReplicationResult remainingTime(BigDecimal remainingTime) {
    this.remainingTime = remainingTime;
    return this;
  }

  /**
   * The time remaining for mapper phase (seconds).
   * @return remainingTime
  **/
  @ApiModelProperty(value = "The time remaining for mapper phase (seconds).")

  @Valid

  public BigDecimal getRemainingTime() {
    return remainingTime;
  }

  public void setRemainingTime(BigDecimal remainingTime) {
    this.remainingTime = remainingTime;
  }

  public ApiHdfsReplicationResult estimatedCompletionTime(String estimatedCompletionTime) {
    this.estimatedCompletionTime = estimatedCompletionTime;
    return this;
  }

  /**
   * The estimated completion time for the mapper phase.
   * @return estimatedCompletionTime
  **/
  @ApiModelProperty(value = "The estimated completion time for the mapper phase.")


  public String getEstimatedCompletionTime() {
    return estimatedCompletionTime;
  }

  public void setEstimatedCompletionTime(String estimatedCompletionTime) {
    this.estimatedCompletionTime = estimatedCompletionTime;
  }

  public ApiHdfsReplicationResult counters(List<ApiHdfsReplicationCounter> counters) {
    this.counters = counters;
    return this;
  }

  public ApiHdfsReplicationResult addCountersItem(ApiHdfsReplicationCounter countersItem) {
    if (this.counters == null) {
      this.counters = new ArrayList<>();
    }
    this.counters.add(countersItem);
    return this;
  }

  /**
   * The counters collected from the replication job. <p/> Starting with API v4, the full list of counters is only available in the full view.
   * @return counters
  **/
  @ApiModelProperty(value = "The counters collected from the replication job. <p/> Starting with API v4, the full list of counters is only available in the full view.")

  @Valid

  public List<ApiHdfsReplicationCounter> getCounters() {
    return counters;
  }

  public void setCounters(List<ApiHdfsReplicationCounter> counters) {
    this.counters = counters;
  }

  public ApiHdfsReplicationResult numFilesDryRun(BigDecimal numFilesDryRun) {
    this.numFilesDryRun = numFilesDryRun;
    return this;
  }

  /**
   * The number of files found to copy.
   * @return numFilesDryRun
  **/
  @ApiModelProperty(value = "The number of files found to copy.")

  @Valid

  public BigDecimal getNumFilesDryRun() {
    return numFilesDryRun;
  }

  public void setNumFilesDryRun(BigDecimal numFilesDryRun) {
    this.numFilesDryRun = numFilesDryRun;
  }

  public ApiHdfsReplicationResult numBytesDryRun(BigDecimal numBytesDryRun) {
    this.numBytesDryRun = numBytesDryRun;
    return this;
  }

  /**
   * The number of bytes found to copy.
   * @return numBytesDryRun
  **/
  @ApiModelProperty(value = "The number of bytes found to copy.")

  @Valid

  public BigDecimal getNumBytesDryRun() {
    return numBytesDryRun;
  }

  public void setNumBytesDryRun(BigDecimal numBytesDryRun) {
    this.numBytesDryRun = numBytesDryRun;
  }

  public ApiHdfsReplicationResult numFilesExpected(BigDecimal numFilesExpected) {
    this.numFilesExpected = numFilesExpected;
    return this;
  }

  /**
   * The number of files expected to be copied.
   * @return numFilesExpected
  **/
  @ApiModelProperty(value = "The number of files expected to be copied.")

  @Valid

  public BigDecimal getNumFilesExpected() {
    return numFilesExpected;
  }

  public void setNumFilesExpected(BigDecimal numFilesExpected) {
    this.numFilesExpected = numFilesExpected;
  }

  public ApiHdfsReplicationResult numBytesExpected(BigDecimal numBytesExpected) {
    this.numBytesExpected = numBytesExpected;
    return this;
  }

  /**
   * The number of bytes expected to be copied.
   * @return numBytesExpected
  **/
  @ApiModelProperty(value = "The number of bytes expected to be copied.")

  @Valid

  public BigDecimal getNumBytesExpected() {
    return numBytesExpected;
  }

  public void setNumBytesExpected(BigDecimal numBytesExpected) {
    this.numBytesExpected = numBytesExpected;
  }

  public ApiHdfsReplicationResult numFilesCopied(BigDecimal numFilesCopied) {
    this.numFilesCopied = numFilesCopied;
    return this;
  }

  /**
   * The number of files actually copied.
   * @return numFilesCopied
  **/
  @ApiModelProperty(value = "The number of files actually copied.")

  @Valid

  public BigDecimal getNumFilesCopied() {
    return numFilesCopied;
  }

  public void setNumFilesCopied(BigDecimal numFilesCopied) {
    this.numFilesCopied = numFilesCopied;
  }

  public ApiHdfsReplicationResult numBytesCopied(BigDecimal numBytesCopied) {
    this.numBytesCopied = numBytesCopied;
    return this;
  }

  /**
   * The number of bytes actually copied.
   * @return numBytesCopied
  **/
  @ApiModelProperty(value = "The number of bytes actually copied.")

  @Valid

  public BigDecimal getNumBytesCopied() {
    return numBytesCopied;
  }

  public void setNumBytesCopied(BigDecimal numBytesCopied) {
    this.numBytesCopied = numBytesCopied;
  }

  public ApiHdfsReplicationResult numFilesSkipped(BigDecimal numFilesSkipped) {
    this.numFilesSkipped = numFilesSkipped;
    return this;
  }

  /**
   * The number of files that were unchanged and thus skipped during copying.
   * @return numFilesSkipped
  **/
  @ApiModelProperty(value = "The number of files that were unchanged and thus skipped during copying.")

  @Valid

  public BigDecimal getNumFilesSkipped() {
    return numFilesSkipped;
  }

  public void setNumFilesSkipped(BigDecimal numFilesSkipped) {
    this.numFilesSkipped = numFilesSkipped;
  }

  public ApiHdfsReplicationResult numBytesSkipped(BigDecimal numBytesSkipped) {
    this.numBytesSkipped = numBytesSkipped;
    return this;
  }

  /**
   * The aggregate number of bytes in the skipped files.
   * @return numBytesSkipped
  **/
  @ApiModelProperty(value = "The aggregate number of bytes in the skipped files.")

  @Valid

  public BigDecimal getNumBytesSkipped() {
    return numBytesSkipped;
  }

  public void setNumBytesSkipped(BigDecimal numBytesSkipped) {
    this.numBytesSkipped = numBytesSkipped;
  }

  public ApiHdfsReplicationResult numFilesDeleted(BigDecimal numFilesDeleted) {
    this.numFilesDeleted = numFilesDeleted;
    return this;
  }

  /**
   * The number of files deleted since they were present at destination, but missing from source.
   * @return numFilesDeleted
  **/
  @ApiModelProperty(value = "The number of files deleted since they were present at destination, but missing from source.")

  @Valid

  public BigDecimal getNumFilesDeleted() {
    return numFilesDeleted;
  }

  public void setNumFilesDeleted(BigDecimal numFilesDeleted) {
    this.numFilesDeleted = numFilesDeleted;
  }

  public ApiHdfsReplicationResult numFilesCopyFailed(BigDecimal numFilesCopyFailed) {
    this.numFilesCopyFailed = numFilesCopyFailed;
    return this;
  }

  /**
   * The number of files for which copy failed.
   * @return numFilesCopyFailed
  **/
  @ApiModelProperty(value = "The number of files for which copy failed.")

  @Valid

  public BigDecimal getNumFilesCopyFailed() {
    return numFilesCopyFailed;
  }

  public void setNumFilesCopyFailed(BigDecimal numFilesCopyFailed) {
    this.numFilesCopyFailed = numFilesCopyFailed;
  }

  public ApiHdfsReplicationResult numBytesCopyFailed(BigDecimal numBytesCopyFailed) {
    this.numBytesCopyFailed = numBytesCopyFailed;
    return this;
  }

  /**
   * The aggregate number of bytes in the files for which copy failed.
   * @return numBytesCopyFailed
  **/
  @ApiModelProperty(value = "The aggregate number of bytes in the files for which copy failed.")

  @Valid

  public BigDecimal getNumBytesCopyFailed() {
    return numBytesCopyFailed;
  }

  public void setNumBytesCopyFailed(BigDecimal numBytesCopyFailed) {
    this.numBytesCopyFailed = numBytesCopyFailed;
  }

  public ApiHdfsReplicationResult setupError(String setupError) {
    this.setupError = setupError;
    return this;
  }

  /**
   * The error that happened during job setup, if any.
   * @return setupError
  **/
  @ApiModelProperty(value = "The error that happened during job setup, if any.")


  public String getSetupError() {
    return setupError;
  }

  public void setSetupError(String setupError) {
    this.setupError = setupError;
  }

  public ApiHdfsReplicationResult jobId(String jobId) {
    this.jobId = jobId;
    return this;
  }

  /**
   * Read-only. The MapReduce job ID for the replication job. Available since API v4. <p/> This can be used to query information about the replication job from the MapReduce server where it was executed. Refer to the \"/activities\" resource for services for further details.
   * @return jobId
  **/
  @ApiModelProperty(value = "Read-only. The MapReduce job ID for the replication job. Available since API v4. <p/> This can be used to query information about the replication job from the MapReduce server where it was executed. Refer to the \"/activities\" resource for services for further details.")


  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public ApiHdfsReplicationResult jobDetailsUri(String jobDetailsUri) {
    this.jobDetailsUri = jobDetailsUri;
    return this;
  }

  /**
   * Read-only. The URI (relative to the CM server's root) where to find the Activity Monitor page for the job. Available since API v4.
   * @return jobDetailsUri
  **/
  @ApiModelProperty(value = "Read-only. The URI (relative to the CM server's root) where to find the Activity Monitor page for the job. Available since API v4.")


  public String getJobDetailsUri() {
    return jobDetailsUri;
  }

  public void setJobDetailsUri(String jobDetailsUri) {
    this.jobDetailsUri = jobDetailsUri;
  }

  public ApiHdfsReplicationResult dryRun(Boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  /**
   * Whether this was a dry run.
   * @return dryRun
  **/
  @ApiModelProperty(value = "Whether this was a dry run.")


  public Boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(Boolean dryRun) {
    this.dryRun = dryRun;
  }

  public ApiHdfsReplicationResult snapshottedDirs(List<String> snapshottedDirs) {
    this.snapshottedDirs = snapshottedDirs;
    return this;
  }

  public ApiHdfsReplicationResult addSnapshottedDirsItem(String snapshottedDirsItem) {
    if (this.snapshottedDirs == null) {
      this.snapshottedDirs = new ArrayList<>();
    }
    this.snapshottedDirs.add(snapshottedDirsItem);
    return this;
  }

  /**
   * The list of directories for which snapshots were taken and used as part of this replication.
   * @return snapshottedDirs
  **/
  @ApiModelProperty(example = "\"null\"", value = "The list of directories for which snapshots were taken and used as part of this replication.")


  public List<String> getSnapshottedDirs() {
    return snapshottedDirs;
  }

  public void setSnapshottedDirs(List<String> snapshottedDirs) {
    this.snapshottedDirs = snapshottedDirs;
  }

  public ApiHdfsReplicationResult runAsUser(String runAsUser) {
    this.runAsUser = runAsUser;
    return this;
  }

  /**
   * Returns run-as user name. Available since API v11.
   * @return runAsUser
  **/
  @ApiModelProperty(value = "Returns run-as user name. Available since API v11.")


  public String getRunAsUser() {
    return runAsUser;
  }

  public void setRunAsUser(String runAsUser) {
    this.runAsUser = runAsUser;
  }

  public ApiHdfsReplicationResult runOnSourceAsUser(String runOnSourceAsUser) {
    this.runOnSourceAsUser = runOnSourceAsUser;
    return this;
  }

  /**
   * Returns run-as user name for source cluster. Available since API v18.
   * @return runOnSourceAsUser
  **/
  @ApiModelProperty(value = "Returns run-as user name for source cluster. Available since API v18.")


  public String getRunOnSourceAsUser() {
    return runOnSourceAsUser;
  }

  public void setRunOnSourceAsUser(String runOnSourceAsUser) {
    this.runOnSourceAsUser = runOnSourceAsUser;
  }

  public ApiHdfsReplicationResult logPath(String logPath) {
    this.logPath = logPath;
    return this;
  }

  /**
   * Returns HDFS path of DistCp execution log files. Available since API v33.
   * @return logPath
  **/
  @ApiModelProperty(value = "Returns HDFS path of DistCp execution log files. Available since API v33.")


  public String getLogPath() {
    return logPath;
  }

  public void setLogPath(String logPath) {
    this.logPath = logPath;
  }

  public ApiHdfsReplicationResult failedFiles(List<String> failedFiles) {
    this.failedFiles = failedFiles;
    return this;
  }

  public ApiHdfsReplicationResult addFailedFilesItem(String failedFilesItem) {
    if (this.failedFiles == null) {
      this.failedFiles = new ArrayList<>();
    }
    this.failedFiles.add(failedFilesItem);
    return this;
  }

  /**
   * The list of files that failed during replication. Available since API v11.
   * @return failedFiles
  **/
  @ApiModelProperty(example = "\"null\"", value = "The list of files that failed during replication. Available since API v11.")


  public List<String> getFailedFiles() {
    return failedFiles;
  }

  public void setFailedFiles(List<String> failedFiles) {
    this.failedFiles = failedFiles;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsReplicationResult apiHdfsReplicationResult = (ApiHdfsReplicationResult) o;
    return Objects.equals(this.progress, apiHdfsReplicationResult.progress) &&
        Objects.equals(this.throughput, apiHdfsReplicationResult.throughput) &&
        Objects.equals(this.remainingTime, apiHdfsReplicationResult.remainingTime) &&
        Objects.equals(this.estimatedCompletionTime, apiHdfsReplicationResult.estimatedCompletionTime) &&
        Objects.equals(this.counters, apiHdfsReplicationResult.counters) &&
        Objects.equals(this.numFilesDryRun, apiHdfsReplicationResult.numFilesDryRun) &&
        Objects.equals(this.numBytesDryRun, apiHdfsReplicationResult.numBytesDryRun) &&
        Objects.equals(this.numFilesExpected, apiHdfsReplicationResult.numFilesExpected) &&
        Objects.equals(this.numBytesExpected, apiHdfsReplicationResult.numBytesExpected) &&
        Objects.equals(this.numFilesCopied, apiHdfsReplicationResult.numFilesCopied) &&
        Objects.equals(this.numBytesCopied, apiHdfsReplicationResult.numBytesCopied) &&
        Objects.equals(this.numFilesSkipped, apiHdfsReplicationResult.numFilesSkipped) &&
        Objects.equals(this.numBytesSkipped, apiHdfsReplicationResult.numBytesSkipped) &&
        Objects.equals(this.numFilesDeleted, apiHdfsReplicationResult.numFilesDeleted) &&
        Objects.equals(this.numFilesCopyFailed, apiHdfsReplicationResult.numFilesCopyFailed) &&
        Objects.equals(this.numBytesCopyFailed, apiHdfsReplicationResult.numBytesCopyFailed) &&
        Objects.equals(this.setupError, apiHdfsReplicationResult.setupError) &&
        Objects.equals(this.jobId, apiHdfsReplicationResult.jobId) &&
        Objects.equals(this.jobDetailsUri, apiHdfsReplicationResult.jobDetailsUri) &&
        Objects.equals(this.dryRun, apiHdfsReplicationResult.dryRun) &&
        Objects.equals(this.snapshottedDirs, apiHdfsReplicationResult.snapshottedDirs) &&
        Objects.equals(this.runAsUser, apiHdfsReplicationResult.runAsUser) &&
        Objects.equals(this.runOnSourceAsUser, apiHdfsReplicationResult.runOnSourceAsUser) &&
        Objects.equals(this.logPath, apiHdfsReplicationResult.logPath) &&
        Objects.equals(this.failedFiles, apiHdfsReplicationResult.failedFiles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(progress, throughput, remainingTime, estimatedCompletionTime, counters, numFilesDryRun, numBytesDryRun, numFilesExpected, numBytesExpected, numFilesCopied, numBytesCopied, numFilesSkipped, numBytesSkipped, numFilesDeleted, numFilesCopyFailed, numBytesCopyFailed, setupError, jobId, jobDetailsUri, dryRun, snapshottedDirs, runAsUser, runOnSourceAsUser, logPath, failedFiles);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsReplicationResult {\n");
    
    sb.append("    progress: ").append(toIndentedString(progress)).append("\n");
    sb.append("    throughput: ").append(toIndentedString(throughput)).append("\n");
    sb.append("    remainingTime: ").append(toIndentedString(remainingTime)).append("\n");
    sb.append("    estimatedCompletionTime: ").append(toIndentedString(estimatedCompletionTime)).append("\n");
    sb.append("    counters: ").append(toIndentedString(counters)).append("\n");
    sb.append("    numFilesDryRun: ").append(toIndentedString(numFilesDryRun)).append("\n");
    sb.append("    numBytesDryRun: ").append(toIndentedString(numBytesDryRun)).append("\n");
    sb.append("    numFilesExpected: ").append(toIndentedString(numFilesExpected)).append("\n");
    sb.append("    numBytesExpected: ").append(toIndentedString(numBytesExpected)).append("\n");
    sb.append("    numFilesCopied: ").append(toIndentedString(numFilesCopied)).append("\n");
    sb.append("    numBytesCopied: ").append(toIndentedString(numBytesCopied)).append("\n");
    sb.append("    numFilesSkipped: ").append(toIndentedString(numFilesSkipped)).append("\n");
    sb.append("    numBytesSkipped: ").append(toIndentedString(numBytesSkipped)).append("\n");
    sb.append("    numFilesDeleted: ").append(toIndentedString(numFilesDeleted)).append("\n");
    sb.append("    numFilesCopyFailed: ").append(toIndentedString(numFilesCopyFailed)).append("\n");
    sb.append("    numBytesCopyFailed: ").append(toIndentedString(numBytesCopyFailed)).append("\n");
    sb.append("    setupError: ").append(toIndentedString(setupError)).append("\n");
    sb.append("    jobId: ").append(toIndentedString(jobId)).append("\n");
    sb.append("    jobDetailsUri: ").append(toIndentedString(jobDetailsUri)).append("\n");
    sb.append("    dryRun: ").append(toIndentedString(dryRun)).append("\n");
    sb.append("    snapshottedDirs: ").append(toIndentedString(snapshottedDirs)).append("\n");
    sb.append("    runAsUser: ").append(toIndentedString(runAsUser)).append("\n");
    sb.append("    runOnSourceAsUser: ").append(toIndentedString(runOnSourceAsUser)).append("\n");
    sb.append("    logPath: ").append(toIndentedString(logPath)).append("\n");
    sb.append("    failedFiles: ").append(toIndentedString(failedFiles)).append("\n");
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

