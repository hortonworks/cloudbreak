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
 * Arguments used for Rolling Restart commands.
 */
@ApiModel(description = "Arguments used for Rolling Restart commands.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiImpalaRoleDiagnosticsArgs   {
  @JsonProperty("ticketNumber")
  private String ticketNumber = null;

  @JsonProperty("comments")
  private String comments = null;

  @JsonProperty("stacksCount")
  private Integer stacksCount = null;

  @JsonProperty("stacksIntervalSeconds")
  private Integer stacksIntervalSeconds = null;

  @JsonProperty("jmap")
  private Boolean jmap = null;

  @JsonProperty("gcore")
  private Boolean gcore = null;

  @JsonProperty("minidumpsCount")
  private Integer minidumpsCount = null;

  @JsonProperty("minidumpsIntervalSeconds")
  private Integer minidumpsIntervalSeconds = null;

  @JsonProperty("phoneHome")
  private Boolean phoneHome = null;

  public ApiImpalaRoleDiagnosticsArgs ticketNumber(String ticketNumber) {
    this.ticketNumber = ticketNumber;
    return this;
  }

  /**
   * The support ticket number to attach to this data collection.
   * @return ticketNumber
  **/
  @ApiModelProperty(value = "The support ticket number to attach to this data collection.")


  public String getTicketNumber() {
    return ticketNumber;
  }

  public void setTicketNumber(String ticketNumber) {
    this.ticketNumber = ticketNumber;
  }

  public ApiImpalaRoleDiagnosticsArgs comments(String comments) {
    this.comments = comments;
    return this;
  }

  /**
   * Comments to include with this data collection.
   * @return comments
  **/
  @ApiModelProperty(value = "Comments to include with this data collection.")


  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public ApiImpalaRoleDiagnosticsArgs stacksCount(Integer stacksCount) {
    this.stacksCount = stacksCount;
    return this;
  }

  /**
   * 
   * @return stacksCount
  **/
  @ApiModelProperty(value = "")


  public Integer getStacksCount() {
    return stacksCount;
  }

  public void setStacksCount(Integer stacksCount) {
    this.stacksCount = stacksCount;
  }

  public ApiImpalaRoleDiagnosticsArgs stacksIntervalSeconds(Integer stacksIntervalSeconds) {
    this.stacksIntervalSeconds = stacksIntervalSeconds;
    return this;
  }

  /**
   * Interval between stack collections. Defaults to 0
   * @return stacksIntervalSeconds
  **/
  @ApiModelProperty(value = "Interval between stack collections. Defaults to 0")


  public Integer getStacksIntervalSeconds() {
    return stacksIntervalSeconds;
  }

  public void setStacksIntervalSeconds(Integer stacksIntervalSeconds) {
    this.stacksIntervalSeconds = stacksIntervalSeconds;
  }

  public ApiImpalaRoleDiagnosticsArgs jmap(Boolean jmap) {
    this.jmap = jmap;
    return this;
  }

  /**
   * 
   * @return jmap
  **/
  @ApiModelProperty(value = "")


  public Boolean isJmap() {
    return jmap;
  }

  public void setJmap(Boolean jmap) {
    this.jmap = jmap;
  }

  public ApiImpalaRoleDiagnosticsArgs gcore(Boolean gcore) {
    this.gcore = gcore;
    return this;
  }

  /**
   * 
   * @return gcore
  **/
  @ApiModelProperty(value = "")


  public Boolean isGcore() {
    return gcore;
  }

  public void setGcore(Boolean gcore) {
    this.gcore = gcore;
  }

  public ApiImpalaRoleDiagnosticsArgs minidumpsCount(Integer minidumpsCount) {
    this.minidumpsCount = minidumpsCount;
    return this;
  }

  /**
   * 
   * @return minidumpsCount
  **/
  @ApiModelProperty(value = "")


  public Integer getMinidumpsCount() {
    return minidumpsCount;
  }

  public void setMinidumpsCount(Integer minidumpsCount) {
    this.minidumpsCount = minidumpsCount;
  }

  public ApiImpalaRoleDiagnosticsArgs minidumpsIntervalSeconds(Integer minidumpsIntervalSeconds) {
    this.minidumpsIntervalSeconds = minidumpsIntervalSeconds;
    return this;
  }

  /**
   * 
   * @return minidumpsIntervalSeconds
  **/
  @ApiModelProperty(value = "")


  public Integer getMinidumpsIntervalSeconds() {
    return minidumpsIntervalSeconds;
  }

  public void setMinidumpsIntervalSeconds(Integer minidumpsIntervalSeconds) {
    this.minidumpsIntervalSeconds = minidumpsIntervalSeconds;
  }

  public ApiImpalaRoleDiagnosticsArgs phoneHome(Boolean phoneHome) {
    this.phoneHome = phoneHome;
    return this;
  }

  /**
   * 
   * @return phoneHome
  **/
  @ApiModelProperty(value = "")


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
    ApiImpalaRoleDiagnosticsArgs apiImpalaRoleDiagnosticsArgs = (ApiImpalaRoleDiagnosticsArgs) o;
    return Objects.equals(this.ticketNumber, apiImpalaRoleDiagnosticsArgs.ticketNumber) &&
        Objects.equals(this.comments, apiImpalaRoleDiagnosticsArgs.comments) &&
        Objects.equals(this.stacksCount, apiImpalaRoleDiagnosticsArgs.stacksCount) &&
        Objects.equals(this.stacksIntervalSeconds, apiImpalaRoleDiagnosticsArgs.stacksIntervalSeconds) &&
        Objects.equals(this.jmap, apiImpalaRoleDiagnosticsArgs.jmap) &&
        Objects.equals(this.gcore, apiImpalaRoleDiagnosticsArgs.gcore) &&
        Objects.equals(this.minidumpsCount, apiImpalaRoleDiagnosticsArgs.minidumpsCount) &&
        Objects.equals(this.minidumpsIntervalSeconds, apiImpalaRoleDiagnosticsArgs.minidumpsIntervalSeconds) &&
        Objects.equals(this.phoneHome, apiImpalaRoleDiagnosticsArgs.phoneHome);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticketNumber, comments, stacksCount, stacksIntervalSeconds, jmap, gcore, minidumpsCount, minidumpsIntervalSeconds, phoneHome);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaRoleDiagnosticsArgs {\n");
    
    sb.append("    ticketNumber: ").append(toIndentedString(ticketNumber)).append("\n");
    sb.append("    comments: ").append(toIndentedString(comments)).append("\n");
    sb.append("    stacksCount: ").append(toIndentedString(stacksCount)).append("\n");
    sb.append("    stacksIntervalSeconds: ").append(toIndentedString(stacksIntervalSeconds)).append("\n");
    sb.append("    jmap: ").append(toIndentedString(jmap)).append("\n");
    sb.append("    gcore: ").append(toIndentedString(gcore)).append("\n");
    sb.append("    minidumpsCount: ").append(toIndentedString(minidumpsCount)).append("\n");
    sb.append("    minidumpsIntervalSeconds: ").append(toIndentedString(minidumpsIntervalSeconds)).append("\n");
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

