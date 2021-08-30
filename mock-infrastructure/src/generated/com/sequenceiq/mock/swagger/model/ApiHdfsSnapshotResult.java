package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHdfsSnapshot;
import com.sequenceiq.mock.swagger.model.ApiHdfsSnapshotError;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Detailed information about an HDFS snapshot command.
 */
@ApiModel(description = "Detailed information about an HDFS snapshot command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsSnapshotResult   {
  @JsonProperty("processedPathCount")
  private Integer processedPathCount = null;

  @JsonProperty("processedPaths")
  @Valid
  private List<String> processedPaths = null;

  @JsonProperty("unprocessedPathCount")
  private Integer unprocessedPathCount = null;

  @JsonProperty("unprocessedPaths")
  @Valid
  private List<String> unprocessedPaths = null;

  @JsonProperty("createdSnapshotCount")
  private Integer createdSnapshotCount = null;

  @JsonProperty("createdSnapshots")
  @Valid
  private List<ApiHdfsSnapshot> createdSnapshots = null;

  @JsonProperty("deletedSnapshotCount")
  private Integer deletedSnapshotCount = null;

  @JsonProperty("deletedSnapshots")
  @Valid
  private List<ApiHdfsSnapshot> deletedSnapshots = null;

  @JsonProperty("creationErrorCount")
  private Integer creationErrorCount = null;

  @JsonProperty("creationErrors")
  @Valid
  private List<ApiHdfsSnapshotError> creationErrors = null;

  @JsonProperty("deletionErrorCount")
  private Integer deletionErrorCount = null;

  @JsonProperty("deletionErrors")
  @Valid
  private List<ApiHdfsSnapshotError> deletionErrors = null;

  public ApiHdfsSnapshotResult processedPathCount(Integer processedPathCount) {
    this.processedPathCount = processedPathCount;
    return this;
  }

  /**
   * Number of processed paths.
   * @return processedPathCount
  **/
  @ApiModelProperty(value = "Number of processed paths.")


  public Integer getProcessedPathCount() {
    return processedPathCount;
  }

  public void setProcessedPathCount(Integer processedPathCount) {
    this.processedPathCount = processedPathCount;
  }

  public ApiHdfsSnapshotResult processedPaths(List<String> processedPaths) {
    this.processedPaths = processedPaths;
    return this;
  }

  public ApiHdfsSnapshotResult addProcessedPathsItem(String processedPathsItem) {
    if (this.processedPaths == null) {
      this.processedPaths = new ArrayList<>();
    }
    this.processedPaths.add(processedPathsItem);
    return this;
  }

  /**
   * The list of processed paths. <p/> This is only available in the full view.
   * @return processedPaths
  **/
  @ApiModelProperty(value = "The list of processed paths. <p/> This is only available in the full view.")


  public List<String> getProcessedPaths() {
    return processedPaths;
  }

  public void setProcessedPaths(List<String> processedPaths) {
    this.processedPaths = processedPaths;
  }

  public ApiHdfsSnapshotResult unprocessedPathCount(Integer unprocessedPathCount) {
    this.unprocessedPathCount = unprocessedPathCount;
    return this;
  }

  /**
   * Number of unprocessed paths.
   * @return unprocessedPathCount
  **/
  @ApiModelProperty(value = "Number of unprocessed paths.")


  public Integer getUnprocessedPathCount() {
    return unprocessedPathCount;
  }

  public void setUnprocessedPathCount(Integer unprocessedPathCount) {
    this.unprocessedPathCount = unprocessedPathCount;
  }

  public ApiHdfsSnapshotResult unprocessedPaths(List<String> unprocessedPaths) {
    this.unprocessedPaths = unprocessedPaths;
    return this;
  }

  public ApiHdfsSnapshotResult addUnprocessedPathsItem(String unprocessedPathsItem) {
    if (this.unprocessedPaths == null) {
      this.unprocessedPaths = new ArrayList<>();
    }
    this.unprocessedPaths.add(unprocessedPathsItem);
    return this;
  }

  /**
   * The list of unprocessed paths. Note that paths that are currently being processed will also be included in this list. <p/> This is only available in the full view.
   * @return unprocessedPaths
  **/
  @ApiModelProperty(value = "The list of unprocessed paths. Note that paths that are currently being processed will also be included in this list. <p/> This is only available in the full view.")


  public List<String> getUnprocessedPaths() {
    return unprocessedPaths;
  }

  public void setUnprocessedPaths(List<String> unprocessedPaths) {
    this.unprocessedPaths = unprocessedPaths;
  }

  public ApiHdfsSnapshotResult createdSnapshotCount(Integer createdSnapshotCount) {
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

  public ApiHdfsSnapshotResult createdSnapshots(List<ApiHdfsSnapshot> createdSnapshots) {
    this.createdSnapshots = createdSnapshots;
    return this;
  }

  public ApiHdfsSnapshotResult addCreatedSnapshotsItem(ApiHdfsSnapshot createdSnapshotsItem) {
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

  public List<ApiHdfsSnapshot> getCreatedSnapshots() {
    return createdSnapshots;
  }

  public void setCreatedSnapshots(List<ApiHdfsSnapshot> createdSnapshots) {
    this.createdSnapshots = createdSnapshots;
  }

  public ApiHdfsSnapshotResult deletedSnapshotCount(Integer deletedSnapshotCount) {
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

  public ApiHdfsSnapshotResult deletedSnapshots(List<ApiHdfsSnapshot> deletedSnapshots) {
    this.deletedSnapshots = deletedSnapshots;
    return this;
  }

  public ApiHdfsSnapshotResult addDeletedSnapshotsItem(ApiHdfsSnapshot deletedSnapshotsItem) {
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

  public List<ApiHdfsSnapshot> getDeletedSnapshots() {
    return deletedSnapshots;
  }

  public void setDeletedSnapshots(List<ApiHdfsSnapshot> deletedSnapshots) {
    this.deletedSnapshots = deletedSnapshots;
  }

  public ApiHdfsSnapshotResult creationErrorCount(Integer creationErrorCount) {
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

  public ApiHdfsSnapshotResult creationErrors(List<ApiHdfsSnapshotError> creationErrors) {
    this.creationErrors = creationErrors;
    return this;
  }

  public ApiHdfsSnapshotResult addCreationErrorsItem(ApiHdfsSnapshotError creationErrorsItem) {
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

  public List<ApiHdfsSnapshotError> getCreationErrors() {
    return creationErrors;
  }

  public void setCreationErrors(List<ApiHdfsSnapshotError> creationErrors) {
    this.creationErrors = creationErrors;
  }

  public ApiHdfsSnapshotResult deletionErrorCount(Integer deletionErrorCount) {
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

  public ApiHdfsSnapshotResult deletionErrors(List<ApiHdfsSnapshotError> deletionErrors) {
    this.deletionErrors = deletionErrors;
    return this;
  }

  public ApiHdfsSnapshotResult addDeletionErrorsItem(ApiHdfsSnapshotError deletionErrorsItem) {
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

  public List<ApiHdfsSnapshotError> getDeletionErrors() {
    return deletionErrors;
  }

  public void setDeletionErrors(List<ApiHdfsSnapshotError> deletionErrors) {
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
    ApiHdfsSnapshotResult apiHdfsSnapshotResult = (ApiHdfsSnapshotResult) o;
    return Objects.equals(this.processedPathCount, apiHdfsSnapshotResult.processedPathCount) &&
        Objects.equals(this.processedPaths, apiHdfsSnapshotResult.processedPaths) &&
        Objects.equals(this.unprocessedPathCount, apiHdfsSnapshotResult.unprocessedPathCount) &&
        Objects.equals(this.unprocessedPaths, apiHdfsSnapshotResult.unprocessedPaths) &&
        Objects.equals(this.createdSnapshotCount, apiHdfsSnapshotResult.createdSnapshotCount) &&
        Objects.equals(this.createdSnapshots, apiHdfsSnapshotResult.createdSnapshots) &&
        Objects.equals(this.deletedSnapshotCount, apiHdfsSnapshotResult.deletedSnapshotCount) &&
        Objects.equals(this.deletedSnapshots, apiHdfsSnapshotResult.deletedSnapshots) &&
        Objects.equals(this.creationErrorCount, apiHdfsSnapshotResult.creationErrorCount) &&
        Objects.equals(this.creationErrors, apiHdfsSnapshotResult.creationErrors) &&
        Objects.equals(this.deletionErrorCount, apiHdfsSnapshotResult.deletionErrorCount) &&
        Objects.equals(this.deletionErrors, apiHdfsSnapshotResult.deletionErrors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(processedPathCount, processedPaths, unprocessedPathCount, unprocessedPaths, createdSnapshotCount, createdSnapshots, deletedSnapshotCount, deletedSnapshots, creationErrorCount, creationErrors, deletionErrorCount, deletionErrors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsSnapshotResult {\n");
    
    sb.append("    processedPathCount: ").append(toIndentedString(processedPathCount)).append("\n");
    sb.append("    processedPaths: ").append(toIndentedString(processedPaths)).append("\n");
    sb.append("    unprocessedPathCount: ").append(toIndentedString(unprocessedPathCount)).append("\n");
    sb.append("    unprocessedPaths: ").append(toIndentedString(unprocessedPaths)).append("\n");
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

