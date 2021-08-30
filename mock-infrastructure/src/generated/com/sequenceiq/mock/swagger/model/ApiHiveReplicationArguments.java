package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHdfsReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiHiveTable;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Replication arguments for Hive services.
 */
@ApiModel(description = "Replication arguments for Hive services.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHiveReplicationArguments   {
  @JsonProperty("sourceService")
  private ApiServiceRef sourceService = null;

  @JsonProperty("tableFilters")
  @Valid
  private List<ApiHiveTable> tableFilters = null;

  @JsonProperty("exportDir")
  private String exportDir = null;

  @JsonProperty("force")
  private Boolean force = null;

  @JsonProperty("replicateData")
  private Boolean replicateData = null;

  @JsonProperty("hdfsArguments")
  private ApiHdfsReplicationArguments hdfsArguments = null;

  @JsonProperty("replicateImpalaMetadata")
  private Boolean replicateImpalaMetadata = null;

  @JsonProperty("runInvalidateMetadata")
  private Boolean runInvalidateMetadata = null;

  @JsonProperty("dryRun")
  private Boolean dryRun = null;

  @JsonProperty("numThreads")
  private Integer numThreads = null;

  @JsonProperty("sentryMigration")
  private Boolean sentryMigration = null;

  @JsonProperty("skipUrlPermissions")
  private Boolean skipUrlPermissions = null;

  public ApiHiveReplicationArguments sourceService(ApiServiceRef sourceService) {
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

  public ApiHiveReplicationArguments tableFilters(List<ApiHiveTable> tableFilters) {
    this.tableFilters = tableFilters;
    return this;
  }

  public ApiHiveReplicationArguments addTableFiltersItem(ApiHiveTable tableFiltersItem) {
    if (this.tableFilters == null) {
      this.tableFilters = new ArrayList<>();
    }
    this.tableFilters.add(tableFiltersItem);
    return this;
  }

  /**
   * Filters for tables to include in the replication. Optional. If not provided, include all tables in all databases.
   * @return tableFilters
  **/
  @ApiModelProperty(value = "Filters for tables to include in the replication. Optional. If not provided, include all tables in all databases.")

  @Valid

  public List<ApiHiveTable> getTableFilters() {
    return tableFilters;
  }

  public void setTableFilters(List<ApiHiveTable> tableFilters) {
    this.tableFilters = tableFilters;
  }

  public ApiHiveReplicationArguments exportDir(String exportDir) {
    this.exportDir = exportDir;
    return this;
  }

  /**
   * Directory, in the HDFS service where the target Hive service's data is stored, where the export file will be saved. Optional. If not provided, Cloudera Manager will pick a directory for storing the data.
   * @return exportDir
  **/
  @ApiModelProperty(value = "Directory, in the HDFS service where the target Hive service's data is stored, where the export file will be saved. Optional. If not provided, Cloudera Manager will pick a directory for storing the data.")


  public String getExportDir() {
    return exportDir;
  }

  public void setExportDir(String exportDir) {
    this.exportDir = exportDir;
  }

  public ApiHiveReplicationArguments force(Boolean force) {
    this.force = force;
    return this;
  }

  /**
   * Whether to force overwriting of mismatched tables. Defaults to false.
   * @return force
  **/
  @ApiModelProperty(value = "Whether to force overwriting of mismatched tables. Defaults to false.")


  public Boolean isForce() {
    return force;
  }

  public void setForce(Boolean force) {
    this.force = force;
  }

  public ApiHiveReplicationArguments replicateData(Boolean replicateData) {
    this.replicateData = replicateData;
    return this;
  }

  /**
   * Whether to replicate table data stored in HDFS. Defaults to false. <p/> If set, the \"hdfsArguments\" property must be set to configure the HDFS replication job.
   * @return replicateData
  **/
  @ApiModelProperty(value = "Whether to replicate table data stored in HDFS. Defaults to false. <p/> If set, the \"hdfsArguments\" property must be set to configure the HDFS replication job.")


  public Boolean isReplicateData() {
    return replicateData;
  }

  public void setReplicateData(Boolean replicateData) {
    this.replicateData = replicateData;
  }

  public ApiHiveReplicationArguments hdfsArguments(ApiHdfsReplicationArguments hdfsArguments) {
    this.hdfsArguments = hdfsArguments;
    return this;
  }

  /**
   * Arguments for the HDFS replication job. <p/> This must be provided when choosing to replicate table data stored in HDFS. The \"sourceService\", \"sourcePath\" and \"dryRun\" properties of the HDFS arguments are ignored; their values are derived from the Hive replication's information. <p/> The \"destinationPath\" property is used slightly differently from the usual HDFS replication jobs. It is used to map the root path of the source service into the target service. It may be omitted, in which case the source and target paths will match. <p/> Example: if the destination path is set to \"/new_root\", a \"/foo/bar\" path in the source will be stored in \"/new_root/foo/bar\" in the target.
   * @return hdfsArguments
  **/
  @ApiModelProperty(value = "Arguments for the HDFS replication job. <p/> This must be provided when choosing to replicate table data stored in HDFS. The \"sourceService\", \"sourcePath\" and \"dryRun\" properties of the HDFS arguments are ignored; their values are derived from the Hive replication's information. <p/> The \"destinationPath\" property is used slightly differently from the usual HDFS replication jobs. It is used to map the root path of the source service into the target service. It may be omitted, in which case the source and target paths will match. <p/> Example: if the destination path is set to \"/new_root\", a \"/foo/bar\" path in the source will be stored in \"/new_root/foo/bar\" in the target.")

  @Valid

  public ApiHdfsReplicationArguments getHdfsArguments() {
    return hdfsArguments;
  }

  public void setHdfsArguments(ApiHdfsReplicationArguments hdfsArguments) {
    this.hdfsArguments = hdfsArguments;
  }

  public ApiHiveReplicationArguments replicateImpalaMetadata(Boolean replicateImpalaMetadata) {
    this.replicateImpalaMetadata = replicateImpalaMetadata;
    return this;
  }

  /**
   * Whether to replicate the impala metadata. (i.e. the metadata for impala UDFs and their corresponding binaries in HDFS).
   * @return replicateImpalaMetadata
  **/
  @ApiModelProperty(value = "Whether to replicate the impala metadata. (i.e. the metadata for impala UDFs and their corresponding binaries in HDFS).")


  public Boolean isReplicateImpalaMetadata() {
    return replicateImpalaMetadata;
  }

  public void setReplicateImpalaMetadata(Boolean replicateImpalaMetadata) {
    this.replicateImpalaMetadata = replicateImpalaMetadata;
  }

  public ApiHiveReplicationArguments runInvalidateMetadata(Boolean runInvalidateMetadata) {
    this.runInvalidateMetadata = runInvalidateMetadata;
    return this;
  }

  /**
   * Whether to run invalidate metadata query or not
   * @return runInvalidateMetadata
  **/
  @ApiModelProperty(value = "Whether to run invalidate metadata query or not")


  public Boolean isRunInvalidateMetadata() {
    return runInvalidateMetadata;
  }

  public void setRunInvalidateMetadata(Boolean runInvalidateMetadata) {
    this.runInvalidateMetadata = runInvalidateMetadata;
  }

  public ApiHiveReplicationArguments dryRun(Boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  /**
   * Whether to perform a dry run. Defaults to false
   * @return dryRun
  **/
  @ApiModelProperty(value = "Whether to perform a dry run. Defaults to false")


  public Boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(Boolean dryRun) {
    this.dryRun = dryRun;
  }

  public ApiHiveReplicationArguments numThreads(Integer numThreads) {
    this.numThreads = numThreads;
    return this;
  }

  /**
   * Number of threads to use in multi-threaded export/import phase
   * @return numThreads
  **/
  @ApiModelProperty(value = "Number of threads to use in multi-threaded export/import phase")


  public Integer getNumThreads() {
    return numThreads;
  }

  public void setNumThreads(Integer numThreads) {
    this.numThreads = numThreads;
  }

  public ApiHiveReplicationArguments sentryMigration(Boolean sentryMigration) {
    this.sentryMigration = sentryMigration;
    return this;
  }

  /**
   * 
   * @return sentryMigration
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean isSentryMigration() {
    return sentryMigration;
  }

  public void setSentryMigration(Boolean sentryMigration) {
    this.sentryMigration = sentryMigration;
  }

  public ApiHiveReplicationArguments skipUrlPermissions(Boolean skipUrlPermissions) {
    this.skipUrlPermissions = skipUrlPermissions;
    return this;
  }

  /**
   * Is skipUrlPermissions on.
   * @return skipUrlPermissions
  **/
  @ApiModelProperty(required = true, value = "Is skipUrlPermissions on.")
  @NotNull


  public Boolean isSkipUrlPermissions() {
    return skipUrlPermissions;
  }

  public void setSkipUrlPermissions(Boolean skipUrlPermissions) {
    this.skipUrlPermissions = skipUrlPermissions;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHiveReplicationArguments apiHiveReplicationArguments = (ApiHiveReplicationArguments) o;
    return Objects.equals(this.sourceService, apiHiveReplicationArguments.sourceService) &&
        Objects.equals(this.tableFilters, apiHiveReplicationArguments.tableFilters) &&
        Objects.equals(this.exportDir, apiHiveReplicationArguments.exportDir) &&
        Objects.equals(this.force, apiHiveReplicationArguments.force) &&
        Objects.equals(this.replicateData, apiHiveReplicationArguments.replicateData) &&
        Objects.equals(this.hdfsArguments, apiHiveReplicationArguments.hdfsArguments) &&
        Objects.equals(this.replicateImpalaMetadata, apiHiveReplicationArguments.replicateImpalaMetadata) &&
        Objects.equals(this.runInvalidateMetadata, apiHiveReplicationArguments.runInvalidateMetadata) &&
        Objects.equals(this.dryRun, apiHiveReplicationArguments.dryRun) &&
        Objects.equals(this.numThreads, apiHiveReplicationArguments.numThreads) &&
        Objects.equals(this.sentryMigration, apiHiveReplicationArguments.sentryMigration) &&
        Objects.equals(this.skipUrlPermissions, apiHiveReplicationArguments.skipUrlPermissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceService, tableFilters, exportDir, force, replicateData, hdfsArguments, replicateImpalaMetadata, runInvalidateMetadata, dryRun, numThreads, sentryMigration, skipUrlPermissions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHiveReplicationArguments {\n");
    
    sb.append("    sourceService: ").append(toIndentedString(sourceService)).append("\n");
    sb.append("    tableFilters: ").append(toIndentedString(tableFilters)).append("\n");
    sb.append("    exportDir: ").append(toIndentedString(exportDir)).append("\n");
    sb.append("    force: ").append(toIndentedString(force)).append("\n");
    sb.append("    replicateData: ").append(toIndentedString(replicateData)).append("\n");
    sb.append("    hdfsArguments: ").append(toIndentedString(hdfsArguments)).append("\n");
    sb.append("    replicateImpalaMetadata: ").append(toIndentedString(replicateImpalaMetadata)).append("\n");
    sb.append("    runInvalidateMetadata: ").append(toIndentedString(runInvalidateMetadata)).append("\n");
    sb.append("    dryRun: ").append(toIndentedString(dryRun)).append("\n");
    sb.append("    numThreads: ").append(toIndentedString(numThreads)).append("\n");
    sb.append("    sentryMigration: ").append(toIndentedString(sentryMigration)).append("\n");
    sb.append("    skipUrlPermissions: ").append(toIndentedString(skipUrlPermissions)).append("\n");
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

