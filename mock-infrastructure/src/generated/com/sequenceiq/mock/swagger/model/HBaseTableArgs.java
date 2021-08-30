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
 * Holds information for an HBase table.
 */
@ApiModel(description = "Holds information for an HBase table.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class HBaseTableArgs   {
  @JsonProperty("tableName")
  private String tableName = null;

  @JsonProperty("initialSnapshot")
  private Boolean initialSnapshot = null;

  public HBaseTableArgs tableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * 
   * @return tableName
  **/
  @ApiModelProperty(value = "")


  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public HBaseTableArgs initialSnapshot(Boolean initialSnapshot) {
    this.initialSnapshot = initialSnapshot;
    return this;
  }

  /**
   * 
   * @return initialSnapshot
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean isInitialSnapshot() {
    return initialSnapshot;
  }

  public void setInitialSnapshot(Boolean initialSnapshot) {
    this.initialSnapshot = initialSnapshot;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HBaseTableArgs hbaseTableArgs = (HBaseTableArgs) o;
    return Objects.equals(this.tableName, hbaseTableArgs.tableName) &&
        Objects.equals(this.initialSnapshot, hbaseTableArgs.initialSnapshot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableName, initialSnapshot);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HBaseTableArgs {\n");
    
    sb.append("    tableName: ").append(toIndentedString(tableName)).append("\n");
    sb.append("    initialSnapshot: ").append(toIndentedString(initialSnapshot)).append("\n");
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

