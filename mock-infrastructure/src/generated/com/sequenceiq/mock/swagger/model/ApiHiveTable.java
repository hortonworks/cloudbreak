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
 * A Hive table identifier.
 */
@ApiModel(description = "A Hive table identifier.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHiveTable   {
  @JsonProperty("database")
  private String database = null;

  @JsonProperty("tableName")
  private String tableName = null;

  public ApiHiveTable database(String database) {
    this.database = database;
    return this;
  }

  /**
   * Name of the database to which this table belongs.
   * @return database
  **/
  @ApiModelProperty(value = "Name of the database to which this table belongs.")


  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public ApiHiveTable tableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * Name of the table. When used as input for a replication job, this can be a regular expression that matches several table names. Refer to the Hive documentation for the syntax of regular expressions.
   * @return tableName
  **/
  @ApiModelProperty(value = "Name of the table. When used as input for a replication job, this can be a regular expression that matches several table names. Refer to the Hive documentation for the syntax of regular expressions.")


  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHiveTable apiHiveTable = (ApiHiveTable) o;
    return Objects.equals(this.database, apiHiveTable.database) &&
        Objects.equals(this.tableName, apiHiveTable.tableName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(database, tableName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHiveTable {\n");
    
    sb.append("    database: ").append(toIndentedString(database)).append("\n");
    sb.append("    tableName: ").append(toIndentedString(tableName)).append("\n");
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

