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
 * An HBase snapshot descriptor.
 */
@ApiModel(description = "An HBase snapshot descriptor.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHBaseSnapshot   {
  @JsonProperty("snapshotName")
  private String snapshotName = null;

  @JsonProperty("tableName")
  private String tableName = null;

  @JsonProperty("creationTime")
  private String creationTime = null;

  @JsonProperty("storage")
  private Storage storage = null;

  @JsonProperty("storageLocation")
  private String storageLocation = null;

  public ApiHBaseSnapshot snapshotName(String snapshotName) {
    this.snapshotName = snapshotName;
    return this;
  }

  /**
   * Snapshot name.
   * @return snapshotName
  **/
  @ApiModelProperty(value = "Snapshot name.")


  public String getSnapshotName() {
    return snapshotName;
  }

  public void setSnapshotName(String snapshotName) {
    this.snapshotName = snapshotName;
  }

  public ApiHBaseSnapshot tableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * Name of the table this snapshot is for.
   * @return tableName
  **/
  @ApiModelProperty(value = "Name of the table this snapshot is for.")


  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public ApiHBaseSnapshot creationTime(String creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  /**
   * Snapshot creation time.
   * @return creationTime
  **/
  @ApiModelProperty(value = "Snapshot creation time.")


  public String getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(String creationTime) {
    this.creationTime = creationTime;
  }

  public ApiHBaseSnapshot storage(Storage storage) {
    this.storage = storage;
    return this;
  }

  /**
   * Whether the snapshot is local or remote.
   * @return storage
  **/
  @ApiModelProperty(value = "Whether the snapshot is local or remote.")

  @Valid

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
  }

  public ApiHBaseSnapshot storageLocation(String storageLocation) {
    this.storageLocation = storageLocation;
    return this;
  }

  /**
   * The remote location where a snapshot is stored. /_*  Optional - location comes from HBase service config if this is not defined
   * @return storageLocation
  **/
  @ApiModelProperty(value = "The remote location where a snapshot is stored. /_*  Optional - location comes from HBase service config if this is not defined")


  public String getStorageLocation() {
    return storageLocation;
  }

  public void setStorageLocation(String storageLocation) {
    this.storageLocation = storageLocation;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHBaseSnapshot apiHBaseSnapshot = (ApiHBaseSnapshot) o;
    return Objects.equals(this.snapshotName, apiHBaseSnapshot.snapshotName) &&
        Objects.equals(this.tableName, apiHBaseSnapshot.tableName) &&
        Objects.equals(this.creationTime, apiHBaseSnapshot.creationTime) &&
        Objects.equals(this.storage, apiHBaseSnapshot.storage) &&
        Objects.equals(this.storageLocation, apiHBaseSnapshot.storageLocation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(snapshotName, tableName, creationTime, storage, storageLocation);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseSnapshot {\n");
    
    sb.append("    snapshotName: ").append(toIndentedString(snapshotName)).append("\n");
    sb.append("    tableName: ").append(toIndentedString(tableName)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    storage: ").append(toIndentedString(storage)).append("\n");
    sb.append("    storageLocation: ").append(toIndentedString(storageLocation)).append("\n");
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

