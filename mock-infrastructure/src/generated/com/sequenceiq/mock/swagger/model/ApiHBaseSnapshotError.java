package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.Storage;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A HBase snapshot operation error.
 */
@ApiModel(description = "A HBase snapshot operation error.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiHBaseSnapshotError   {
  @JsonProperty("tableName")
  private String tableName = null;

  @JsonProperty("snapshotName")
  private String snapshotName = null;

  @JsonProperty("storage")
  private Storage storage = null;

  @JsonProperty("error")
  private String error = null;

  public ApiHBaseSnapshotError tableName(String tableName) {
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

  public ApiHBaseSnapshotError snapshotName(String snapshotName) {
    this.snapshotName = snapshotName;
    return this;
  }

  /**
   * Name of the snapshot.
   * @return snapshotName
  **/
  @ApiModelProperty(value = "Name of the snapshot.")


  public String getSnapshotName() {
    return snapshotName;
  }

  public void setSnapshotName(String snapshotName) {
    this.snapshotName = snapshotName;
  }

  public ApiHBaseSnapshotError storage(Storage storage) {
    this.storage = storage;
    return this;
  }

  /**
   * The location of the snapshot.
   * @return storage
  **/
  @ApiModelProperty(value = "The location of the snapshot.")

  @Valid

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
  }

  public ApiHBaseSnapshotError error(String error) {
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
    ApiHBaseSnapshotError apiHBaseSnapshotError = (ApiHBaseSnapshotError) o;
    return Objects.equals(this.tableName, apiHBaseSnapshotError.tableName) &&
        Objects.equals(this.snapshotName, apiHBaseSnapshotError.snapshotName) &&
        Objects.equals(this.storage, apiHBaseSnapshotError.storage) &&
        Objects.equals(this.error, apiHBaseSnapshotError.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableName, snapshotName, storage, error);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseSnapshotError {\n");
    
    sb.append("    tableName: ").append(toIndentedString(tableName)).append("\n");
    sb.append("    snapshotName: ").append(toIndentedString(snapshotName)).append("\n");
    sb.append("    storage: ").append(toIndentedString(storage)).append("\n");
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

