package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for collecting diagnostics data for Yarn applications
 */
@ApiModel(description = "Arguments used for collecting diagnostics data for Yarn applications")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiYarnApplicationDiagnosticsCollectionArgs   {
  @JsonProperty("applicationIds")
  @Valid
  private List<String> applicationIds = null;

  @JsonProperty("ticketNumber")
  private String ticketNumber = null;

  @JsonProperty("comments")
  private String comments = null;

  public ApiYarnApplicationDiagnosticsCollectionArgs applicationIds(List<String> applicationIds) {
    this.applicationIds = applicationIds;
    return this;
  }

  public ApiYarnApplicationDiagnosticsCollectionArgs addApplicationIdsItem(String applicationIdsItem) {
    if (this.applicationIds == null) {
      this.applicationIds = new ArrayList<>();
    }
    this.applicationIds.add(applicationIdsItem);
    return this;
  }

  /**
   * Id's of the applications whose diagnostics data has to be collected
   * @return applicationIds
  **/
  @ApiModelProperty(value = "Id's of the applications whose diagnostics data has to be collected")


  public List<String> getApplicationIds() {
    return applicationIds;
  }

  public void setApplicationIds(List<String> applicationIds) {
    this.applicationIds = applicationIds;
  }

  public ApiYarnApplicationDiagnosticsCollectionArgs ticketNumber(String ticketNumber) {
    this.ticketNumber = ticketNumber;
    return this;
  }

  /**
   * Ticket Number of the Cloudera Support Ticket
   * @return ticketNumber
  **/
  @ApiModelProperty(value = "Ticket Number of the Cloudera Support Ticket")


  public String getTicketNumber() {
    return ticketNumber;
  }

  public void setTicketNumber(String ticketNumber) {
    this.ticketNumber = ticketNumber;
  }

  public ApiYarnApplicationDiagnosticsCollectionArgs comments(String comments) {
    this.comments = comments;
    return this;
  }

  /**
   * Comments to add to the support bundle
   * @return comments
  **/
  @ApiModelProperty(value = "Comments to add to the support bundle")


  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiYarnApplicationDiagnosticsCollectionArgs apiYarnApplicationDiagnosticsCollectionArgs = (ApiYarnApplicationDiagnosticsCollectionArgs) o;
    return Objects.equals(this.applicationIds, apiYarnApplicationDiagnosticsCollectionArgs.applicationIds) &&
        Objects.equals(this.ticketNumber, apiYarnApplicationDiagnosticsCollectionArgs.ticketNumber) &&
        Objects.equals(this.comments, apiYarnApplicationDiagnosticsCollectionArgs.comments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(applicationIds, ticketNumber, comments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiYarnApplicationDiagnosticsCollectionArgs {\n");
    
    sb.append("    applicationIds: ").append(toIndentedString(applicationIds)).append("\n");
    sb.append("    ticketNumber: ").append(toIndentedString(ticketNumber)).append("\n");
    sb.append("    comments: ").append(toIndentedString(comments)).append("\n");
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

