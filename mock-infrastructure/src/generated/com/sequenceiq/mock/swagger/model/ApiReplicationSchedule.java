package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHBaseReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiHdfsCloudReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiHdfsReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiHiveCloudReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiHiveReplicationArguments;
import com.sequenceiq.mock.swagger.model.ApiReplicationCommand;
import com.sequenceiq.mock.swagger.model.ApiSchedule;
import com.sequenceiq.mock.swagger.model.ApiScheduleInterval;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A replication job schedule. &lt;p/&gt; Replication jobs have service-specific arguments. This object has methods to retrieve arguments for all supported types of replication, but only one argument type is allowed to be set; the backend will check that the provided argument matches the service type where the replication is being scheduled. &lt;p/&gt; The replication job&#39;s arguments should match the underlying service. Refer to each property&#39;s documentation to find out which properties correspond to which services.
 */
@ApiModel(description = "A replication job schedule. <p/> Replication jobs have service-specific arguments. This object has methods to retrieve arguments for all supported types of replication, but only one argument type is allowed to be set; the backend will check that the provided argument matches the service type where the replication is being scheduled. <p/> The replication job's arguments should match the underlying service. Refer to each property's documentation to find out which properties correspond to which services.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiReplicationSchedule extends ApiSchedule  {
  @JsonProperty("hdfsArguments")
  private ApiHdfsReplicationArguments hdfsArguments = null;

  @JsonProperty("hiveArguments")
  private ApiHiveReplicationArguments hiveArguments = null;

  @JsonProperty("hdfsCloudArguments")
  private ApiHdfsCloudReplicationArguments hdfsCloudArguments = null;

  @JsonProperty("history")
  @Valid
  private List<ApiReplicationCommand> history = null;

  @JsonProperty("active")
  private Boolean active = null;

  @JsonProperty("hiveCloudArguments")
  private ApiHiveCloudReplicationArguments hiveCloudArguments = null;

  @JsonProperty("hbaseArguments")
  private ApiHBaseReplicationArguments hbaseArguments = null;

  @JsonProperty("hive3Arguments")
  private ApiHive3ReplicationArguments hive3Arguments = null;

  public ApiReplicationSchedule hdfsArguments(ApiHdfsReplicationArguments hdfsArguments) {
    this.hdfsArguments = hdfsArguments;
    return this;
  }

  /**
   * Arguments for HDFS replication commands.
   * @return hdfsArguments
  **/
  @ApiModelProperty(value = "Arguments for HDFS replication commands.")

  @Valid

  public ApiHdfsReplicationArguments getHdfsArguments() {
    return hdfsArguments;
  }

  public void setHdfsArguments(ApiHdfsReplicationArguments hdfsArguments) {
    this.hdfsArguments = hdfsArguments;
  }

  public ApiReplicationSchedule hiveArguments(ApiHiveReplicationArguments hiveArguments) {
    this.hiveArguments = hiveArguments;
    return this;
  }

  /**
   * Arguments for Hive replication commands.
   * @return hiveArguments
  **/
  @ApiModelProperty(value = "Arguments for Hive replication commands.")

  @Valid

  public ApiHiveReplicationArguments getHiveArguments() {
    return hiveArguments;
  }

  public void setHiveArguments(ApiHiveReplicationArguments hiveArguments) {
    this.hiveArguments = hiveArguments;
  }

  public ApiReplicationSchedule hdfsCloudArguments(ApiHdfsCloudReplicationArguments hdfsCloudArguments) {
    this.hdfsCloudArguments = hdfsCloudArguments;
    return this;
  }

  /**
   * Arguments for HDFS cloud replication commands.
   * @return hdfsCloudArguments
  **/
  @ApiModelProperty(value = "Arguments for HDFS cloud replication commands.")

  @Valid

  public ApiHdfsCloudReplicationArguments getHdfsCloudArguments() {
    return hdfsCloudArguments;
  }

  public void setHdfsCloudArguments(ApiHdfsCloudReplicationArguments hdfsCloudArguments) {
    this.hdfsCloudArguments = hdfsCloudArguments;
  }

  public ApiReplicationSchedule history(List<ApiReplicationCommand> history) {
    this.history = history;
    return this;
  }

  public ApiReplicationSchedule addHistoryItem(ApiReplicationCommand historyItem) {
    if (this.history == null) {
      this.history = new ArrayList<>();
    }
    this.history.add(historyItem);
    return this;
  }

  /**
   * List of active and/or finished commands for this schedule.
   * @return history
  **/
  @ApiModelProperty(value = "List of active and/or finished commands for this schedule.")

  @Valid

  public List<ApiReplicationCommand> getHistory() {
    return history;
  }

  public void setHistory(List<ApiReplicationCommand> history) {
    this.history = history;
  }

  public ApiReplicationSchedule active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * Read-only field that is true if this schedule is currently active, false if not. Available since API v11.
   * @return active
  **/
  @ApiModelProperty(value = "Read-only field that is true if this schedule is currently active, false if not. Available since API v11.")


  public Boolean isActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public ApiReplicationSchedule hiveCloudArguments(ApiHiveCloudReplicationArguments hiveCloudArguments) {
    this.hiveCloudArguments = hiveCloudArguments;
    return this;
  }

  /**
   * Arguments for Hive cloud replication commands.
   * @return hiveCloudArguments
  **/
  @ApiModelProperty(value = "Arguments for Hive cloud replication commands.")

  @Valid

  public ApiHiveCloudReplicationArguments getHiveCloudArguments() {
    return hiveCloudArguments;
  }

  public void setHiveCloudArguments(ApiHiveCloudReplicationArguments hiveCloudArguments) {
    this.hiveCloudArguments = hiveCloudArguments;
  }

  public ApiReplicationSchedule hbaseArguments(ApiHBaseReplicationArguments hbaseArguments) {
    this.hbaseArguments = hbaseArguments;
    return this;
  }

  /**
   * Arguments for HBase replication commands.
   * @return hbaseArguments
  **/
  @ApiModelProperty(value = "Arguments for HBase replication commands.")

  @Valid

  public ApiHBaseReplicationArguments getHbaseArguments() {
    return hbaseArguments;
  }

  public void setHbaseArguments(ApiHBaseReplicationArguments hbaseArguments) {
    this.hbaseArguments = hbaseArguments;
  }

  public ApiReplicationSchedule hive3Arguments(ApiHive3ReplicationArguments hive3Arguments) {
    this.hive3Arguments = hive3Arguments;
    return this;
  }

  /**
   * arguments for Hive3 schedules
   * @return hive3Arguments
  **/
  @ApiModelProperty(value = "arguments for Hive3 schedules")

  @Valid

  public ApiHive3ReplicationArguments getHive3Arguments() {
    return hive3Arguments;
  }

  public void setHive3Arguments(ApiHive3ReplicationArguments hive3Arguments) {
    this.hive3Arguments = hive3Arguments;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiReplicationSchedule apiReplicationSchedule = (ApiReplicationSchedule) o;
    return Objects.equals(this.hdfsArguments, apiReplicationSchedule.hdfsArguments) &&
        Objects.equals(this.hiveArguments, apiReplicationSchedule.hiveArguments) &&
        Objects.equals(this.hdfsCloudArguments, apiReplicationSchedule.hdfsCloudArguments) &&
        Objects.equals(this.history, apiReplicationSchedule.history) &&
        Objects.equals(this.active, apiReplicationSchedule.active) &&
        Objects.equals(this.hiveCloudArguments, apiReplicationSchedule.hiveCloudArguments) &&
        Objects.equals(this.hbaseArguments, apiReplicationSchedule.hbaseArguments) &&
        Objects.equals(this.hive3Arguments, apiReplicationSchedule.hive3Arguments) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hdfsArguments, hiveArguments, hdfsCloudArguments, history, active, hiveCloudArguments, hbaseArguments, hive3Arguments, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiReplicationSchedule {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    hdfsArguments: ").append(toIndentedString(hdfsArguments)).append("\n");
    sb.append("    hiveArguments: ").append(toIndentedString(hiveArguments)).append("\n");
    sb.append("    hdfsCloudArguments: ").append(toIndentedString(hdfsCloudArguments)).append("\n");
    sb.append("    history: ").append(toIndentedString(history)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    hiveCloudArguments: ").append(toIndentedString(hiveCloudArguments)).append("\n");
    sb.append("    hbaseArguments: ").append(toIndentedString(hbaseArguments)).append("\n");
    sb.append("    hive3Arguments: ").append(toIndentedString(hive3Arguments)).append("\n");
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

