package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Optional arguments for diagnostics collection.
 */
@ApiModel(description = "Optional arguments for diagnostics collection.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiReplicationDiagnosticsCollectionArgs   {
  @JsonProperty("commands")
  private ApiCommandList commands = null;

  @JsonProperty("ticketNumber")
  private String ticketNumber = null;

  @JsonProperty("comments")
  private String comments = null;

  @JsonProperty("phoneHome")
  private Boolean phoneHome = null;

  public ApiReplicationDiagnosticsCollectionArgs commands(ApiCommandList commands) {
    this.commands = commands;
    return this;
  }

  /**
   * Commands to limit diagnostics to. By default, the most recent 10 commands on the schedule will be used.
   * @return commands
  **/
  @ApiModelProperty(value = "Commands to limit diagnostics to. By default, the most recent 10 commands on the schedule will be used.")

  @Valid

  public ApiCommandList getCommands() {
    return commands;
  }

  public void setCommands(ApiCommandList commands) {
    this.commands = commands;
  }

  public ApiReplicationDiagnosticsCollectionArgs ticketNumber(String ticketNumber) {
    this.ticketNumber = ticketNumber;
    return this;
  }

  /**
   * Ticket number to which this bundle must be associated with.
   * @return ticketNumber
  **/
  @ApiModelProperty(value = "Ticket number to which this bundle must be associated with.")


  public String getTicketNumber() {
    return ticketNumber;
  }

  public void setTicketNumber(String ticketNumber) {
    this.ticketNumber = ticketNumber;
  }

  public ApiReplicationDiagnosticsCollectionArgs comments(String comments) {
    this.comments = comments;
    return this;
  }

  /**
   * Additional comments for the bundle.
   * @return comments
  **/
  @ApiModelProperty(value = "Additional comments for the bundle.")


  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public ApiReplicationDiagnosticsCollectionArgs phoneHome(Boolean phoneHome) {
    this.phoneHome = phoneHome;
    return this;
  }

  /**
   * Whether the diagnostics bundle must be uploaded to Cloudera.
   * @return phoneHome
  **/
  @ApiModelProperty(value = "Whether the diagnostics bundle must be uploaded to Cloudera.")


  public Boolean isPhoneHome() {
    return phoneHome;
  }

  public void setPhoneHome(Boolean phoneHome) {
    this.phoneHome = phoneHome;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiReplicationDiagnosticsCollectionArgs apiReplicationDiagnosticsCollectionArgs = (ApiReplicationDiagnosticsCollectionArgs) o;
    return Objects.equals(this.commands, apiReplicationDiagnosticsCollectionArgs.commands) &&
        Objects.equals(this.ticketNumber, apiReplicationDiagnosticsCollectionArgs.ticketNumber) &&
        Objects.equals(this.comments, apiReplicationDiagnosticsCollectionArgs.comments) &&
        Objects.equals(this.phoneHome, apiReplicationDiagnosticsCollectionArgs.phoneHome);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commands, ticketNumber, comments, phoneHome);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiReplicationDiagnosticsCollectionArgs {\n");
    
    sb.append("    commands: ").append(toIndentedString(commands)).append("\n");
    sb.append("    ticketNumber: ").append(toIndentedString(ticketNumber)).append("\n");
    sb.append("    comments: ").append(toIndentedString(comments)).append("\n");
    sb.append("    phoneHome: ").append(toIndentedString(phoneHome)).append("\n");
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

