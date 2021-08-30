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
 * The state of Hive/HDFS Replication.
 */
@ApiModel(description = "The state of Hive/HDFS Replication.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiReplicationState   {
  @JsonProperty("incrementalExportEnabled")
  private Boolean incrementalExportEnabled = null;

  public ApiReplicationState incrementalExportEnabled(Boolean incrementalExportEnabled) {
    this.incrementalExportEnabled = incrementalExportEnabled;
    return this;
  }

  /**
   * returns if incremental export is enabled for the given Hive service. Not applicable for HDFS service.
   * @return incrementalExportEnabled
  **/
  @ApiModelProperty(value = "returns if incremental export is enabled for the given Hive service. Not applicable for HDFS service.")


  public Boolean isIncrementalExportEnabled() {
    return incrementalExportEnabled;
  }

  public void setIncrementalExportEnabled(Boolean incrementalExportEnabled) {
    this.incrementalExportEnabled = incrementalExportEnabled;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiReplicationState apiReplicationState = (ApiReplicationState) o;
    return Objects.equals(this.incrementalExportEnabled, apiReplicationState.incrementalExportEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(incrementalExportEnabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiReplicationState {\n");
    
    sb.append("    incrementalExportEnabled: ").append(toIndentedString(incrementalExportEnabled)).append("\n");
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

