package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHBaseSnapshotPolicyArguments;
import com.sequenceiq.mock.swagger.model.ApiHdfsSnapshotPolicyArguments;
import com.sequenceiq.mock.swagger.model.ApiSnapshotCommand;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A snapshot policy. &lt;p/&gt; Snapshot policies have service specific arguments. This object has methods to retrieve arguments for all supported types of snapshots, but only one argument type is allowed to be set; the backend will check that the provided argument matches the type of the service with which the snapshot policy is associated.
 */
@ApiModel(description = "A snapshot policy. <p/> Snapshot policies have service specific arguments. This object has methods to retrieve arguments for all supported types of snapshots, but only one argument type is allowed to be set; the backend will check that the provided argument matches the type of the service with which the snapshot policy is associated.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiSnapshotPolicy   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("hourlySnapshots")
  private Integer hourlySnapshots = null;

  @JsonProperty("dailySnapshots")
  private Integer dailySnapshots = null;

  @JsonProperty("weeklySnapshots")
  private Integer weeklySnapshots = null;

  @JsonProperty("monthlySnapshots")
  private Integer monthlySnapshots = null;

  @JsonProperty("yearlySnapshots")
  private Integer yearlySnapshots = null;

  @JsonProperty("minuteOfHour")
  private Integer minuteOfHour = null;

  @JsonProperty("hoursForHourlySnapshots")
  @Valid
  private List<Integer> hoursForHourlySnapshots = null;

  @JsonProperty("hourOfDay")
  private Integer hourOfDay = null;

  @JsonProperty("dayOfWeek")
  private Integer dayOfWeek = null;

  @JsonProperty("dayOfMonth")
  private Integer dayOfMonth = null;

  @JsonProperty("monthOfYear")
  private Integer monthOfYear = null;

  @JsonProperty("alertOnStart")
  private Boolean alertOnStart = null;

  @JsonProperty("alertOnSuccess")
  private Boolean alertOnSuccess = null;

  @JsonProperty("alertOnFail")
  private Boolean alertOnFail = null;

  @JsonProperty("alertOnAbort")
  private Boolean alertOnAbort = null;

  @JsonProperty("hbaseArguments")
  private ApiHBaseSnapshotPolicyArguments hbaseArguments = null;

  @JsonProperty("hdfsArguments")
  private ApiHdfsSnapshotPolicyArguments hdfsArguments = null;

  @JsonProperty("lastCommand")
  private ApiSnapshotCommand lastCommand = null;

  @JsonProperty("lastSuccessfulCommand")
  private ApiSnapshotCommand lastSuccessfulCommand = null;

  @JsonProperty("paused")
  private Boolean paused = null;

  public ApiSnapshotPolicy name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Name of the snapshot policy.
   * @return name
  **/
  @ApiModelProperty(value = "Name of the snapshot policy.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiSnapshotPolicy description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Description of the snapshot policy.
   * @return description
  **/
  @ApiModelProperty(value = "Description of the snapshot policy.")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ApiSnapshotPolicy hourlySnapshots(Integer hourlySnapshots) {
    this.hourlySnapshots = hourlySnapshots;
    return this;
  }

  /**
   * Number of hourly snapshots to be retained. Defaults to 0
   * @return hourlySnapshots
  **/
  @ApiModelProperty(value = "Number of hourly snapshots to be retained. Defaults to 0")


  public Integer getHourlySnapshots() {
    return hourlySnapshots;
  }

  public void setHourlySnapshots(Integer hourlySnapshots) {
    this.hourlySnapshots = hourlySnapshots;
  }

  public ApiSnapshotPolicy dailySnapshots(Integer dailySnapshots) {
    this.dailySnapshots = dailySnapshots;
    return this;
  }

  /**
   * Number of daily snapshots to be retained. Defaults to 0
   * @return dailySnapshots
  **/
  @ApiModelProperty(value = "Number of daily snapshots to be retained. Defaults to 0")


  public Integer getDailySnapshots() {
    return dailySnapshots;
  }

  public void setDailySnapshots(Integer dailySnapshots) {
    this.dailySnapshots = dailySnapshots;
  }

  public ApiSnapshotPolicy weeklySnapshots(Integer weeklySnapshots) {
    this.weeklySnapshots = weeklySnapshots;
    return this;
  }

  /**
   * Number of weekly snapshots to be retained. Defaults to 0
   * @return weeklySnapshots
  **/
  @ApiModelProperty(value = "Number of weekly snapshots to be retained. Defaults to 0")


  public Integer getWeeklySnapshots() {
    return weeklySnapshots;
  }

  public void setWeeklySnapshots(Integer weeklySnapshots) {
    this.weeklySnapshots = weeklySnapshots;
  }

  public ApiSnapshotPolicy monthlySnapshots(Integer monthlySnapshots) {
    this.monthlySnapshots = monthlySnapshots;
    return this;
  }

  /**
   * Number of monthly snapshots to be retained. Defaults to 0
   * @return monthlySnapshots
  **/
  @ApiModelProperty(value = "Number of monthly snapshots to be retained. Defaults to 0")


  public Integer getMonthlySnapshots() {
    return monthlySnapshots;
  }

  public void setMonthlySnapshots(Integer monthlySnapshots) {
    this.monthlySnapshots = monthlySnapshots;
  }

  public ApiSnapshotPolicy yearlySnapshots(Integer yearlySnapshots) {
    this.yearlySnapshots = yearlySnapshots;
    return this;
  }

  /**
   * Number of yearly snapshots to be retained. Defaults to 0
   * @return yearlySnapshots
  **/
  @ApiModelProperty(value = "Number of yearly snapshots to be retained. Defaults to 0")


  public Integer getYearlySnapshots() {
    return yearlySnapshots;
  }

  public void setYearlySnapshots(Integer yearlySnapshots) {
    this.yearlySnapshots = yearlySnapshots;
  }

  public ApiSnapshotPolicy minuteOfHour(Integer minuteOfHour) {
    this.minuteOfHour = minuteOfHour;
    return this;
  }

  /**
   * Minute in the hour that hourly, daily, weekly, monthly and yearly snapshots should be created. Valid values are 0 to 59. Default value is 0.
   * @return minuteOfHour
  **/
  @ApiModelProperty(value = "Minute in the hour that hourly, daily, weekly, monthly and yearly snapshots should be created. Valid values are 0 to 59. Default value is 0.")


  public Integer getMinuteOfHour() {
    return minuteOfHour;
  }

  public void setMinuteOfHour(Integer minuteOfHour) {
    this.minuteOfHour = minuteOfHour;
  }

  public ApiSnapshotPolicy hoursForHourlySnapshots(List<Integer> hoursForHourlySnapshots) {
    this.hoursForHourlySnapshots = hoursForHourlySnapshots;
    return this;
  }

  public ApiSnapshotPolicy addHoursForHourlySnapshotsItem(Integer hoursForHourlySnapshotsItem) {
    if (this.hoursForHourlySnapshots == null) {
      this.hoursForHourlySnapshots = new ArrayList<>();
    }
    this.hoursForHourlySnapshots.add(hoursForHourlySnapshotsItem);
    return this;
  }

  /**
   * Hours of the day that hourly snapshots should be created. Valid values are 0 to 23. If this list is null or empty, then hourly snapshots are created for every hour.
   * @return hoursForHourlySnapshots
  **/
  @ApiModelProperty(value = "Hours of the day that hourly snapshots should be created. Valid values are 0 to 23. If this list is null or empty, then hourly snapshots are created for every hour.")


  public List<Integer> getHoursForHourlySnapshots() {
    return hoursForHourlySnapshots;
  }

  public void setHoursForHourlySnapshots(List<Integer> hoursForHourlySnapshots) {
    this.hoursForHourlySnapshots = hoursForHourlySnapshots;
  }

  public ApiSnapshotPolicy hourOfDay(Integer hourOfDay) {
    this.hourOfDay = hourOfDay;
    return this;
  }

  /**
   * Hour in the day that daily, weekly, monthly and yearly snapshots should be created. Valid values are 0 to 23. Default value is 0.
   * @return hourOfDay
  **/
  @ApiModelProperty(value = "Hour in the day that daily, weekly, monthly and yearly snapshots should be created. Valid values are 0 to 23. Default value is 0.")


  public Integer getHourOfDay() {
    return hourOfDay;
  }

  public void setHourOfDay(Integer hourOfDay) {
    this.hourOfDay = hourOfDay;
  }

  public ApiSnapshotPolicy dayOfWeek(Integer dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
    return this;
  }

  /**
   * Day of the week that weekly snapshots should be created. Valid values are 1 to 7, 1 representing Sunday. Default value is 1.
   * @return dayOfWeek
  **/
  @ApiModelProperty(value = "Day of the week that weekly snapshots should be created. Valid values are 1 to 7, 1 representing Sunday. Default value is 1.")


  public Integer getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(Integer dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public ApiSnapshotPolicy dayOfMonth(Integer dayOfMonth) {
    this.dayOfMonth = dayOfMonth;
    return this;
  }

  /**
   * Day of the month that monthly and yearly snapshots should be created. Values from 1 to 31 are allowed. Additionally 0 to -30 can be used to specify offsets from the last day of the month. Default value is 1. <p/> If this value is invalid for any month for which snapshots are required, the backend will throw an exception.
   * @return dayOfMonth
  **/
  @ApiModelProperty(value = "Day of the month that monthly and yearly snapshots should be created. Values from 1 to 31 are allowed. Additionally 0 to -30 can be used to specify offsets from the last day of the month. Default value is 1. <p/> If this value is invalid for any month for which snapshots are required, the backend will throw an exception.")


  public Integer getDayOfMonth() {
    return dayOfMonth;
  }

  public void setDayOfMonth(Integer dayOfMonth) {
    this.dayOfMonth = dayOfMonth;
  }

  public ApiSnapshotPolicy monthOfYear(Integer monthOfYear) {
    this.monthOfYear = monthOfYear;
    return this;
  }

  /**
   * Month of the year that yearly snapshots should be created. Valid values are 1 to 12, 1 representing January. Default value is 1.
   * @return monthOfYear
  **/
  @ApiModelProperty(value = "Month of the year that yearly snapshots should be created. Valid values are 1 to 12, 1 representing January. Default value is 1.")


  public Integer getMonthOfYear() {
    return monthOfYear;
  }

  public void setMonthOfYear(Integer monthOfYear) {
    this.monthOfYear = monthOfYear;
  }

  public ApiSnapshotPolicy alertOnStart(Boolean alertOnStart) {
    this.alertOnStart = alertOnStart;
    return this;
  }

  /**
   * Whether to alert on start of snapshot creation/deletion activity. Defaults to false
   * @return alertOnStart
  **/
  @ApiModelProperty(value = "Whether to alert on start of snapshot creation/deletion activity. Defaults to false")


  public Boolean isAlertOnStart() {
    return alertOnStart;
  }

  public void setAlertOnStart(Boolean alertOnStart) {
    this.alertOnStart = alertOnStart;
  }

  public ApiSnapshotPolicy alertOnSuccess(Boolean alertOnSuccess) {
    this.alertOnSuccess = alertOnSuccess;
    return this;
  }

  /**
   * Whether to alert on successful completion of snapshot creation/deletion activity. Defaults to false.
   * @return alertOnSuccess
  **/
  @ApiModelProperty(value = "Whether to alert on successful completion of snapshot creation/deletion activity. Defaults to false.")


  public Boolean isAlertOnSuccess() {
    return alertOnSuccess;
  }

  public void setAlertOnSuccess(Boolean alertOnSuccess) {
    this.alertOnSuccess = alertOnSuccess;
  }

  public ApiSnapshotPolicy alertOnFail(Boolean alertOnFail) {
    this.alertOnFail = alertOnFail;
    return this;
  }

  /**
   * Whether to alert on failure of snapshot creation/deletion activity. Defaults to false.
   * @return alertOnFail
  **/
  @ApiModelProperty(value = "Whether to alert on failure of snapshot creation/deletion activity. Defaults to false.")


  public Boolean isAlertOnFail() {
    return alertOnFail;
  }

  public void setAlertOnFail(Boolean alertOnFail) {
    this.alertOnFail = alertOnFail;
  }

  public ApiSnapshotPolicy alertOnAbort(Boolean alertOnAbort) {
    this.alertOnAbort = alertOnAbort;
    return this;
  }

  /**
   * Whether to alert on abort of snapshot creation/deletion activity. Defaults to false.
   * @return alertOnAbort
  **/
  @ApiModelProperty(value = "Whether to alert on abort of snapshot creation/deletion activity. Defaults to false.")


  public Boolean isAlertOnAbort() {
    return alertOnAbort;
  }

  public void setAlertOnAbort(Boolean alertOnAbort) {
    this.alertOnAbort = alertOnAbort;
  }

  public ApiSnapshotPolicy hbaseArguments(ApiHBaseSnapshotPolicyArguments hbaseArguments) {
    this.hbaseArguments = hbaseArguments;
    return this;
  }

  /**
   * Arguments specific to HBase snapshot policies.
   * @return hbaseArguments
  **/
  @ApiModelProperty(value = "Arguments specific to HBase snapshot policies.")

  @Valid

  public ApiHBaseSnapshotPolicyArguments getHbaseArguments() {
    return hbaseArguments;
  }

  public void setHbaseArguments(ApiHBaseSnapshotPolicyArguments hbaseArguments) {
    this.hbaseArguments = hbaseArguments;
  }

  public ApiSnapshotPolicy hdfsArguments(ApiHdfsSnapshotPolicyArguments hdfsArguments) {
    this.hdfsArguments = hdfsArguments;
    return this;
  }

  /**
   * Arguments specific to Hdfs snapshot policies.
   * @return hdfsArguments
  **/
  @ApiModelProperty(value = "Arguments specific to Hdfs snapshot policies.")

  @Valid

  public ApiHdfsSnapshotPolicyArguments getHdfsArguments() {
    return hdfsArguments;
  }

  public void setHdfsArguments(ApiHdfsSnapshotPolicyArguments hdfsArguments) {
    this.hdfsArguments = hdfsArguments;
  }

  public ApiSnapshotPolicy lastCommand(ApiSnapshotCommand lastCommand) {
    this.lastCommand = lastCommand;
    return this;
  }

  /**
   * Latest command of this policy. The command might still be active.
   * @return lastCommand
  **/
  @ApiModelProperty(value = "Latest command of this policy. The command might still be active.")

  @Valid

  public ApiSnapshotCommand getLastCommand() {
    return lastCommand;
  }

  public void setLastCommand(ApiSnapshotCommand lastCommand) {
    this.lastCommand = lastCommand;
  }

  public ApiSnapshotPolicy lastSuccessfulCommand(ApiSnapshotCommand lastSuccessfulCommand) {
    this.lastSuccessfulCommand = lastSuccessfulCommand;
    return this;
  }

  /**
   * Last successful command of this policy. Returns null if there has been no successful command.
   * @return lastSuccessfulCommand
  **/
  @ApiModelProperty(value = "Last successful command of this policy. Returns null if there has been no successful command.")

  @Valid

  public ApiSnapshotCommand getLastSuccessfulCommand() {
    return lastSuccessfulCommand;
  }

  public void setLastSuccessfulCommand(ApiSnapshotCommand lastSuccessfulCommand) {
    this.lastSuccessfulCommand = lastSuccessfulCommand;
  }

  public ApiSnapshotPolicy paused(Boolean paused) {
    this.paused = paused;
    return this;
  }

  /**
   * Whether to pause a snapshot policy, available since V11.
   * @return paused
  **/
  @ApiModelProperty(value = "Whether to pause a snapshot policy, available since V11.")


  public Boolean isPaused() {
    return paused;
  }

  public void setPaused(Boolean paused) {
    this.paused = paused;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiSnapshotPolicy apiSnapshotPolicy = (ApiSnapshotPolicy) o;
    return Objects.equals(this.name, apiSnapshotPolicy.name) &&
        Objects.equals(this.description, apiSnapshotPolicy.description) &&
        Objects.equals(this.hourlySnapshots, apiSnapshotPolicy.hourlySnapshots) &&
        Objects.equals(this.dailySnapshots, apiSnapshotPolicy.dailySnapshots) &&
        Objects.equals(this.weeklySnapshots, apiSnapshotPolicy.weeklySnapshots) &&
        Objects.equals(this.monthlySnapshots, apiSnapshotPolicy.monthlySnapshots) &&
        Objects.equals(this.yearlySnapshots, apiSnapshotPolicy.yearlySnapshots) &&
        Objects.equals(this.minuteOfHour, apiSnapshotPolicy.minuteOfHour) &&
        Objects.equals(this.hoursForHourlySnapshots, apiSnapshotPolicy.hoursForHourlySnapshots) &&
        Objects.equals(this.hourOfDay, apiSnapshotPolicy.hourOfDay) &&
        Objects.equals(this.dayOfWeek, apiSnapshotPolicy.dayOfWeek) &&
        Objects.equals(this.dayOfMonth, apiSnapshotPolicy.dayOfMonth) &&
        Objects.equals(this.monthOfYear, apiSnapshotPolicy.monthOfYear) &&
        Objects.equals(this.alertOnStart, apiSnapshotPolicy.alertOnStart) &&
        Objects.equals(this.alertOnSuccess, apiSnapshotPolicy.alertOnSuccess) &&
        Objects.equals(this.alertOnFail, apiSnapshotPolicy.alertOnFail) &&
        Objects.equals(this.alertOnAbort, apiSnapshotPolicy.alertOnAbort) &&
        Objects.equals(this.hbaseArguments, apiSnapshotPolicy.hbaseArguments) &&
        Objects.equals(this.hdfsArguments, apiSnapshotPolicy.hdfsArguments) &&
        Objects.equals(this.lastCommand, apiSnapshotPolicy.lastCommand) &&
        Objects.equals(this.lastSuccessfulCommand, apiSnapshotPolicy.lastSuccessfulCommand) &&
        Objects.equals(this.paused, apiSnapshotPolicy.paused);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, hourlySnapshots, dailySnapshots, weeklySnapshots, monthlySnapshots, yearlySnapshots, minuteOfHour, hoursForHourlySnapshots, hourOfDay, dayOfWeek, dayOfMonth, monthOfYear, alertOnStart, alertOnSuccess, alertOnFail, alertOnAbort, hbaseArguments, hdfsArguments, lastCommand, lastSuccessfulCommand, paused);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiSnapshotPolicy {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    hourlySnapshots: ").append(toIndentedString(hourlySnapshots)).append("\n");
    sb.append("    dailySnapshots: ").append(toIndentedString(dailySnapshots)).append("\n");
    sb.append("    weeklySnapshots: ").append(toIndentedString(weeklySnapshots)).append("\n");
    sb.append("    monthlySnapshots: ").append(toIndentedString(monthlySnapshots)).append("\n");
    sb.append("    yearlySnapshots: ").append(toIndentedString(yearlySnapshots)).append("\n");
    sb.append("    minuteOfHour: ").append(toIndentedString(minuteOfHour)).append("\n");
    sb.append("    hoursForHourlySnapshots: ").append(toIndentedString(hoursForHourlySnapshots)).append("\n");
    sb.append("    hourOfDay: ").append(toIndentedString(hourOfDay)).append("\n");
    sb.append("    dayOfWeek: ").append(toIndentedString(dayOfWeek)).append("\n");
    sb.append("    dayOfMonth: ").append(toIndentedString(dayOfMonth)).append("\n");
    sb.append("    monthOfYear: ").append(toIndentedString(monthOfYear)).append("\n");
    sb.append("    alertOnStart: ").append(toIndentedString(alertOnStart)).append("\n");
    sb.append("    alertOnSuccess: ").append(toIndentedString(alertOnSuccess)).append("\n");
    sb.append("    alertOnFail: ").append(toIndentedString(alertOnFail)).append("\n");
    sb.append("    alertOnAbort: ").append(toIndentedString(alertOnAbort)).append("\n");
    sb.append("    hbaseArguments: ").append(toIndentedString(hbaseArguments)).append("\n");
    sb.append("    hdfsArguments: ").append(toIndentedString(hdfsArguments)).append("\n");
    sb.append("    lastCommand: ").append(toIndentedString(lastCommand)).append("\n");
    sb.append("    lastSuccessfulCommand: ").append(toIndentedString(lastSuccessfulCommand)).append("\n");
    sb.append("    paused: ").append(toIndentedString(paused)).append("\n");
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

