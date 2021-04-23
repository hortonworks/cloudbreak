package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationMetricsResultRow;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationScheduledExecutionsResultRow;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationScheduledQueriesResultRow;
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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiHive3ReplicationQueryResultRow   {
  @JsonProperty("scheduledQueriesResultRow")
  private ApiHive3ReplicationScheduledQueriesResultRow scheduledQueriesResultRow = null;

  @JsonProperty("scheduledExecutionsResultRow")
  private ApiHive3ReplicationScheduledExecutionsResultRow scheduledExecutionsResultRow = null;

  @JsonProperty("replicationMetricsResultRow")
  private ApiHive3ReplicationMetricsResultRow replicationMetricsResultRow = null;

  public ApiHive3ReplicationQueryResultRow scheduledQueriesResultRow(ApiHive3ReplicationScheduledQueriesResultRow scheduledQueriesResultRow) {
    this.scheduledQueriesResultRow = scheduledQueriesResultRow;
    return this;
  }

  /**
   * 
   * @return scheduledQueriesResultRow
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHive3ReplicationScheduledQueriesResultRow getScheduledQueriesResultRow() {
    return scheduledQueriesResultRow;
  }

  public void setScheduledQueriesResultRow(ApiHive3ReplicationScheduledQueriesResultRow scheduledQueriesResultRow) {
    this.scheduledQueriesResultRow = scheduledQueriesResultRow;
  }

  public ApiHive3ReplicationQueryResultRow scheduledExecutionsResultRow(ApiHive3ReplicationScheduledExecutionsResultRow scheduledExecutionsResultRow) {
    this.scheduledExecutionsResultRow = scheduledExecutionsResultRow;
    return this;
  }

  /**
   * 
   * @return scheduledExecutionsResultRow
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHive3ReplicationScheduledExecutionsResultRow getScheduledExecutionsResultRow() {
    return scheduledExecutionsResultRow;
  }

  public void setScheduledExecutionsResultRow(ApiHive3ReplicationScheduledExecutionsResultRow scheduledExecutionsResultRow) {
    this.scheduledExecutionsResultRow = scheduledExecutionsResultRow;
  }

  public ApiHive3ReplicationQueryResultRow replicationMetricsResultRow(ApiHive3ReplicationMetricsResultRow replicationMetricsResultRow) {
    this.replicationMetricsResultRow = replicationMetricsResultRow;
    return this;
  }

  /**
   * 
   * @return replicationMetricsResultRow
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHive3ReplicationMetricsResultRow getReplicationMetricsResultRow() {
    return replicationMetricsResultRow;
  }

  public void setReplicationMetricsResultRow(ApiHive3ReplicationMetricsResultRow replicationMetricsResultRow) {
    this.replicationMetricsResultRow = replicationMetricsResultRow;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHive3ReplicationQueryResultRow apiHive3ReplicationQueryResultRow = (ApiHive3ReplicationQueryResultRow) o;
    return Objects.equals(this.scheduledQueriesResultRow, apiHive3ReplicationQueryResultRow.scheduledQueriesResultRow) &&
        Objects.equals(this.scheduledExecutionsResultRow, apiHive3ReplicationQueryResultRow.scheduledExecutionsResultRow) &&
        Objects.equals(this.replicationMetricsResultRow, apiHive3ReplicationQueryResultRow.replicationMetricsResultRow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scheduledQueriesResultRow, scheduledExecutionsResultRow, replicationMetricsResultRow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHive3ReplicationQueryResultRow {\n");
    
    sb.append("    scheduledQueriesResultRow: ").append(toIndentedString(scheduledQueriesResultRow)).append("\n");
    sb.append("    scheduledExecutionsResultRow: ").append(toIndentedString(scheduledExecutionsResultRow)).append("\n");
    sb.append("    replicationMetricsResultRow: ").append(toIndentedString(replicationMetricsResultRow)).append("\n");
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

