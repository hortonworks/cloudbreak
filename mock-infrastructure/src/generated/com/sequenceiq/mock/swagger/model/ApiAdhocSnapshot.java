package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiAdhocHBaseSnapshot;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * An adhoc snapshot descriptor.
 */
@ApiModel(description = "An adhoc snapshot descriptor.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiAdhocSnapshot   {
  @JsonProperty("snapshotName")
  private String snapshotName = null;

  @JsonProperty("hbaseSnapshot")
  private ApiAdhocHBaseSnapshot hbaseSnapshot = null;

  public ApiAdhocSnapshot snapshotName(String snapshotName) {
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

  public ApiAdhocSnapshot hbaseSnapshot(ApiAdhocHBaseSnapshot hbaseSnapshot) {
    this.hbaseSnapshot = hbaseSnapshot;
    return this;
  }

  /**
   * 
   * @return hbaseSnapshot
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiAdhocHBaseSnapshot getHbaseSnapshot() {
    return hbaseSnapshot;
  }

  public void setHbaseSnapshot(ApiAdhocHBaseSnapshot hbaseSnapshot) {
    this.hbaseSnapshot = hbaseSnapshot;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiAdhocSnapshot apiAdhocSnapshot = (ApiAdhocSnapshot) o;
    return Objects.equals(this.snapshotName, apiAdhocSnapshot.snapshotName) &&
        Objects.equals(this.hbaseSnapshot, apiAdhocSnapshot.hbaseSnapshot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(snapshotName, hbaseSnapshot);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiAdhocSnapshot {\n");
    
    sb.append("    snapshotName: ").append(toIndentedString(snapshotName)).append("\n");
    sb.append("    hbaseSnapshot: ").append(toIndentedString(hbaseSnapshot)).append("\n");
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

