package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.Storage;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * HBase specific snapshot policy arguments.
 */
@ApiModel(description = "HBase specific snapshot policy arguments.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHBaseSnapshotPolicyArguments   {
  @JsonProperty("tableRegExps")
  @Valid
  private List<String> tableRegExps = null;

  @JsonProperty("storage")
  private Storage storage = null;

  public ApiHBaseSnapshotPolicyArguments tableRegExps(List<String> tableRegExps) {
    this.tableRegExps = tableRegExps;
    return this;
  }

  public ApiHBaseSnapshotPolicyArguments addTableRegExpsItem(String tableRegExpsItem) {
    if (this.tableRegExps == null) {
      this.tableRegExps = new ArrayList<>();
    }
    this.tableRegExps.add(tableRegExpsItem);
    return this;
  }

  /**
   * The regular expressions specifying the tables. Tables matching any of them will be eligible for snapshot creation.
   * @return tableRegExps
  **/
  @ApiModelProperty(value = "The regular expressions specifying the tables. Tables matching any of them will be eligible for snapshot creation.")


  public List<String> getTableRegExps() {
    return tableRegExps;
  }

  public void setTableRegExps(List<String> tableRegExps) {
    this.tableRegExps = tableRegExps;
  }

  public ApiHBaseSnapshotPolicyArguments storage(Storage storage) {
    this.storage = storage;
    return this;
  }

  /**
   * The location where the snapshots should be stored.
   * @return storage
  **/
  @ApiModelProperty(value = "The location where the snapshots should be stored.")

  @Valid

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHBaseSnapshotPolicyArguments apiHBaseSnapshotPolicyArguments = (ApiHBaseSnapshotPolicyArguments) o;
    return Objects.equals(this.tableRegExps, apiHBaseSnapshotPolicyArguments.tableRegExps) &&
        Objects.equals(this.storage, apiHBaseSnapshotPolicyArguments.storage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableRegExps, storage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseSnapshotPolicyArguments {\n");
    
    sb.append("    tableRegExps: ").append(toIndentedString(tableRegExps)).append("\n");
    sb.append("    storage: ").append(toIndentedString(storage)).append("\n");
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

