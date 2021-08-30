package com.sequenceiq.mock.swagger.model;

import java.util.Objects;

import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a user activity, such as a MapReduce job, a Hive query, an Oozie workflow, etc.
 */
@ApiModel(description = "Represents a user activity, such as a MapReduce job, a Hive query, an Oozie workflow, etc.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiActivity   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("type")
  private ApiActivityType type = null;

  @JsonProperty("parent")
  private String parent = null;

  @JsonProperty("startTime")
  private String startTime = null;

  @JsonProperty("finishTime")
  private String finishTime = null;

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("status")
  private ApiActivityStatus status = null;

  @JsonProperty("user")
  private String user = null;

  @JsonProperty("group")
  private String group = null;

  @JsonProperty("inputDir")
  private String inputDir = null;

  @JsonProperty("outputDir")
  private String outputDir = null;

  @JsonProperty("mapper")
  private String mapper = null;

  @JsonProperty("combiner")
  private String combiner = null;

  @JsonProperty("reducer")
  private String reducer = null;

  @JsonProperty("queueName")
  private String queueName = null;

  @JsonProperty("schedulerPriority")
  private String schedulerPriority = null;

  public ApiActivity name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Activity name.
   * @return name
  **/
  @ApiModelProperty(value = "Activity name.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiActivity type(ApiActivityType type) {
    this.type = type;
    return this;
  }

  /**
   * Activity type. Whether it's an MR job, a Pig job, a Hive query, etc.
   * @return type
  **/
  @ApiModelProperty(value = "Activity type. Whether it's an MR job, a Pig job, a Hive query, etc.")

  @Valid

  public ApiActivityType getType() {
    return type;
  }

  public void setType(ApiActivityType type) {
    this.type = type;
  }

  public ApiActivity parent(String parent) {
    this.parent = parent;
    return this;
  }

  /**
   * The name of the parent activity.
   * @return parent
  **/
  @ApiModelProperty(value = "The name of the parent activity.")


  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public ApiActivity startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * The start time of this activity.
   * @return startTime
  **/
  @ApiModelProperty(value = "The start time of this activity.")


  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public ApiActivity finishTime(String finishTime) {
    this.finishTime = finishTime;
    return this;
  }

  /**
   * The finish time of this activity.
   * @return finishTime
  **/
  @ApiModelProperty(value = "The finish time of this activity.")


  public String getFinishTime() {
    return finishTime;
  }

  public void setFinishTime(String finishTime) {
    this.finishTime = finishTime;
  }

  public ApiActivity id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Activity id, which is unique within a MapReduce service.
   * @return id
  **/
  @ApiModelProperty(value = "Activity id, which is unique within a MapReduce service.")


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ApiActivity status(ApiActivityStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Activity status.
   * @return status
  **/
  @ApiModelProperty(value = "Activity status.")

  @Valid

  public ApiActivityStatus getStatus() {
    return status;
  }

  public void setStatus(ApiActivityStatus status) {
    this.status = status;
  }

  public ApiActivity user(String user) {
    this.user = user;
    return this;
  }

  /**
   * The user who submitted this activity.
   * @return user
  **/
  @ApiModelProperty(value = "The user who submitted this activity.")


  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public ApiActivity group(String group) {
    this.group = group;
    return this;
  }

  /**
   * The user-group of this activity.
   * @return group
  **/
  @ApiModelProperty(value = "The user-group of this activity.")


  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public ApiActivity inputDir(String inputDir) {
    this.inputDir = inputDir;
    return this;
  }

  /**
   * The input data directory of the activity. An HDFS url.
   * @return inputDir
  **/
  @ApiModelProperty(value = "The input data directory of the activity. An HDFS url.")


  public String getInputDir() {
    return inputDir;
  }

  public void setInputDir(String inputDir) {
    this.inputDir = inputDir;
  }

  public ApiActivity outputDir(String outputDir) {
    this.outputDir = outputDir;
    return this;
  }

  /**
   * The output result directory of the activity. An HDFS url.
   * @return outputDir
  **/
  @ApiModelProperty(value = "The output result directory of the activity. An HDFS url.")


  public String getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(String outputDir) {
    this.outputDir = outputDir;
  }

  public ApiActivity mapper(String mapper) {
    this.mapper = mapper;
    return this;
  }

  /**
   * The mapper class.
   * @return mapper
  **/
  @ApiModelProperty(value = "The mapper class.")


  public String getMapper() {
    return mapper;
  }

  public void setMapper(String mapper) {
    this.mapper = mapper;
  }

  public ApiActivity combiner(String combiner) {
    this.combiner = combiner;
    return this;
  }

  /**
   * The combiner class.
   * @return combiner
  **/
  @ApiModelProperty(value = "The combiner class.")


  public String getCombiner() {
    return combiner;
  }

  public void setCombiner(String combiner) {
    this.combiner = combiner;
  }

  public ApiActivity reducer(String reducer) {
    this.reducer = reducer;
    return this;
  }

  /**
   * The reducer class.
   * @return reducer
  **/
  @ApiModelProperty(value = "The reducer class.")


  public String getReducer() {
    return reducer;
  }

  public void setReducer(String reducer) {
    this.reducer = reducer;
  }

  public ApiActivity queueName(String queueName) {
    this.queueName = queueName;
    return this;
  }

  /**
   * The scheduler queue this activity is in.
   * @return queueName
  **/
  @ApiModelProperty(value = "The scheduler queue this activity is in.")


  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public ApiActivity schedulerPriority(String schedulerPriority) {
    this.schedulerPriority = schedulerPriority;
    return this;
  }

  /**
   * The scheduler priority of this activity.
   * @return schedulerPriority
  **/
  @ApiModelProperty(value = "The scheduler priority of this activity.")


  public String getSchedulerPriority() {
    return schedulerPriority;
  }

  public void setSchedulerPriority(String schedulerPriority) {
    this.schedulerPriority = schedulerPriority;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiActivity apiActivity = (ApiActivity) o;
    return Objects.equals(this.name, apiActivity.name) &&
        Objects.equals(this.type, apiActivity.type) &&
        Objects.equals(this.parent, apiActivity.parent) &&
        Objects.equals(this.startTime, apiActivity.startTime) &&
        Objects.equals(this.finishTime, apiActivity.finishTime) &&
        Objects.equals(this.id, apiActivity.id) &&
        Objects.equals(this.status, apiActivity.status) &&
        Objects.equals(this.user, apiActivity.user) &&
        Objects.equals(this.group, apiActivity.group) &&
        Objects.equals(this.inputDir, apiActivity.inputDir) &&
        Objects.equals(this.outputDir, apiActivity.outputDir) &&
        Objects.equals(this.mapper, apiActivity.mapper) &&
        Objects.equals(this.combiner, apiActivity.combiner) &&
        Objects.equals(this.reducer, apiActivity.reducer) &&
        Objects.equals(this.queueName, apiActivity.queueName) &&
        Objects.equals(this.schedulerPriority, apiActivity.schedulerPriority);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, parent, startTime, finishTime, id, status, user, group, inputDir, outputDir, mapper, combiner, reducer, queueName, schedulerPriority);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiActivity {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    parent: ").append(toIndentedString(parent)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    finishTime: ").append(toIndentedString(finishTime)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    group: ").append(toIndentedString(group)).append("\n");
    sb.append("    inputDir: ").append(toIndentedString(inputDir)).append("\n");
    sb.append("    outputDir: ").append(toIndentedString(outputDir)).append("\n");
    sb.append("    mapper: ").append(toIndentedString(mapper)).append("\n");
    sb.append("    combiner: ").append(toIndentedString(combiner)).append("\n");
    sb.append("    reducer: ").append(toIndentedString(reducer)).append("\n");
    sb.append("    queueName: ").append(toIndentedString(queueName)).append("\n");
    sb.append("    schedulerPriority: ").append(toIndentedString(schedulerPriority)).append("\n");
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

