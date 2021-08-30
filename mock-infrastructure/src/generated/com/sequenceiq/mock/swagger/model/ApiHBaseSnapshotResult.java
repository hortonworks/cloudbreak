package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHBaseSnapshot;
import com.sequenceiq.mock.swagger.model.ApiHBaseSnapshotError;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Detailed information about an HBase snapshot command.
 */
@ApiModel(description = "Detailed information about an HBase snapshot command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHBaseSnapshotResult   {
  @JsonProperty("processedTableCount")
  private Integer processedTableCount = null;

  @JsonProperty("processedTables")
  @Valid
  private List<String> processedTables = null;

  @JsonProperty("unprocessedTableCount")
  private Integer unprocessedTableCount = null;

  @JsonProperty("unprocessedTables")
  @Valid
  private List<String> unprocessedTables = null;

  @JsonProperty("createdSnapshotCount")
  private Integer createdSnapshotCount = null;

  @JsonProperty("createdSnapshots")
  @Valid
  private List<ApiHBaseSnapshot> createdSnapshots = null;

  @JsonProperty("deletedSnapshotCount")
  private Integer deletedSnapshotCount = null;

  @JsonProperty("deletedSnapshots")
  @Valid
  private List<ApiHBaseSnapshot> deletedSnapshots = null;

  @JsonProperty("creationErrorCount")
  private Integer creationErrorCount = null;

  @JsonProperty("creationErrors")
  @Valid
  private List<ApiHBaseSnapshotError> creationErrors = null;

  @JsonProperty("deletionErrorCount")
  private Integer deletionErrorCount = null;

  @JsonProperty("deletionErrors")
  @Valid
  private List<ApiHBaseSnapshotError> deletionErrors = null;

  public ApiHBaseSnapshotResult processedTableCount(Integer processedTableCount) {
    this.processedTableCount = processedTableCount;
    return this;
  }

  /**
   * Number of processed tables.
   * @return processedTableCount
  **/
  @ApiModelProperty(value = "Number of processed tables.")


  public Integer getProcessedTableCount() {
    return processedTableCount;
  }

  public void setProcessedTableCount(Integer processedTableCount) {
    this.processedTableCount = processedTableCount;
  }

  public ApiHBaseSnapshotResult processedTables(List<String> processedTables) {
    this.processedTables = processedTables;
    return this;
  }

  public ApiHBaseSnapshotResult addProcessedTablesItem(String processedTablesItem) {
    if (this.processedTables == null) {
      this.processedTables = new ArrayList<>();
    }
    this.processedTables.add(processedTablesItem);
    return this;
  }

  /**
   * The list of processed tables. <p/> This is only available in the full view.
   * @return processedTables
  **/
  @ApiModelProperty(value = "The list of processed tables. <p/> This is only available in the full view.")


  public List<String> getProcessedTables() {
    return processedTables;
  }

  public void setProcessedTables(List<String> processedTables) {
    this.processedTables = processedTables;
  }

  public ApiHBaseSnapshotResult unprocessedTableCount(Integer unprocessedTableCount) {
    this.unprocessedTableCount = unprocessedTableCount;
    return this;
  }

  /**
   * Number of unprocessed tables.
   * @return unprocessedTableCount
  **/
  @ApiModelProperty(value = "Number of unprocessed tables.")


  public Integer getUnprocessedTableCount() {
    return unprocessedTableCount;
  }

  public void setUnprocessedTableCount(Integer unprocessedTableCount) {
    this.unprocessedTableCount = unprocessedTableCount;
  }

  public ApiHBaseSnapshotResult unprocessedTables(List<String> unprocessedTables) {
    this.unprocessedTables = unprocessedTables;
    return this;
  }

  public ApiHBaseSnapshotResult addUnprocessedTablesItem(String unprocessedTablesItem) {
    if (this.unprocessedTables == null) {
      this.unprocessedTables = new ArrayList<>();
    }
    this.unprocessedTables.add(unprocessedTablesItem);
    return this;
  }

  /**
   * The list of unprocessed tables. Note that tables that are currently being processed will also be included in this list. <p/> This is only available in the full view.
   * @return unprocessedTables
  **/
  @ApiModelProperty(value = "The list of unprocessed tables. Note that tables that are currently being processed will also be included in this list. <p/> This is only available in the full view.")


  public List<String> getUnprocessedTables() {
    return unprocessedTables;
  }

  public void setUnprocessedTables(List<String> unprocessedTables) {
    this.unprocessedTables = unprocessedTables;
  }

  public ApiHBaseSnapshotResult createdSnapshotCount(Integer createdSnapshotCount) {
    this.createdSnapshotCount = createdSnapshotCount;
    return this;
  }

  /**
   * Number of snapshots created.
   * @return createdSnapshotCount
  **/
  @ApiModelProperty(value = "Number of snapshots created.")


  public Integer getCreatedSnapshotCount() {
    return createdSnapshotCount;
  }

  public void setCreatedSnapshotCount(Integer createdSnapshotCount) {
    this.createdSnapshotCount = createdSnapshotCount;
  }

  public ApiHBaseSnapshotResult createdSnapshots(List<ApiHBaseSnapshot> createdSnapshots) {
    this.createdSnapshots = createdSnapshots;
    return this;
  }

  public ApiHBaseSnapshotResult addCreatedSnapshotsItem(ApiHBaseSnapshot createdSnapshotsItem) {
    if (this.createdSnapshots == null) {
      this.createdSnapshots = new ArrayList<>();
    }
    this.createdSnapshots.add(createdSnapshotsItem);
    return this;
  }

  /**
   * List of snapshots created. <p/> This is only available in the full view.
   * @return createdSnapshots
  **/
  @ApiModelProperty(value = "List of snapshots created. <p/> This is only available in the full view.")

  @Valid

  public List<ApiHBaseSnapshot> getCreatedSnapshots() {
    return createdSnapshots;
  }

  public void setCreatedSnapshots(List<ApiHBaseSnapshot> createdSnapshots) {
    this.createdSnapshots = createdSnapshots;
  }

  public ApiHBaseSnapshotResult deletedSnapshotCount(Integer deletedSnapshotCount) {
    this.deletedSnapshotCount = deletedSnapshotCount;
    return this;
  }

  /**
   * Number of snapshots deleted.
   * @return deletedSnapshotCount
  **/
  @ApiModelProperty(value = "Number of snapshots deleted.")


  public Integer getDeletedSnapshotCount() {
    return deletedSnapshotCount;
  }

  public void setDeletedSnapshotCount(Integer deletedSnapshotCount) {
    this.deletedSnapshotCount = deletedSnapshotCount;
  }

  public ApiHBaseSnapshotResult deletedSnapshots(List<ApiHBaseSnapshot> deletedSnapshots) {
    this.deletedSnapshots = deletedSnapshots;
    return this;
  }

  public ApiHBaseSnapshotResult addDeletedSnapshotsItem(ApiHBaseSnapshot deletedSnapshotsItem) {
    if (this.deletedSnapshots == null) {
      this.deletedSnapshots = new ArrayList<>();
    }
    this.deletedSnapshots.add(deletedSnapshotsItem);
    return this;
  }

  /**
   * List of snapshots deleted. <p/> This is only available in the full view.
   * @return deletedSnapshots
  **/
  @ApiModelProperty(value = "List of snapshots deleted. <p/> This is only available in the full view.")

  @Valid

  public List<ApiHBaseSnapshot> getDeletedSnapshots() {
    return deletedSnapshots;
  }

  public void setDeletedSnapshots(List<ApiHBaseSnapshot> deletedSnapshots) {
    this.deletedSnapshots = deletedSnapshots;
  }

  public ApiHBaseSnapshotResult creationErrorCount(Integer creationErrorCount) {
    this.creationErrorCount = creationErrorCount;
    return this;
  }

  /**
   * Number of errors detected when creating snapshots.
   * @return creationErrorCount
  **/
  @ApiModelProperty(value = "Number of errors detected when creating snapshots.")


  public Integer getCreationErrorCount() {
    return creationErrorCount;
  }

  public void setCreationErrorCount(Integer creationErrorCount) {
    this.creationErrorCount = creationErrorCount;
  }

  public ApiHBaseSnapshotResult creationErrors(List<ApiHBaseSnapshotError> creationErrors) {
    this.creationErrors = creationErrors;
    return this;
  }

  public ApiHBaseSnapshotResult addCreationErrorsItem(ApiHBaseSnapshotError creationErrorsItem) {
    if (this.creationErrors == null) {
      this.creationErrors = new ArrayList<>();
    }
    this.creationErrors.add(creationErrorsItem);
    return this;
  }

  /**
   * List of errors encountered when creating snapshots. <p/> This is only available in the full view.
   * @return creationErrors
  **/
  @ApiModelProperty(value = "List of errors encountered when creating snapshots. <p/> This is only available in the full view.")

  @Valid

  public List<ApiHBaseSnapshotError> getCreationErrors() {
    return creationErrors;
  }

  public void setCreationErrors(List<ApiHBaseSnapshotError> creationErrors) {
    this.creationErrors = creationErrors;
  }

  public ApiHBaseSnapshotResult deletionErrorCount(Integer deletionErrorCount) {
    this.deletionErrorCount = deletionErrorCount;
    return this;
  }

  /**
   * Number of errors detected when deleting snapshots.
   * @return deletionErrorCount
  **/
  @ApiModelProperty(value = "Number of errors detected when deleting snapshots.")


  public Integer getDeletionErrorCount() {
    return deletionErrorCount;
  }

  public void setDeletionErrorCount(Integer deletionErrorCount) {
    this.deletionErrorCount = deletionErrorCount;
  }

  public ApiHBaseSnapshotResult deletionErrors(List<ApiHBaseSnapshotError> deletionErrors) {
    this.deletionErrors = deletionErrors;
    return this;
  }

  public ApiHBaseSnapshotResult addDeletionErrorsItem(ApiHBaseSnapshotError deletionErrorsItem) {
    if (this.deletionErrors == null) {
      this.deletionErrors = new ArrayList<>();
    }
    this.deletionErrors.add(deletionErrorsItem);
    return this;
  }

  /**
   * List of errors encountered when deleting snapshots. <p/> This is only available in the full view.
   * @return deletionErrors
  **/
  @ApiModelProperty(value = "List of errors encountered when deleting snapshots. <p/> This is only available in the full view.")

  @Valid

  public List<ApiHBaseSnapshotError> getDeletionErrors() {
    return deletionErrors;
  }

  public void setDeletionErrors(List<ApiHBaseSnapshotError> deletionErrors) {
    this.deletionErrors = deletionErrors;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHBaseSnapshotResult apiHBaseSnapshotResult = (ApiHBaseSnapshotResult) o;
    return Objects.equals(this.processedTableCount, apiHBaseSnapshotResult.processedTableCount) &&
        Objects.equals(this.processedTables, apiHBaseSnapshotResult.processedTables) &&
        Objects.equals(this.unprocessedTableCount, apiHBaseSnapshotResult.unprocessedTableCount) &&
        Objects.equals(this.unprocessedTables, apiHBaseSnapshotResult.unprocessedTables) &&
        Objects.equals(this.createdSnapshotCount, apiHBaseSnapshotResult.createdSnapshotCount) &&
        Objects.equals(this.createdSnapshots, apiHBaseSnapshotResult.createdSnapshots) &&
        Objects.equals(this.deletedSnapshotCount, apiHBaseSnapshotResult.deletedSnapshotCount) &&
        Objects.equals(this.deletedSnapshots, apiHBaseSnapshotResult.deletedSnapshots) &&
        Objects.equals(this.creationErrorCount, apiHBaseSnapshotResult.creationErrorCount) &&
        Objects.equals(this.creationErrors, apiHBaseSnapshotResult.creationErrors) &&
        Objects.equals(this.deletionErrorCount, apiHBaseSnapshotResult.deletionErrorCount) &&
        Objects.equals(this.deletionErrors, apiHBaseSnapshotResult.deletionErrors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(processedTableCount, processedTables, unprocessedTableCount, unprocessedTables, createdSnapshotCount, createdSnapshots, deletedSnapshotCount, deletedSnapshots, creationErrorCount, creationErrors, deletionErrorCount, deletionErrors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseSnapshotResult {\n");
    
    sb.append("    processedTableCount: ").append(toIndentedString(processedTableCount)).append("\n");
    sb.append("    processedTables: ").append(toIndentedString(processedTables)).append("\n");
    sb.append("    unprocessedTableCount: ").append(toIndentedString(unprocessedTableCount)).append("\n");
    sb.append("    unprocessedTables: ").append(toIndentedString(unprocessedTables)).append("\n");
    sb.append("    createdSnapshotCount: ").append(toIndentedString(createdSnapshotCount)).append("\n");
    sb.append("    createdSnapshots: ").append(toIndentedString(createdSnapshots)).append("\n");
    sb.append("    deletedSnapshotCount: ").append(toIndentedString(deletedSnapshotCount)).append("\n");
    sb.append("    deletedSnapshots: ").append(toIndentedString(deletedSnapshots)).append("\n");
    sb.append("    creationErrorCount: ").append(toIndentedString(creationErrorCount)).append("\n");
    sb.append("    creationErrors: ").append(toIndentedString(creationErrors)).append("\n");
    sb.append("    deletionErrorCount: ").append(toIndentedString(deletionErrorCount)).append("\n");
    sb.append("    deletionErrors: ").append(toIndentedString(deletionErrors)).append("\n");
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

