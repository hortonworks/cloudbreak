package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiMr2AppInformation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Represents a Yarn application
 */
@ApiModel(description = "Represents a Yarn application")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiYarnApplication   {
  @JsonProperty("allocatedMB")
  private Integer allocatedMB = null;

  @JsonProperty("allocatedVCores")
  private Integer allocatedVCores = null;

  @JsonProperty("runningContainers")
  private Integer runningContainers = null;

  @JsonProperty("applicationTags")
  @Valid
  private List<String> applicationTags = null;

  @JsonProperty("allocatedMemorySeconds")
  private Integer allocatedMemorySeconds = null;

  @JsonProperty("allocatedVcoreSeconds")
  private Integer allocatedVcoreSeconds = null;

  @JsonProperty("applicationId")
  private String applicationId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("startTime")
  private String startTime = null;

  @JsonProperty("endTime")
  private String endTime = null;

  @JsonProperty("user")
  private String user = null;

  @JsonProperty("pool")
  private String pool = null;

  @JsonProperty("progress")
  private BigDecimal progress = null;

  @JsonProperty("attributes")
  @Valid
  private Map<String, String> attributes = null;

  @JsonProperty("mr2AppInformation")
  private ApiMr2AppInformation mr2AppInformation = null;

  @JsonProperty("state")
  private String state = null;

  @JsonProperty("containerUsedMemorySeconds")
  private BigDecimal containerUsedMemorySeconds = null;

  @JsonProperty("containerUsedMemoryMax")
  private BigDecimal containerUsedMemoryMax = null;

  @JsonProperty("containerUsedCpuSeconds")
  private BigDecimal containerUsedCpuSeconds = null;

  @JsonProperty("containerUsedVcoreSeconds")
  private BigDecimal containerUsedVcoreSeconds = null;

  @JsonProperty("containerAllocatedMemorySeconds")
  private BigDecimal containerAllocatedMemorySeconds = null;

  @JsonProperty("containerAllocatedVcoreSeconds")
  private BigDecimal containerAllocatedVcoreSeconds = null;

  public ApiYarnApplication allocatedMB(Integer allocatedMB) {
    this.allocatedMB = allocatedMB;
    return this;
  }

  /**
   * The sum of memory in MB allocated to the application's running containers Available since v12.
   * @return allocatedMB
  **/
  @ApiModelProperty(value = "The sum of memory in MB allocated to the application's running containers Available since v12.")


  public Integer getAllocatedMB() {
    return allocatedMB;
  }

  public void setAllocatedMB(Integer allocatedMB) {
    this.allocatedMB = allocatedMB;
  }

  public ApiYarnApplication allocatedVCores(Integer allocatedVCores) {
    this.allocatedVCores = allocatedVCores;
    return this;
  }

  /**
   * The sum of virtual cores allocated to the application's running containers Available since v12.
   * @return allocatedVCores
  **/
  @ApiModelProperty(value = "The sum of virtual cores allocated to the application's running containers Available since v12.")


  public Integer getAllocatedVCores() {
    return allocatedVCores;
  }

  public void setAllocatedVCores(Integer allocatedVCores) {
    this.allocatedVCores = allocatedVCores;
  }

  public ApiYarnApplication runningContainers(Integer runningContainers) {
    this.runningContainers = runningContainers;
    return this;
  }

  /**
   * The number of containers currently running for the application Available since v12.
   * @return runningContainers
  **/
  @ApiModelProperty(value = "The number of containers currently running for the application Available since v12.")


  public Integer getRunningContainers() {
    return runningContainers;
  }

  public void setRunningContainers(Integer runningContainers) {
    this.runningContainers = runningContainers;
  }

  public ApiYarnApplication applicationTags(List<String> applicationTags) {
    this.applicationTags = applicationTags;
    return this;
  }

  public ApiYarnApplication addApplicationTagsItem(String applicationTagsItem) {
    if (this.applicationTags == null) {
      this.applicationTags = new ArrayList<>();
    }
    this.applicationTags.add(applicationTagsItem);
    return this;
  }

  /**
   * List of YARN application tags. Available since v12.
   * @return applicationTags
  **/
  @ApiModelProperty(value = "List of YARN application tags. Available since v12.")


  public List<String> getApplicationTags() {
    return applicationTags;
  }

  public void setApplicationTags(List<String> applicationTags) {
    this.applicationTags = applicationTags;
  }

  public ApiYarnApplication allocatedMemorySeconds(Integer allocatedMemorySeconds) {
    this.allocatedMemorySeconds = allocatedMemorySeconds;
    return this;
  }

  /**
   * Allocated memory to the application in units of mb-secs. Available since v12.
   * @return allocatedMemorySeconds
  **/
  @ApiModelProperty(value = "Allocated memory to the application in units of mb-secs. Available since v12.")


  public Integer getAllocatedMemorySeconds() {
    return allocatedMemorySeconds;
  }

  public void setAllocatedMemorySeconds(Integer allocatedMemorySeconds) {
    this.allocatedMemorySeconds = allocatedMemorySeconds;
  }

  public ApiYarnApplication allocatedVcoreSeconds(Integer allocatedVcoreSeconds) {
    this.allocatedVcoreSeconds = allocatedVcoreSeconds;
    return this;
  }

  /**
   * Allocated vcore-secs to the application. Available since v12.
   * @return allocatedVcoreSeconds
  **/
  @ApiModelProperty(value = "Allocated vcore-secs to the application. Available since v12.")


  public Integer getAllocatedVcoreSeconds() {
    return allocatedVcoreSeconds;
  }

  public void setAllocatedVcoreSeconds(Integer allocatedVcoreSeconds) {
    this.allocatedVcoreSeconds = allocatedVcoreSeconds;
  }

  public ApiYarnApplication applicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  /**
   * The application id.
   * @return applicationId
  **/
  @ApiModelProperty(value = "The application id.")


  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public ApiYarnApplication name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the application.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the application.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiYarnApplication startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * The time the application was submitted.
   * @return startTime
  **/
  @ApiModelProperty(value = "The time the application was submitted.")


  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public ApiYarnApplication endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * The time the application finished. If the application hasn't finished this will return null.
   * @return endTime
  **/
  @ApiModelProperty(value = "The time the application finished. If the application hasn't finished this will return null.")


  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public ApiYarnApplication user(String user) {
    this.user = user;
    return this;
  }

  /**
   * The user who submitted the application.
   * @return user
  **/
  @ApiModelProperty(value = "The user who submitted the application.")


  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public ApiYarnApplication pool(String pool) {
    this.pool = pool;
    return this;
  }

  /**
   * The pool the application was submitted to.
   * @return pool
  **/
  @ApiModelProperty(value = "The pool the application was submitted to.")


  public String getPool() {
    return pool;
  }

  public void setPool(String pool) {
    this.pool = pool;
  }

  public ApiYarnApplication progress(BigDecimal progress) {
    this.progress = progress;
    return this;
  }

  /**
   * The progress, as a percentage, the application has made. This is only set if the application is currently executing.
   * @return progress
  **/
  @ApiModelProperty(value = "The progress, as a percentage, the application has made. This is only set if the application is currently executing.")

  @Valid

  public BigDecimal getProgress() {
    return progress;
  }

  public void setProgress(BigDecimal progress) {
    this.progress = progress;
  }

  public ApiYarnApplication attributes(Map<String, String> attributes) {
    this.attributes = attributes;
    return this;
  }

  public ApiYarnApplication putAttributesItem(String key, String attributesItem) {
    if (this.attributes == null) {
      this.attributes = new HashMap<>();
    }
    this.attributes.put(key, attributesItem);
    return this;
  }

  /**
   * A map of additional application attributes which is generated by Cloudera Manager. For example MR2 job counters are exposed as key/value pairs here. For more details see the Cloudera Manager documentation.
   * @return attributes
  **/
  @ApiModelProperty(value = "A map of additional application attributes which is generated by Cloudera Manager. For example MR2 job counters are exposed as key/value pairs here. For more details see the Cloudera Manager documentation.")


  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public ApiYarnApplication mr2AppInformation(ApiMr2AppInformation mr2AppInformation) {
    this.mr2AppInformation = mr2AppInformation;
    return this;
  }

  /**
   * 
   * @return mr2AppInformation
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiMr2AppInformation getMr2AppInformation() {
    return mr2AppInformation;
  }

  public void setMr2AppInformation(ApiMr2AppInformation mr2AppInformation) {
    this.mr2AppInformation = mr2AppInformation;
  }

  public ApiYarnApplication state(String state) {
    this.state = state;
    return this;
  }

  /**
   * 
   * @return state
  **/
  @ApiModelProperty(value = "")


  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public ApiYarnApplication containerUsedMemorySeconds(BigDecimal containerUsedMemorySeconds) {
    this.containerUsedMemorySeconds = containerUsedMemorySeconds;
    return this;
  }

  /**
   * Actual memory (in MB-secs) used by containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.
   * @return containerUsedMemorySeconds
  **/
  @ApiModelProperty(value = "Actual memory (in MB-secs) used by containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.")

  @Valid

  public BigDecimal getContainerUsedMemorySeconds() {
    return containerUsedMemorySeconds;
  }

  public void setContainerUsedMemorySeconds(BigDecimal containerUsedMemorySeconds) {
    this.containerUsedMemorySeconds = containerUsedMemorySeconds;
  }

  public ApiYarnApplication containerUsedMemoryMax(BigDecimal containerUsedMemoryMax) {
    this.containerUsedMemoryMax = containerUsedMemoryMax;
    return this;
  }

  /**
   * Maximum memory used by containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics Available since v16
   * @return containerUsedMemoryMax
  **/
  @ApiModelProperty(value = "Maximum memory used by containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics Available since v16")

  @Valid

  public BigDecimal getContainerUsedMemoryMax() {
    return containerUsedMemoryMax;
  }

  public void setContainerUsedMemoryMax(BigDecimal containerUsedMemoryMax) {
    this.containerUsedMemoryMax = containerUsedMemoryMax;
  }

  public ApiYarnApplication containerUsedCpuSeconds(BigDecimal containerUsedCpuSeconds) {
    this.containerUsedCpuSeconds = containerUsedCpuSeconds;
    return this;
  }

  /**
   * Actual CPU (in percent-secs) used by containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.
   * @return containerUsedCpuSeconds
  **/
  @ApiModelProperty(value = "Actual CPU (in percent-secs) used by containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.")

  @Valid

  public BigDecimal getContainerUsedCpuSeconds() {
    return containerUsedCpuSeconds;
  }

  public void setContainerUsedCpuSeconds(BigDecimal containerUsedCpuSeconds) {
    this.containerUsedCpuSeconds = containerUsedCpuSeconds;
  }

  public ApiYarnApplication containerUsedVcoreSeconds(BigDecimal containerUsedVcoreSeconds) {
    this.containerUsedVcoreSeconds = containerUsedVcoreSeconds;
    return this;
  }

  /**
   * Actual VCore-secs used by containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.
   * @return containerUsedVcoreSeconds
  **/
  @ApiModelProperty(value = "Actual VCore-secs used by containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.")

  @Valid

  public BigDecimal getContainerUsedVcoreSeconds() {
    return containerUsedVcoreSeconds;
  }

  public void setContainerUsedVcoreSeconds(BigDecimal containerUsedVcoreSeconds) {
    this.containerUsedVcoreSeconds = containerUsedVcoreSeconds;
  }

  public ApiYarnApplication containerAllocatedMemorySeconds(BigDecimal containerAllocatedMemorySeconds) {
    this.containerAllocatedMemorySeconds = containerAllocatedMemorySeconds;
    return this;
  }

  /**
   * Total memory (in mb-secs) allocated to containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.
   * @return containerAllocatedMemorySeconds
  **/
  @ApiModelProperty(value = "Total memory (in mb-secs) allocated to containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.")

  @Valid

  public BigDecimal getContainerAllocatedMemorySeconds() {
    return containerAllocatedMemorySeconds;
  }

  public void setContainerAllocatedMemorySeconds(BigDecimal containerAllocatedMemorySeconds) {
    this.containerAllocatedMemorySeconds = containerAllocatedMemorySeconds;
  }

  public ApiYarnApplication containerAllocatedVcoreSeconds(BigDecimal containerAllocatedVcoreSeconds) {
    this.containerAllocatedVcoreSeconds = containerAllocatedVcoreSeconds;
    return this;
  }

  /**
   * Total vcore-secs allocated to containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.
   * @return containerAllocatedVcoreSeconds
  **/
  @ApiModelProperty(value = "Total vcore-secs allocated to containers launched by the YARN application. Computed by running a MapReduce job from Cloudera Service Monitor to aggregate YARN usage metrics. Available since v12.")

  @Valid

  public BigDecimal getContainerAllocatedVcoreSeconds() {
    return containerAllocatedVcoreSeconds;
  }

  public void setContainerAllocatedVcoreSeconds(BigDecimal containerAllocatedVcoreSeconds) {
    this.containerAllocatedVcoreSeconds = containerAllocatedVcoreSeconds;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiYarnApplication apiYarnApplication = (ApiYarnApplication) o;
    return Objects.equals(this.allocatedMB, apiYarnApplication.allocatedMB) &&
        Objects.equals(this.allocatedVCores, apiYarnApplication.allocatedVCores) &&
        Objects.equals(this.runningContainers, apiYarnApplication.runningContainers) &&
        Objects.equals(this.applicationTags, apiYarnApplication.applicationTags) &&
        Objects.equals(this.allocatedMemorySeconds, apiYarnApplication.allocatedMemorySeconds) &&
        Objects.equals(this.allocatedVcoreSeconds, apiYarnApplication.allocatedVcoreSeconds) &&
        Objects.equals(this.applicationId, apiYarnApplication.applicationId) &&
        Objects.equals(this.name, apiYarnApplication.name) &&
        Objects.equals(this.startTime, apiYarnApplication.startTime) &&
        Objects.equals(this.endTime, apiYarnApplication.endTime) &&
        Objects.equals(this.user, apiYarnApplication.user) &&
        Objects.equals(this.pool, apiYarnApplication.pool) &&
        Objects.equals(this.progress, apiYarnApplication.progress) &&
        Objects.equals(this.attributes, apiYarnApplication.attributes) &&
        Objects.equals(this.mr2AppInformation, apiYarnApplication.mr2AppInformation) &&
        Objects.equals(this.state, apiYarnApplication.state) &&
        Objects.equals(this.containerUsedMemorySeconds, apiYarnApplication.containerUsedMemorySeconds) &&
        Objects.equals(this.containerUsedMemoryMax, apiYarnApplication.containerUsedMemoryMax) &&
        Objects.equals(this.containerUsedCpuSeconds, apiYarnApplication.containerUsedCpuSeconds) &&
        Objects.equals(this.containerUsedVcoreSeconds, apiYarnApplication.containerUsedVcoreSeconds) &&
        Objects.equals(this.containerAllocatedMemorySeconds, apiYarnApplication.containerAllocatedMemorySeconds) &&
        Objects.equals(this.containerAllocatedVcoreSeconds, apiYarnApplication.containerAllocatedVcoreSeconds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allocatedMB, allocatedVCores, runningContainers, applicationTags, allocatedMemorySeconds, allocatedVcoreSeconds, applicationId, name, startTime, endTime, user, pool, progress, attributes, mr2AppInformation, state, containerUsedMemorySeconds, containerUsedMemoryMax, containerUsedCpuSeconds, containerUsedVcoreSeconds, containerAllocatedMemorySeconds, containerAllocatedVcoreSeconds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiYarnApplication {\n");
    
    sb.append("    allocatedMB: ").append(toIndentedString(allocatedMB)).append("\n");
    sb.append("    allocatedVCores: ").append(toIndentedString(allocatedVCores)).append("\n");
    sb.append("    runningContainers: ").append(toIndentedString(runningContainers)).append("\n");
    sb.append("    applicationTags: ").append(toIndentedString(applicationTags)).append("\n");
    sb.append("    allocatedMemorySeconds: ").append(toIndentedString(allocatedMemorySeconds)).append("\n");
    sb.append("    allocatedVcoreSeconds: ").append(toIndentedString(allocatedVcoreSeconds)).append("\n");
    sb.append("    applicationId: ").append(toIndentedString(applicationId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    pool: ").append(toIndentedString(pool)).append("\n");
    sb.append("    progress: ").append(toIndentedString(progress)).append("\n");
    sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
    sb.append("    mr2AppInformation: ").append(toIndentedString(mr2AppInformation)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    containerUsedMemorySeconds: ").append(toIndentedString(containerUsedMemorySeconds)).append("\n");
    sb.append("    containerUsedMemoryMax: ").append(toIndentedString(containerUsedMemoryMax)).append("\n");
    sb.append("    containerUsedCpuSeconds: ").append(toIndentedString(containerUsedCpuSeconds)).append("\n");
    sb.append("    containerUsedVcoreSeconds: ").append(toIndentedString(containerUsedVcoreSeconds)).append("\n");
    sb.append("    containerAllocatedMemorySeconds: ").append(toIndentedString(containerAllocatedMemorySeconds)).append("\n");
    sb.append("    containerAllocatedVcoreSeconds: ").append(toIndentedString(containerAllocatedVcoreSeconds)).append("\n");
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

