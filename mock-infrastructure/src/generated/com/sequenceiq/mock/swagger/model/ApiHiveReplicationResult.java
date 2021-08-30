package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHdfsReplicationResult;
import com.sequenceiq.mock.swagger.model.ApiHiveReplicationError;
import com.sequenceiq.mock.swagger.model.ApiHiveTable;
import com.sequenceiq.mock.swagger.model.ApiHiveUDF;
import com.sequenceiq.mock.swagger.model.ApiImpalaUDF;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Detailed information about a Hive replication job.
 */
@ApiModel(description = "Detailed information about a Hive replication job.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHiveReplicationResult   {
  @JsonProperty("phase")
  private String phase = null;

  @JsonProperty("tableCount")
  private Integer tableCount = null;

  @JsonProperty("tables")
  @Valid
  private List<ApiHiveTable> tables = null;

  @JsonProperty("impalaUDFCount")
  private Integer impalaUDFCount = null;

  @JsonProperty("hiveUDFCount")
  private Integer hiveUDFCount = null;

  @JsonProperty("impalaUDFs")
  @Valid
  private List<ApiImpalaUDF> impalaUDFs = null;

  @JsonProperty("hiveUDFs")
  @Valid
  private List<ApiHiveUDF> hiveUDFs = null;

  @JsonProperty("errorCount")
  private Integer errorCount = null;

  @JsonProperty("errors")
  @Valid
  private List<ApiHiveReplicationError> errors = null;

  @JsonProperty("dataReplicationResult")
  private ApiHdfsReplicationResult dataReplicationResult = null;

  @JsonProperty("dryRun")
  private Boolean dryRun = null;

  @JsonProperty("runAsUser")
  private String runAsUser = null;

  @JsonProperty("runOnSourceAsUser")
  private String runOnSourceAsUser = null;

  @JsonProperty("logPath")
  private String logPath = null;

  @JsonProperty("directoryForMetadata")
  private String directoryForMetadata = null;

  @JsonProperty("statsAvailable")
  private Boolean statsAvailable = null;

  @JsonProperty("dbProcessed")
  private Integer dbProcessed = null;

  @JsonProperty("tableProcessed")
  private Integer tableProcessed = null;

  @JsonProperty("partitionProcessed")
  private Integer partitionProcessed = null;

  @JsonProperty("functionProcessed")
  private Integer functionProcessed = null;

  @JsonProperty("indexProcessed")
  private Integer indexProcessed = null;

  @JsonProperty("statsProcessed")
  private Integer statsProcessed = null;

  @JsonProperty("dbExpected")
  private Integer dbExpected = null;

  @JsonProperty("tableExpected")
  private Integer tableExpected = null;

  @JsonProperty("partitionExpected")
  private Integer partitionExpected = null;

  @JsonProperty("functionExpected")
  private Integer functionExpected = null;

  @JsonProperty("indexExpected")
  private Integer indexExpected = null;

  @JsonProperty("statsExpected")
  private Integer statsExpected = null;

  public ApiHiveReplicationResult phase(String phase) {
    this.phase = phase;
    return this;
  }

  /**
   * Phase the replication is in. <p/> If the replication job is still active, this will contain a string describing the current phase. This will be one of: EXPORT, DATA or IMPORT, for, respectively, exporting the source metastore information, replicating table data (if configured), and importing metastore information in the target. <p/> This value will not be present if the replication is not active. <p/> Available since API v4.
   * @return phase
  **/
  @ApiModelProperty(value = "Phase the replication is in. <p/> If the replication job is still active, this will contain a string describing the current phase. This will be one of: EXPORT, DATA or IMPORT, for, respectively, exporting the source metastore information, replicating table data (if configured), and importing metastore information in the target. <p/> This value will not be present if the replication is not active. <p/> Available since API v4.")


  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public ApiHiveReplicationResult tableCount(Integer tableCount) {
    this.tableCount = tableCount;
    return this;
  }

  /**
   * Number of tables that were successfully replicated. Available since API v4.
   * @return tableCount
  **/
  @ApiModelProperty(value = "Number of tables that were successfully replicated. Available since API v4.")


  public Integer getTableCount() {
    return tableCount;
  }

  public void setTableCount(Integer tableCount) {
    this.tableCount = tableCount;
  }

  public ApiHiveReplicationResult tables(List<ApiHiveTable> tables) {
    this.tables = tables;
    return this;
  }

  public ApiHiveReplicationResult addTablesItem(ApiHiveTable tablesItem) {
    if (this.tables == null) {
      this.tables = new ArrayList<>();
    }
    this.tables.add(tablesItem);
    return this;
  }

  /**
   * The list of tables successfully replicated. <p/> Since API v4, this is only available in the full view.
   * @return tables
  **/
  @ApiModelProperty(value = "The list of tables successfully replicated. <p/> Since API v4, this is only available in the full view.")

  @Valid

  public List<ApiHiveTable> getTables() {
    return tables;
  }

  public void setTables(List<ApiHiveTable> tables) {
    this.tables = tables;
  }

  public ApiHiveReplicationResult impalaUDFCount(Integer impalaUDFCount) {
    this.impalaUDFCount = impalaUDFCount;
    return this;
  }

  /**
   * Number of impala UDFs that were successfully replicated. Available since API v6.
   * @return impalaUDFCount
  **/
  @ApiModelProperty(value = "Number of impala UDFs that were successfully replicated. Available since API v6.")


  public Integer getImpalaUDFCount() {
    return impalaUDFCount;
  }

  public void setImpalaUDFCount(Integer impalaUDFCount) {
    this.impalaUDFCount = impalaUDFCount;
  }

  public ApiHiveReplicationResult hiveUDFCount(Integer hiveUDFCount) {
    this.hiveUDFCount = hiveUDFCount;
    return this;
  }

  /**
   * Number of hive UDFs that were successfully replicated. Available since API v14.
   * @return hiveUDFCount
  **/
  @ApiModelProperty(value = "Number of hive UDFs that were successfully replicated. Available since API v14.")


  public Integer getHiveUDFCount() {
    return hiveUDFCount;
  }

  public void setHiveUDFCount(Integer hiveUDFCount) {
    this.hiveUDFCount = hiveUDFCount;
  }

  public ApiHiveReplicationResult impalaUDFs(List<ApiImpalaUDF> impalaUDFs) {
    this.impalaUDFs = impalaUDFs;
    return this;
  }

  public ApiHiveReplicationResult addImpalaUDFsItem(ApiImpalaUDF impalaUDFsItem) {
    if (this.impalaUDFs == null) {
      this.impalaUDFs = new ArrayList<>();
    }
    this.impalaUDFs.add(impalaUDFsItem);
    return this;
  }

  /**
   * The list of Impala UDFs successfully replicated. Available since API v6 in the full view.
   * @return impalaUDFs
  **/
  @ApiModelProperty(value = "The list of Impala UDFs successfully replicated. Available since API v6 in the full view.")

  @Valid

  public List<ApiImpalaUDF> getImpalaUDFs() {
    return impalaUDFs;
  }

  public void setImpalaUDFs(List<ApiImpalaUDF> impalaUDFs) {
    this.impalaUDFs = impalaUDFs;
  }

  public ApiHiveReplicationResult hiveUDFs(List<ApiHiveUDF> hiveUDFs) {
    this.hiveUDFs = hiveUDFs;
    return this;
  }

  public ApiHiveReplicationResult addHiveUDFsItem(ApiHiveUDF hiveUDFsItem) {
    if (this.hiveUDFs == null) {
      this.hiveUDFs = new ArrayList<>();
    }
    this.hiveUDFs.add(hiveUDFsItem);
    return this;
  }

  /**
   * The list of Impala UDFs successfully replicated. Available since API v6 in the full view.
   * @return hiveUDFs
  **/
  @ApiModelProperty(value = "The list of Impala UDFs successfully replicated. Available since API v6 in the full view.")

  @Valid

  public List<ApiHiveUDF> getHiveUDFs() {
    return hiveUDFs;
  }

  public void setHiveUDFs(List<ApiHiveUDF> hiveUDFs) {
    this.hiveUDFs = hiveUDFs;
  }

  public ApiHiveReplicationResult errorCount(Integer errorCount) {
    this.errorCount = errorCount;
    return this;
  }

  /**
   * Number of errors detected during replication job. Available since API v4.
   * @return errorCount
  **/
  @ApiModelProperty(value = "Number of errors detected during replication job. Available since API v4.")


  public Integer getErrorCount() {
    return errorCount;
  }

  public void setErrorCount(Integer errorCount) {
    this.errorCount = errorCount;
  }

  public ApiHiveReplicationResult errors(List<ApiHiveReplicationError> errors) {
    this.errors = errors;
    return this;
  }

  public ApiHiveReplicationResult addErrorsItem(ApiHiveReplicationError errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }

  /**
   * List of errors encountered during replication. <p/> Since API v4, this is only available in the full view.
   * @return errors
  **/
  @ApiModelProperty(value = "List of errors encountered during replication. <p/> Since API v4, this is only available in the full view.")

  @Valid

  public List<ApiHiveReplicationError> getErrors() {
    return errors;
  }

  public void setErrors(List<ApiHiveReplicationError> errors) {
    this.errors = errors;
  }

  public ApiHiveReplicationResult dataReplicationResult(ApiHdfsReplicationResult dataReplicationResult) {
    this.dataReplicationResult = dataReplicationResult;
    return this;
  }

  /**
   * Result of table data replication, if performed.
   * @return dataReplicationResult
  **/
  @ApiModelProperty(value = "Result of table data replication, if performed.")

  @Valid

  public ApiHdfsReplicationResult getDataReplicationResult() {
    return dataReplicationResult;
  }

  public void setDataReplicationResult(ApiHdfsReplicationResult dataReplicationResult) {
    this.dataReplicationResult = dataReplicationResult;
  }

  public ApiHiveReplicationResult dryRun(Boolean dryRun) {
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

  public ApiHiveReplicationResult runAsUser(String runAsUser) {
    this.runAsUser = runAsUser;
    return this;
  }

  /**
   * Name of the of proxy user, if any. Available since API v11.
   * @return runAsUser
  **/
  @ApiModelProperty(value = "Name of the of proxy user, if any. Available since API v11.")


  public String getRunAsUser() {
    return runAsUser;
  }

  public void setRunAsUser(String runAsUser) {
    this.runAsUser = runAsUser;
  }

  public ApiHiveReplicationResult runOnSourceAsUser(String runOnSourceAsUser) {
    this.runOnSourceAsUser = runOnSourceAsUser;
    return this;
  }

  /**
   * Name of the source proxy user, if any. Available since API v18.
   * @return runOnSourceAsUser
  **/
  @ApiModelProperty(value = "Name of the source proxy user, if any. Available since API v18.")


  public String getRunOnSourceAsUser() {
    return runOnSourceAsUser;
  }

  public void setRunOnSourceAsUser(String runOnSourceAsUser) {
    this.runOnSourceAsUser = runOnSourceAsUser;
  }

  public ApiHiveReplicationResult logPath(String logPath) {
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

  public ApiHiveReplicationResult directoryForMetadata(String directoryForMetadata) {
    this.directoryForMetadata = directoryForMetadata;
    return this;
  }

  /**
   * Returns HDFS path of export file for Hive replication. Available since API v33.
   * @return directoryForMetadata
  **/
  @ApiModelProperty(value = "Returns HDFS path of export file for Hive replication. Available since API v33.")


  public String getDirectoryForMetadata() {
    return directoryForMetadata;
  }

  public void setDirectoryForMetadata(String directoryForMetadata) {
    this.directoryForMetadata = directoryForMetadata;
  }

  public ApiHiveReplicationResult statsAvailable(Boolean statsAvailable) {
    this.statsAvailable = statsAvailable;
    return this;
  }

  /**
   * Whether stats are available to display or not. Available since API v19.
   * @return statsAvailable
  **/
  @ApiModelProperty(value = "Whether stats are available to display or not. Available since API v19.")


  public Boolean isStatsAvailable() {
    return statsAvailable;
  }

  public void setStatsAvailable(Boolean statsAvailable) {
    this.statsAvailable = statsAvailable;
  }

  public ApiHiveReplicationResult dbProcessed(Integer dbProcessed) {
    this.dbProcessed = dbProcessed;
    return this;
  }

  /**
   * Number of Db's Imported/Exported. Available since API v19.
   * @return dbProcessed
  **/
  @ApiModelProperty(value = "Number of Db's Imported/Exported. Available since API v19.")


  public Integer getDbProcessed() {
    return dbProcessed;
  }

  public void setDbProcessed(Integer dbProcessed) {
    this.dbProcessed = dbProcessed;
  }

  public ApiHiveReplicationResult tableProcessed(Integer tableProcessed) {
    this.tableProcessed = tableProcessed;
    return this;
  }

  /**
   * Number of Tables Imported/Exported. Available since API v19.
   * @return tableProcessed
  **/
  @ApiModelProperty(value = "Number of Tables Imported/Exported. Available since API v19.")


  public Integer getTableProcessed() {
    return tableProcessed;
  }

  public void setTableProcessed(Integer tableProcessed) {
    this.tableProcessed = tableProcessed;
  }

  public ApiHiveReplicationResult partitionProcessed(Integer partitionProcessed) {
    this.partitionProcessed = partitionProcessed;
    return this;
  }

  /**
   * Number of Partitions Imported/Exported. Available since API v19.
   * @return partitionProcessed
  **/
  @ApiModelProperty(value = "Number of Partitions Imported/Exported. Available since API v19.")


  public Integer getPartitionProcessed() {
    return partitionProcessed;
  }

  public void setPartitionProcessed(Integer partitionProcessed) {
    this.partitionProcessed = partitionProcessed;
  }

  public ApiHiveReplicationResult functionProcessed(Integer functionProcessed) {
    this.functionProcessed = functionProcessed;
    return this;
  }

  /**
   * Number of Functions Imported/Exported. Available since API v19.
   * @return functionProcessed
  **/
  @ApiModelProperty(value = "Number of Functions Imported/Exported. Available since API v19.")


  public Integer getFunctionProcessed() {
    return functionProcessed;
  }

  public void setFunctionProcessed(Integer functionProcessed) {
    this.functionProcessed = functionProcessed;
  }

  public ApiHiveReplicationResult indexProcessed(Integer indexProcessed) {
    this.indexProcessed = indexProcessed;
    return this;
  }

  /**
   * Number of Indexes Imported/Exported. Available since API v19.
   * @return indexProcessed
  **/
  @ApiModelProperty(value = "Number of Indexes Imported/Exported. Available since API v19.")


  public Integer getIndexProcessed() {
    return indexProcessed;
  }

  public void setIndexProcessed(Integer indexProcessed) {
    this.indexProcessed = indexProcessed;
  }

  public ApiHiveReplicationResult statsProcessed(Integer statsProcessed) {
    this.statsProcessed = statsProcessed;
    return this;
  }

  /**
   * Number of Table and Partitions Statistics Imported/Exported. Available since API v19.
   * @return statsProcessed
  **/
  @ApiModelProperty(value = "Number of Table and Partitions Statistics Imported/Exported. Available since API v19.")


  public Integer getStatsProcessed() {
    return statsProcessed;
  }

  public void setStatsProcessed(Integer statsProcessed) {
    this.statsProcessed = statsProcessed;
  }

  public ApiHiveReplicationResult dbExpected(Integer dbExpected) {
    this.dbExpected = dbExpected;
    return this;
  }

  /**
   * Number of Db's Expected. Available since API v19.
   * @return dbExpected
  **/
  @ApiModelProperty(value = "Number of Db's Expected. Available since API v19.")


  public Integer getDbExpected() {
    return dbExpected;
  }

  public void setDbExpected(Integer dbExpected) {
    this.dbExpected = dbExpected;
  }

  public ApiHiveReplicationResult tableExpected(Integer tableExpected) {
    this.tableExpected = tableExpected;
    return this;
  }

  /**
   * Number of Tables Expected. Available since API v19.
   * @return tableExpected
  **/
  @ApiModelProperty(value = "Number of Tables Expected. Available since API v19.")


  public Integer getTableExpected() {
    return tableExpected;
  }

  public void setTableExpected(Integer tableExpected) {
    this.tableExpected = tableExpected;
  }

  public ApiHiveReplicationResult partitionExpected(Integer partitionExpected) {
    this.partitionExpected = partitionExpected;
    return this;
  }

  /**
   * Number of Partitions Expected. Available since API v19.
   * @return partitionExpected
  **/
  @ApiModelProperty(value = "Number of Partitions Expected. Available since API v19.")


  public Integer getPartitionExpected() {
    return partitionExpected;
  }

  public void setPartitionExpected(Integer partitionExpected) {
    this.partitionExpected = partitionExpected;
  }

  public ApiHiveReplicationResult functionExpected(Integer functionExpected) {
    this.functionExpected = functionExpected;
    return this;
  }

  /**
   * Number of Functions Expected. Available since API v19.
   * @return functionExpected
  **/
  @ApiModelProperty(value = "Number of Functions Expected. Available since API v19.")


  public Integer getFunctionExpected() {
    return functionExpected;
  }

  public void setFunctionExpected(Integer functionExpected) {
    this.functionExpected = functionExpected;
  }

  public ApiHiveReplicationResult indexExpected(Integer indexExpected) {
    this.indexExpected = indexExpected;
    return this;
  }

  /**
   * Number of Indexes Expected. Available since API v19.
   * @return indexExpected
  **/
  @ApiModelProperty(value = "Number of Indexes Expected. Available since API v19.")


  public Integer getIndexExpected() {
    return indexExpected;
  }

  public void setIndexExpected(Integer indexExpected) {
    this.indexExpected = indexExpected;
  }

  public ApiHiveReplicationResult statsExpected(Integer statsExpected) {
    this.statsExpected = statsExpected;
    return this;
  }

  /**
   * Number of Table and Partition Statistics Expected. Available since API v19.
   * @return statsExpected
  **/
  @ApiModelProperty(value = "Number of Table and Partition Statistics Expected. Available since API v19.")


  public Integer getStatsExpected() {
    return statsExpected;
  }

  public void setStatsExpected(Integer statsExpected) {
    this.statsExpected = statsExpected;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHiveReplicationResult apiHiveReplicationResult = (ApiHiveReplicationResult) o;
    return Objects.equals(this.phase, apiHiveReplicationResult.phase) &&
        Objects.equals(this.tableCount, apiHiveReplicationResult.tableCount) &&
        Objects.equals(this.tables, apiHiveReplicationResult.tables) &&
        Objects.equals(this.impalaUDFCount, apiHiveReplicationResult.impalaUDFCount) &&
        Objects.equals(this.hiveUDFCount, apiHiveReplicationResult.hiveUDFCount) &&
        Objects.equals(this.impalaUDFs, apiHiveReplicationResult.impalaUDFs) &&
        Objects.equals(this.hiveUDFs, apiHiveReplicationResult.hiveUDFs) &&
        Objects.equals(this.errorCount, apiHiveReplicationResult.errorCount) &&
        Objects.equals(this.errors, apiHiveReplicationResult.errors) &&
        Objects.equals(this.dataReplicationResult, apiHiveReplicationResult.dataReplicationResult) &&
        Objects.equals(this.dryRun, apiHiveReplicationResult.dryRun) &&
        Objects.equals(this.runAsUser, apiHiveReplicationResult.runAsUser) &&
        Objects.equals(this.runOnSourceAsUser, apiHiveReplicationResult.runOnSourceAsUser) &&
        Objects.equals(this.logPath, apiHiveReplicationResult.logPath) &&
        Objects.equals(this.directoryForMetadata, apiHiveReplicationResult.directoryForMetadata) &&
        Objects.equals(this.statsAvailable, apiHiveReplicationResult.statsAvailable) &&
        Objects.equals(this.dbProcessed, apiHiveReplicationResult.dbProcessed) &&
        Objects.equals(this.tableProcessed, apiHiveReplicationResult.tableProcessed) &&
        Objects.equals(this.partitionProcessed, apiHiveReplicationResult.partitionProcessed) &&
        Objects.equals(this.functionProcessed, apiHiveReplicationResult.functionProcessed) &&
        Objects.equals(this.indexProcessed, apiHiveReplicationResult.indexProcessed) &&
        Objects.equals(this.statsProcessed, apiHiveReplicationResult.statsProcessed) &&
        Objects.equals(this.dbExpected, apiHiveReplicationResult.dbExpected) &&
        Objects.equals(this.tableExpected, apiHiveReplicationResult.tableExpected) &&
        Objects.equals(this.partitionExpected, apiHiveReplicationResult.partitionExpected) &&
        Objects.equals(this.functionExpected, apiHiveReplicationResult.functionExpected) &&
        Objects.equals(this.indexExpected, apiHiveReplicationResult.indexExpected) &&
        Objects.equals(this.statsExpected, apiHiveReplicationResult.statsExpected);
  }

  @Override
  public int hashCode() {
    return Objects.hash(phase, tableCount, tables, impalaUDFCount, hiveUDFCount, impalaUDFs, hiveUDFs, errorCount, errors, dataReplicationResult, dryRun, runAsUser, runOnSourceAsUser, logPath, directoryForMetadata, statsAvailable, dbProcessed, tableProcessed, partitionProcessed, functionProcessed, indexProcessed, statsProcessed, dbExpected, tableExpected, partitionExpected, functionExpected, indexExpected, statsExpected);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHiveReplicationResult {\n");
    
    sb.append("    phase: ").append(toIndentedString(phase)).append("\n");
    sb.append("    tableCount: ").append(toIndentedString(tableCount)).append("\n");
    sb.append("    tables: ").append(toIndentedString(tables)).append("\n");
    sb.append("    impalaUDFCount: ").append(toIndentedString(impalaUDFCount)).append("\n");
    sb.append("    hiveUDFCount: ").append(toIndentedString(hiveUDFCount)).append("\n");
    sb.append("    impalaUDFs: ").append(toIndentedString(impalaUDFs)).append("\n");
    sb.append("    hiveUDFs: ").append(toIndentedString(hiveUDFs)).append("\n");
    sb.append("    errorCount: ").append(toIndentedString(errorCount)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
    sb.append("    dataReplicationResult: ").append(toIndentedString(dataReplicationResult)).append("\n");
    sb.append("    dryRun: ").append(toIndentedString(dryRun)).append("\n");
    sb.append("    runAsUser: ").append(toIndentedString(runAsUser)).append("\n");
    sb.append("    runOnSourceAsUser: ").append(toIndentedString(runOnSourceAsUser)).append("\n");
    sb.append("    logPath: ").append(toIndentedString(logPath)).append("\n");
    sb.append("    directoryForMetadata: ").append(toIndentedString(directoryForMetadata)).append("\n");
    sb.append("    statsAvailable: ").append(toIndentedString(statsAvailable)).append("\n");
    sb.append("    dbProcessed: ").append(toIndentedString(dbProcessed)).append("\n");
    sb.append("    tableProcessed: ").append(toIndentedString(tableProcessed)).append("\n");
    sb.append("    partitionProcessed: ").append(toIndentedString(partitionProcessed)).append("\n");
    sb.append("    functionProcessed: ").append(toIndentedString(functionProcessed)).append("\n");
    sb.append("    indexProcessed: ").append(toIndentedString(indexProcessed)).append("\n");
    sb.append("    statsProcessed: ").append(toIndentedString(statsProcessed)).append("\n");
    sb.append("    dbExpected: ").append(toIndentedString(dbExpected)).append("\n");
    sb.append("    tableExpected: ").append(toIndentedString(tableExpected)).append("\n");
    sb.append("    partitionExpected: ").append(toIndentedString(partitionExpected)).append("\n");
    sb.append("    functionExpected: ").append(toIndentedString(functionExpected)).append("\n");
    sb.append("    indexExpected: ").append(toIndentedString(indexExpected)).append("\n");
    sb.append("    statsExpected: ").append(toIndentedString(statsExpected)).append("\n");
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

