package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import com.sequenceiq.mock.swagger.model.ReplicationStrategy;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
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




public class ApiHdfsReplicationArguments   {
  @JsonProperty("sourceService")
  private ApiServiceRef sourceService = null;

  @JsonProperty("sourcePath")
  private String sourcePath = null;

  @JsonProperty("destinationPath")
  private String destinationPath = null;

  @JsonProperty("mapreduceServiceName")
  private String mapreduceServiceName = null;

  @JsonProperty("schedulerPoolName")
  private String schedulerPoolName = null;

  @JsonProperty("userName")
  private String userName = null;

  @JsonProperty("sourceUser")
  private String sourceUser = null;

  @JsonProperty("numMaps")
  private Integer numMaps = null;

  @JsonProperty("dryRun")
  private Boolean dryRun = null;

  @JsonProperty("bandwidthPerMap")
  private Integer bandwidthPerMap = null;

  @JsonProperty("abortOnError")
  private Boolean abortOnError = null;

  @JsonProperty("removeMissingFiles")
  private Boolean removeMissingFiles = null;

  @JsonProperty("preserveReplicationCount")
  private Boolean preserveReplicationCount = null;

  @JsonProperty("preserveBlockSize")
  private Boolean preserveBlockSize = null;

  @JsonProperty("preservePermissions")
  private Boolean preservePermissions = null;

  @JsonProperty("logPath")
  private String logPath = null;

  @JsonProperty("skipChecksumChecks")
  private Boolean skipChecksumChecks = null;

  @JsonProperty("skipListingChecksumChecks")
  private Boolean skipListingChecksumChecks = null;

  @JsonProperty("skipTrash")
  private Boolean skipTrash = null;

  @JsonProperty("replicationStrategy")
  private ReplicationStrategy replicationStrategy = null;

  @JsonProperty("preserveXAttrs")
  private Boolean preserveXAttrs = null;

  @JsonProperty("exclusionFilters")
  @Valid
  private List<String> exclusionFilters = null;

  @JsonProperty("raiseSnapshotDiffFailures")
  private Boolean raiseSnapshotDiffFailures = null;

  @JsonProperty("destinationCloudAccount")
  private String destinationCloudAccount = null;

  public ApiHdfsReplicationArguments sourceService(ApiServiceRef sourceService) {
    this.sourceService = sourceService;
    return this;
  }

  /**
   * The service to replicate from.
   * @return sourceService
  **/
  @ApiModelProperty(value = "The service to replicate from.")

  @Valid

  public ApiServiceRef getSourceService() {
    return sourceService;
  }

  public void setSourceService(ApiServiceRef sourceService) {
    this.sourceService = sourceService;
  }

  public ApiHdfsReplicationArguments sourcePath(String sourcePath) {
    this.sourcePath = sourcePath;
    return this;
  }

  /**
   * The path to replicate.
   * @return sourcePath
  **/
  @ApiModelProperty(value = "The path to replicate.")


  public String getSourcePath() {
    return sourcePath;
  }

  public void setSourcePath(String sourcePath) {
    this.sourcePath = sourcePath;
  }

  public ApiHdfsReplicationArguments destinationPath(String destinationPath) {
    this.destinationPath = destinationPath;
    return this;
  }

  /**
   * The destination to replicate to.
   * @return destinationPath
  **/
  @ApiModelProperty(value = "The destination to replicate to.")


  public String getDestinationPath() {
    return destinationPath;
  }

  public void setDestinationPath(String destinationPath) {
    this.destinationPath = destinationPath;
  }

  public ApiHdfsReplicationArguments mapreduceServiceName(String mapreduceServiceName) {
    this.mapreduceServiceName = mapreduceServiceName;
    return this;
  }

  /**
   * The mapreduce service to use for the replication job.
   * @return mapreduceServiceName
  **/
  @ApiModelProperty(value = "The mapreduce service to use for the replication job.")


  public String getMapreduceServiceName() {
    return mapreduceServiceName;
  }

  public void setMapreduceServiceName(String mapreduceServiceName) {
    this.mapreduceServiceName = mapreduceServiceName;
  }

  public ApiHdfsReplicationArguments schedulerPoolName(String schedulerPoolName) {
    this.schedulerPoolName = schedulerPoolName;
    return this;
  }

  /**
   * Name of the scheduler pool to use when submitting the MapReduce job. Currently supports the capacity and fair schedulers. The option is ignored if a different scheduler is configured.
   * @return schedulerPoolName
  **/
  @ApiModelProperty(value = "Name of the scheduler pool to use when submitting the MapReduce job. Currently supports the capacity and fair schedulers. The option is ignored if a different scheduler is configured.")


