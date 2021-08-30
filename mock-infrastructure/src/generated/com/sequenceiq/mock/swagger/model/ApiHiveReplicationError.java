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
 * A Hive replication error.
 */
@ApiModel(description = "A Hive replication error.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHiveReplicationError   {
  @JsonProperty("database")
  private String database = null;

  @JsonProperty("tableName")
  private String tableName = null;

  @JsonProperty("impalaUDF")
  private String impalaUDF = null;

  @JsonProperty("hiveUDF")
  private String hiveUDF = null;

  @JsonProperty("error")
  private String error = null;

  public ApiHiveReplicationError database(String database) {
    this.database = database;
    return this;
  }

  /**
   * Name of the database.
   * @return database
  **/
  @ApiModelProperty(value = "Name of the database.")


  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public ApiHiveReplicationError tableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * Name of the table.
   * @return tableName
  **/
  @ApiModelProperty(value = "Name of the table.")


  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public ApiHiveReplicationError impalaUDF(String impalaUDF) {
    this.impalaUDF = impalaUDF;
    return this;
  }

  /**
   * UDF signature, includes the UDF name and parameter types.
   * @return impalaUDF
  **/
  @ApiModelProperty(value = "UDF signature, includes the UDF name and parameter types.")


  public String getImpalaUDF() {
    return impalaUDF;
  }

  public void setImpalaUDF(String impalaUDF) {
    this.impalaUDF = impalaUDF;
  }

  public ApiHiveReplicationError hiveUDF(String hiveUDF) {
    this.hiveUDF = hiveUDF;
    return this;
  }

  /**
   * Hive UDF signature, includes the UDF name and parameter types.
   * @return hiveUDF
  **/
  @ApiModelProperty(value = "Hive UDF signature, includes the UDF name and parameter types.")


  public String getHiveUDF() {
    return hiveUDF;
  }

  public void setHiveUDF(String hiveUDF) {
    this.hiveUDF = hiveUDF;
  }

  public ApiHiveReplicationError error(String error) {
    this.error = error;
    return this;
  }

  /**
   * Description of the error.
   * @return error
  **/
  @ApiModelProperty(value = "Description of the error.")


  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHiveReplicationError apiHiveReplicationError = (ApiHiveReplicationError) o;
    return Objects.equals(this.database, apiHiveReplicationError.database) &&
        Objects.equals(this.tableName, apiHiveReplicationError.tableName) &&
        Objects.equals(this.impalaUDF, apiHiveReplicationError.impalaUDF) &&
        Objects.equals(this.hiveUDF, apiHiveReplicationError.hiveUDF) &&
        Objects.equals(this.error, apiHiveReplicationError.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(database, tableName, impalaUDF, hiveUDF, error);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHiveReplicationError {\n");
    
    sb.append("    database: ").append(toIndentedString(database)).append("\n");
    sb.append("    tableName: ").append(toIndentedString(tableName)).append("\n");
    sb.append("    impalaUDF: ").append(toIndentedString(impalaUDF)).append("\n");
    sb.append("    hiveUDF: ").append(toIndentedString(hiveUDF)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
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

