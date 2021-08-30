package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiScheduleInterval;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Base class for commands that can be scheduled in Cloudera Manager. &lt;p/&gt; Note that schedule IDs are not preserved upon import. &lt;p/&gt;
 */
@ApiModel(description = "Base class for commands that can be scheduled in Cloudera Manager. <p/> Note that schedule IDs are not preserved upon import. <p/>")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiSchedule   {
  @JsonProperty("id")
  private Integer id = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("startTime")
  private String startTime = null;

  @JsonProperty("endTime")
  private String endTime = null;

  @JsonProperty("interval")
  private Integer interval = null;

  @JsonProperty("intervalUnit")
  private ApiScheduleInterval intervalUnit = null;

  @JsonProperty("nextRun")
  private String nextRun = null;

  @JsonProperty("paused")
  private Boolean paused = null;

  @JsonProperty("alertOnStart")
  private Boolean alertOnStart = null;

  @JsonProperty("alertOnSuccess")
  private Boolean alertOnSuccess = null;

  @JsonProperty("alertOnFail")
  private Boolean alertOnFail = null;

  @JsonProperty("alertOnAbort")
  private Boolean alertOnAbort = null;

  public ApiSchedule id(Integer id) {
    this.id = id;
    return this;
  }

  /**
   * The schedule id.
   * @return id
  **/
  @ApiModelProperty(value = "The schedule id.")


  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public ApiSchedule displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * The schedule display name.
   * @return displayName
  **/
  @ApiModelProperty(value = "The schedule display name.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiSchedule description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The schedule description.
   * @return description
  **/
  @ApiModelProperty(value = "The schedule description.")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ApiSchedule startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * The time at which the scheduled activity is triggered for the first time.
   * @return startTime
  **/
  @ApiModelProperty(value = "The time at which the scheduled activity is triggered for the first time.")


  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public ApiSchedule endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * The time after which the scheduled activity will no longer be triggered.
   * @return endTime
  **/
  @ApiModelProperty(value = "The time after which the scheduled activity will no longer be triggered.")


  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public ApiSchedule interval(Integer interval) {
    this.interval = interval;
    return this;
  }

  /**
   * The duration between consecutive triggers of a scheduled activity. Defaults to 0.
   * @return interval
  **/
  @ApiModelProperty(value = "The duration between consecutive triggers of a scheduled activity. Defaults to 0.")


  public Integer getInterval() {
    return interval;
  }

  public void setInterval(Integer interval) {
    this.interval = interval;
  }

  public ApiSchedule intervalUnit(ApiScheduleInterval intervalUnit) {
    this.intervalUnit = intervalUnit;
    return this;
  }

  /**
   * The unit for the repeat interval.
   * @return intervalUnit
  **/
  @ApiModelProperty(value = "The unit for the repeat interval.")

  @Valid

  public ApiScheduleInterval getIntervalUnit() {
    return intervalUnit;
  }

  public void setIntervalUnit(ApiScheduleInterval intervalUnit) {
    this.intervalUnit = intervalUnit;
  }

  public ApiSchedule nextRun(String nextRun) {
    this.nextRun = nextRun;
    return this;
  }

  /**
   * Readonly. The time the scheduled command will run next.
   * @return nextRun
  **/
  @ApiModelProperty(value = "Readonly. The time the scheduled command will run next.")


  public String getNextRun() {
    return nextRun;
  }

  public void setNextRun(String nextRun) {
    this.nextRun = nextRun;
  }

  public ApiSchedule paused(Boolean paused) {
    this.paused = paused;
    return this;
  }

  /**
   * The paused state for the schedule. The scheduled activity will not be triggered as long as the scheduled is paused. Defaults to false.
   * @return paused
  **/
  @ApiModelProperty(value = "The paused state for the schedule. The scheduled activity will not be triggered as long as the scheduled is paused. Defaults to false.")


  public Boolean isPaused() {
    return paused;
  }

  public void setPaused(Boolean paused) {
    this.paused = paused;
  }

  public ApiSchedule alertOnStart(Boolean alertOnStart) {
    this.alertOnStart = alertOnStart;
    return this;
  }

  /**
   * Whether to alert on start of the scheduled activity. Defaults to false.
   * @return alertOnStart
  **/
  @ApiModelProperty(value = "Whether to alert on start of the scheduled activity. Defaults to false.")


  public Boolean isAlertOnStart() {
    return alertOnStart;
  }

  public void setAlertOnStart(Boolean alertOnStart) {
    this.alertOnStart = alertOnStart;
  }

  public ApiSchedule alertOnSuccess(Boolean alertOnSuccess) {
    this.alertOnSuccess = alertOnSuccess;
    return this;
  }

  /**
   * Whether to alert on successful completion of the scheduled activity. Defaults to false.
   * @return alertOnSuccess
  **/
  @ApiModelProperty(value = "Whether to alert on successful completion of the scheduled activity. Defaults to false.")


  public Boolean isAlertOnSuccess() {
    return alertOnSuccess;
  }

  public void setAlertOnSuccess(Boolean alertOnSuccess) {
    this.alertOnSuccess = alertOnSuccess;
  }

  public ApiSchedule alertOnFail(Boolean alertOnFail) {
    this.alertOnFail = alertOnFail;
    return this;
  }

  /**
   * Whether to alert on failure of the scheduled activity. Defaults to false.
   * @return alertOnFail
  **/
  @ApiModelProperty(value = "Whether to alert on failure of the scheduled activity. Defaults to false.")


  public Boolean isAlertOnFail() {
    return alertOnFail;
  }

  public void setAlertOnFail(Boolean alertOnFail) {
    this.alertOnFail = alertOnFail;
  }

  public ApiSchedule alertOnAbort(Boolean alertOnAbort) {
    this.alertOnAbort = alertOnAbort;
    return this;
  }

  /**
   * Whether to alert on abort of the scheduled activity. Defaults to false.
   * @return alertOnAbort
  **/
  @ApiModelProperty(value = "Whether to alert on abort of the scheduled activity. Defaults to false.")


  public Boolean isAlertOnAbort() {
    return alertOnAbort;
  }

  public void setAlertOnAbort(Boolean alertOnAbort) {
    this.alertOnAbort = alertOnAbort;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiSchedule apiSchedule = (ApiSchedule) o;
    return Objects.equals(this.id, apiSchedule.id) &&
        Objects.equals(this.displayName, apiSchedule.displayName) &&
        Objects.equals(this.description, apiSchedule.description) &&
        Objects.equals(this.startTime, apiSchedule.startTime) &&
        Objects.equals(this.endTime, apiSchedule.endTime) &&
        Objects.equals(this.interval, apiSchedule.interval) &&
        Objects.equals(this.intervalUnit, apiSchedule.intervalUnit) &&
        Objects.equals(this.nextRun, apiSchedule.nextRun) &&
        Objects.equals(this.paused, apiSchedule.paused) &&
        Objects.equals(this.alertOnStart, apiSchedule.alertOnStart) &&
        Objects.equals(this.alertOnSuccess, apiSchedule.alertOnSuccess) &&
        Objects.equals(this.alertOnFail, apiSchedule.alertOnFail) &&
        Objects.equals(this.alertOnAbort, apiSchedule.alertOnAbort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, displayName, description, startTime, endTime, interval, intervalUnit, nextRun, paused, alertOnStart, alertOnSuccess, alertOnFail, alertOnAbort);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiSchedule {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    interval: ").append(toIndentedString(interval)).append("\n");
    sb.append("    intervalUnit: ").append(toIndentedString(intervalUnit)).append("\n");
    sb.append("    nextRun: ").append(toIndentedString(nextRun)).append("\n");
    sb.append("    paused: ").append(toIndentedString(paused)).append("\n");
    sb.append("    alertOnStart: ").append(toIndentedString(alertOnStart)).append("\n");
    sb.append("    alertOnSuccess: ").append(toIndentedString(alertOnSuccess)).append("\n");
    sb.append("    alertOnFail: ").append(toIndentedString(alertOnFail)).append("\n");
    sb.append("    alertOnAbort: ").append(toIndentedString(alertOnAbort)).append("\n");
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