  public String getSchedulerPoolName() {
    return schedulerPoolName;
  }

  public void setSchedulerPoolName(String schedulerPoolName) {
    this.schedulerPoolName = schedulerPoolName;
  }

  public ApiHdfsReplicationArguments userName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * The user which will execute the MapReduce job. Required if running with Kerberos enabled.
   * @return userName
  **/
  @ApiModelProperty(value = "The user which will execute the MapReduce job. Required if running with Kerberos enabled.")


  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public ApiHdfsReplicationArguments sourceUser(String sourceUser) {
    this.sourceUser = sourceUser;
    return this;
  }

  /**
   * The user which will perform operations on source cluster. Required if running with Kerberos enabled.
   * @return sourceUser
  **/
  @ApiModelProperty(value = "The user which will perform operations on source cluster. Required if running with Kerberos enabled.")


  public String getSourceUser() {
    return sourceUser;
  }

  public void setSourceUser(String sourceUser) {
    this.sourceUser = sourceUser;
  }

  public ApiHdfsReplicationArguments numMaps(Integer numMaps) {
    this.numMaps = numMaps;
    return this;
  }

  /**
   * The number of mappers to use for the mapreduce replication job.
   * @return numMaps
  **/
  @ApiModelProperty(value = "The number of mappers to use for the mapreduce replication job.")


  public Integer getNumMaps() {
    return numMaps;
  }

  public void setNumMaps(Integer numMaps) {
    this.numMaps = numMaps;
  }

  public ApiHdfsReplicationArguments dryRun(Boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  /**
   * Whether to perform a dry run. Defaults to false.
   * @return dryRun
  **/
  @ApiModelProperty(value = "Whether to perform a dry run. Defaults to false.")


  public Boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(Boolean dryRun) {
    this.dryRun = dryRun;
  }

  public ApiHdfsReplicationArguments bandwidthPerMap(Integer bandwidthPerMap) {
    this.bandwidthPerMap = bandwidthPerMap;
    return this;
  }

  /**
   * The maximum bandwidth (in MB) per mapper in the mapreduce replication job.
   * @return bandwidthPerMap
  **/
  @ApiModelProperty(value = "The maximum bandwidth (in MB) per mapper in the mapreduce replication job.")


  public Integer getBandwidthPerMap() {
    return bandwidthPerMap;
  }

  public void setBandwidthPerMap(Integer bandwidthPerMap) {
    this.bandwidthPerMap = bandwidthPerMap;
  }

  public ApiHdfsReplicationArguments abortOnError(Boolean abortOnError) {
    this.abortOnError = abortOnError;
    return this;
  }

  /**
   * Whether to abort on a replication failure. Defaults to false.
   * @return abortOnError
  **/
  @ApiModelProperty(value = "Whether to abort on a replication failure. Defaults to false.")


  public Boolean isAbortOnError() {
    return abortOnError;
  }

  public void setAbortOnError(Boolean abortOnError) {
    this.abortOnError = abortOnError;
  }

  public ApiHdfsReplicationArguments removeMissingFiles(Boolean removeMissingFiles) {
    this.removeMissingFiles = removeMissingFiles;
    return this;
  }

  /**
   * Whether to delete destination files that are missing in source. Defaults to false.
   * @return removeMissingFiles
  **/
  @ApiModelProperty(value = "Whether to delete destination files that are missing in source. Defaults to false.")


  public Boolean isRemoveMissingFiles() {
    return removeMissingFiles;
  }

  public void setRemoveMissingFiles(Boolean removeMissingFiles) {
    this.removeMissingFiles = removeMissingFiles;
  }

  public ApiHdfsReplicationArguments preserveReplicationCount(Boolean preserveReplicationCount) {
    this.preserveReplicationCount = preserveReplicationCount;
    return this;
  }

  /**
   * Whether to preserve the HDFS replication count. Defaults to false.
   * @return preserveReplicationCount
  **/
  @ApiModelProperty(value = "Whether to preserve the HDFS replication count. Defaults to false.")


  public Boolean isPreserveReplicationCount() {
    return preserveReplicationCount;
  }

  public void setPreserveReplicationCount(Boolean preserveReplicationCount) {
    this.preserveReplicationCount = preserveReplicationCount;
  }

  public ApiHdfsReplicationArguments preserveBlockSize(Boolean preserveBlockSize) {
    this.preserveBlockSize = preserveBlockSize;
    return this;
  }

  /**
   * Whether to preserve the HDFS block size. Defaults to false.
   * @return preserveBlockSize
  **/
  @ApiModelProperty(value = "Whether to preserve the HDFS block size. Defaults to false.")


  public Boolean isPreserveBlockSize() {
    return preserveBlockSize;
  }

  public void setPreserveBlockSize(Boolean preserveBlockSize) {
    this.preserveBlockSize = preserveBlockSize;
  }

  public ApiHdfsReplicationArguments preservePermissions(Boolean preservePermissions) {
    this.preservePermissions = preservePermissions;
    return this;
  }

  /**
   * Whether to preserve the HDFS owner, group and permissions. Defaults to false. Starting from V10, it also preserves ACLs. Defaults to null (no preserve). ACLs is preserved if both clusters enable ACL support, and replication ignores any ACL related failures.
   * @return preservePermissions
  **/
  @ApiModelProperty(value = "Whether to preserve the HDFS owner, group and permissions. Defaults to false. Starting from V10, it also preserves ACLs. Defaults to null (no preserve). ACLs is preserved if both clusters enable ACL support, and replication ignores any ACL related failures.")


  public Boolean isPreservePermissions() {
    return preservePermissions;
  }

  public void setPreservePermissions(Boolean preservePermissions) {
    this.preservePermissions = preservePermissions;
  }

  public ApiHdfsReplicationArguments logPath(String logPath) {
    this.logPath = logPath;
    return this;
  }

  /**
   * The HDFS path where the replication log files should be written to.
   * @return logPath
  **/
  @ApiModelProperty(value = "The HDFS path where the replication log files should be written to.")


  public String getLogPath() {
    return logPath;
  }

  public void setLogPath(String logPath) {
    this.logPath = logPath;
  }

  public ApiHdfsReplicationArguments skipChecksumChecks(Boolean skipChecksumChecks) {
    this.skipChecksumChecks = skipChecksumChecks;
    return this;
  }

  /**
   * Whether to skip checksum based file validation during replication. Defaults to false.
   * @return skipChecksumChecks
  **/
  @ApiModelProperty(value = "Whether to skip checksum based file validation during replication. Defaults to false.")


  public Boolean isSkipChecksumChecks() {
    return skipChecksumChecks;
  }

  public void setSkipChecksumChecks(Boolean skipChecksumChecks) {
    this.skipChecksumChecks = skipChecksumChecks;
  }

  public ApiHdfsReplicationArguments skipListingChecksumChecks(Boolean skipListingChecksumChecks) {
    this.skipListingChecksumChecks = skipListingChecksumChecks;
    return this;
  }

  /**
   * Whether to skip checksum based file comparison during replication. Defaults to false.
   * @return skipListingChecksumChecks
  **/
  @ApiModelProperty(value = "Whether to skip checksum based file comparison during replication. Defaults to false.")


  public Boolean isSkipListingChecksumChecks() {
    return skipListingChecksumChecks;
  }

  public void setSkipListingChecksumChecks(Boolean skipListingChecksumChecks) {
    this.skipListingChecksumChecks = skipListingChecksumChecks;
  }

  public ApiHdfsReplicationArguments skipTrash(Boolean skipTrash) {
    this.skipTrash = skipTrash;
    return this;
  }

  /**
   * Whether to permanently delete destination files that are missing in source. Defaults to null.
   * @return skipTrash
  **/
  @ApiModelProperty(value = "Whether to permanently delete destination files that are missing in source. Defaults to null.")


  public Boolean isSkipTrash() {
    return skipTrash;
  }

  public void setSkipTrash(Boolean skipTrash) {
    this.skipTrash = skipTrash;
  }

  public ApiHdfsReplicationArguments replicationStrategy(ReplicationStrategy replicationStrategy) {
    this.replicationStrategy = replicationStrategy;
    return this;
  }

  /**
   * The strategy for distributing the file replication tasks among the mappers of the MR job associated with a replication. Default is ReplicationStrategy#STATIC.
   * @return replicationStrategy
  **/
  @ApiModelProperty(value = "The strategy for distributing the file replication tasks among the mappers of the MR job associated with a replication. Default is ReplicationStrategy#STATIC.")

  @Valid

  public ReplicationStrategy getReplicationStrategy() {
    return replicationStrategy;
  }

  public void setReplicationStrategy(ReplicationStrategy replicationStrategy) {
    this.replicationStrategy = replicationStrategy;
  }

  public ApiHdfsReplicationArguments preserveXAttrs(Boolean preserveXAttrs) {
    this.preserveXAttrs = preserveXAttrs;
    return this;
  }

  /**
   * Whether to preserve XAttrs, default to false This is introduced in V10. To preserve XAttrs, both CDH versions should be >= 5.2. Replication fails if either cluster does not support XAttrs.
   * @return preserveXAttrs
  **/
  @ApiModelProperty(value = "Whether to preserve XAttrs, default to false This is introduced in V10. To preserve XAttrs, both CDH versions should be >= 5.2. Replication fails if either cluster does not support XAttrs.")


  public Boolean isPreserveXAttrs() {
    return preserveXAttrs;
  }

  public void setPreserveXAttrs(Boolean preserveXAttrs) {
    this.preserveXAttrs = preserveXAttrs;
  }

  public ApiHdfsReplicationArguments exclusionFilters(List<String> exclusionFilters) {
    this.exclusionFilters = exclusionFilters;
    return this;
  }

  public ApiHdfsReplicationArguments addExclusionFiltersItem(String exclusionFiltersItem) {
    if (this.exclusionFilters == null) {
      this.exclusionFilters = new ArrayList<>();
    }
    this.exclusionFilters.add(exclusionFiltersItem);
    return this;
  }

  /**
   * Specify regular expression strings to match full paths of files and directories matching source paths and exclude them from the replication. Optional. Available since V11.
   * @return exclusionFilters
  **/
  @ApiModelProperty(value = "Specify regular expression strings to match full paths of files and directories matching source paths and exclude them from the replication. Optional. Available since V11.")


  public List<String> getExclusionFilters() {
    return exclusionFilters;
  }

  public void setExclusionFilters(List<String> exclusionFilters) {
    this.exclusionFilters = exclusionFilters;
  }

  public ApiHdfsReplicationArguments raiseSnapshotDiffFailures(Boolean raiseSnapshotDiffFailures) {
    this.raiseSnapshotDiffFailures = raiseSnapshotDiffFailures;
    return this;
  }

  /**
   * Flag indicating if failures during snapshotDiff should be ignored or not. When it is set to false then, replication will fallback to full copy listing in case of any error in snapshot diff handling and it will ignore snapshot delete/rename failures at the end of a replication. The flag is by default set to false in distcp tool which means it will ignore snapshot diff failures and mark replication as success for snapshot delete/rename failures. In UI, the flag is set to true by default when source CM Version is greater than 5.14.
   * @return raiseSnapshotDiffFailures
  **/
  @ApiModelProperty(value = "Flag indicating if failures during snapshotDiff should be ignored or not. When it is set to false then, replication will fallback to full copy listing in case of any error in snapshot diff handling and it will ignore snapshot delete/rename failures at the end of a replication. The flag is by default set to false in distcp tool which means it will ignore snapshot diff failures and mark replication as success for snapshot delete/rename failures. In UI, the flag is set to true by default when source CM Version is greater than 5.14.")


  public Boolean isRaiseSnapshotDiffFailures() {
    return raiseSnapshotDiffFailures;
  }

  public void setRaiseSnapshotDiffFailures(Boolean raiseSnapshotDiffFailures) {
    this.raiseSnapshotDiffFailures = raiseSnapshotDiffFailures;
  }

  public ApiHdfsReplicationArguments destinationCloudAccount(String destinationCloudAccount) {
    this.destinationCloudAccount = destinationCloudAccount;
    return this;
  }

  /**
   * The cloud account name which is used in direct hive cloud replication, if specified.
   * @return destinationCloudAccount
  **/
  @ApiModelProperty(value = "The cloud account name which is used in direct hive cloud replication, if specified.")


  public String getDestinationCloudAccount() {
    return destinationCloudAccount;
  }

  public void setDestinationCloudAccount(String destinationCloudAccount) {
    this.destinationCloudAccount = destinationCloudAccount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsReplicationArguments apiHdfsReplicationArguments = (ApiHdfsReplicationArguments) o;
    return Objects.equals(this.sourceService, apiHdfsReplicationArguments.sourceService) &&
        Objects.equals(this.sourcePath, apiHdfsReplicationArguments.sourcePath) &&
        Objects.equals(this.destinationPath, apiHdfsReplicationArguments.destinationPath) &&
        Objects.equals(this.mapreduceServiceName, apiHdfsReplicationArguments.mapreduceServiceName) &&
        Objects.equals(this.schedulerPoolName, apiHdfsReplicationArguments.schedulerPoolName) &&
        Objects.equals(this.userName, apiHdfsReplicationArguments.userName) &&
        Objects.equals(this.sourceUser, apiHdfsReplicationArguments.sourceUser) &&
        Objects.equals(this.numMaps, apiHdfsReplicationArguments.numMaps) &&
        Objects.equals(this.dryRun, apiHdfsReplicationArguments.dryRun) &&
        Objects.equals(this.bandwidthPerMap, apiHdfsReplicationArguments.bandwidthPerMap) &&
        Objects.equals(this.abortOnError, apiHdfsReplicationArguments.abortOnError) &&
        Objects.equals(this.removeMissingFiles, apiHdfsReplicationArguments.removeMissingFiles) &&
        Objects.equals(this.preserveReplicationCount, apiHdfsReplicationArguments.preserveReplicationCount) &&
        Objects.equals(this.preserveBlockSize, apiHdfsReplicationArguments.preserveBlockSize) &&
        Objects.equals(this.preservePermissions, apiHdfsReplicationArguments.preservePermissions) &&
        Objects.equals(this.logPath, apiHdfsReplicationArguments.logPath) &&
        Objects.equals(this.skipChecksumChecks, apiHdfsReplicationArguments.skipChecksumChecks) &&
        Objects.equals(this.skipListingChecksumChecks, apiHdfsReplicationArguments.skipListingChecksumChecks) &&
        Objects.equals(this.skipTrash, apiHdfsReplicationArguments.skipTrash) &&
        Objects.equals(this.replicationStrategy, apiHdfsReplicationArguments.replicationStrategy) &&
        Objects.equals(this.preserveXAttrs, apiHdfsReplicationArguments.preserveXAttrs) &&
        Objects.equals(this.exclusionFilters, apiHdfsReplicationArguments.exclusionFilters) &&
        Objects.equals(this.raiseSnapshotDiffFailures, apiHdfsReplicationArguments.raiseSnapshotDiffFailures) &&
        Objects.equals(this.destinationCloudAccount, apiHdfsReplicationArguments.destinationCloudAccount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceService, sourcePath, destinationPath, mapreduceServiceName, schedulerPoolName, userName, sourceUser, numMaps, dryRun, bandwidthPerMap, abortOnError, removeMissingFiles, preserveReplicationCount, preserveBlockSize, preservePermissions, logPath, skipChecksumChecks, skipListingChecksumChecks, skipTrash, replicationStrategy, preserveXAttrs, exclusionFilters, raiseSnapshotDiffFailures, destinationCloudAccount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsReplicationArguments {\n");
    
    sb.append("    sourceService: ").append(toIndentedString(sourceService)).append("\n");
    sb.append("    sourcePath: ").append(toIndentedString(sourcePath)).append("\n");
    sb.append("    destinationPath: ").append(toIndentedString(destinationPath)).append("\n");
    sb.append("    mapreduceServiceName: ").append(toIndentedString(mapreduceServiceName)).append("\n");
    sb.append("    schedulerPoolName: ").append(toIndentedString(schedulerPoolName)).append("\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("    sourceUser: ").append(toIndentedString(sourceUser)).append("\n");
    sb.append("    numMaps: ").append(toIndentedString(numMaps)).append("\n");
    sb.append("    dryRun: ").append(toIndentedString(dryRun)).append("\n");
    sb.append("    bandwidthPerMap: ").append(toIndentedString(bandwidthPerMap)).append("\n");
    sb.append("    abortOnError: ").append(toIndentedString(abortOnError)).append("\n");
    sb.append("    removeMissingFiles: ").append(toIndentedString(removeMissingFiles)).append("\n");
    sb.append("    preserveReplicationCount: ").append(toIndentedString(preserveReplicationCount)).append("\n");
    sb.append("    preserveBlockSize: ").append(toIndentedString(preserveBlockSize)).append("\n");
    sb.append("    preservePermissions: ").append(toIndentedString(preservePermissions)).append("\n");
    sb.append("    logPath: ").append(toIndentedString(logPath)).append("\n");
    sb.append("    skipChecksumChecks: ").append(toIndentedString(skipChecksumChecks)).append("\n");
    sb.append("    skipListingChecksumChecks: ").append(toIndentedString(skipListingChecksumChecks)).append("\n");
    sb.append("    skipTrash: ").append(toIndentedString(skipTrash)).append("\n");
    sb.append("    replicationStrategy: ").append(toIndentedString(replicationStrategy)).append("\n");
    sb.append("    preserveXAttrs: ").append(toIndentedString(preserveXAttrs)).append("\n");
    sb.append("    exclusionFilters: ").append(toIndentedString(exclusionFilters)).append("\n");
    sb.append("    raiseSnapshotDiffFailures: ").append(toIndentedString(raiseSnapshotDiffFailures)).append("\n");
    sb.append("    destinationCloudAccount: ").append(toIndentedString(destinationCloudAccount)).append("\n");
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

