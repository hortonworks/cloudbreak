package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHealthSummary;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Represents a result from a health test performed by Cloudera Manager for an entity.
 */
@ApiModel(description = "Represents a result from a health test performed by Cloudera Manager for an entity.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHealthCheck   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("summary")
  private ApiHealthSummary summary = null;

  @JsonProperty("explanation")
  private String explanation = null;

  @JsonProperty("suppressed")
  private Boolean suppressed = null;

  public ApiHealthCheck name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Unique name of this health check.
   * @return name
  **/
  @ApiModelProperty(value = "Unique name of this health check.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiHealthCheck summary(ApiHealthSummary summary) {
    this.summary = summary;
    return this;
  }

  /**
   * The summary status of this check.
   * @return summary
  **/
  @ApiModelProperty(value = "The summary status of this check.")

  @Valid

  public ApiHealthSummary getSummary() {
    return summary;
  }

  public void setSummary(ApiHealthSummary summary) {
    this.summary = summary;
  }

  public ApiHealthCheck explanation(String explanation) {
    this.explanation = explanation;
    return this;
  }

  /**
   * The explanation of this health check. Available since v11.
   * @return explanation
  **/
  @ApiModelProperty(value = "The explanation of this health check. Available since v11.")


  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public ApiHealthCheck suppressed(Boolean suppressed) {
    this.suppressed = suppressed;
    return this;
  }

  /**
   * Whether this health test is suppressed. A suppressed health test is not considered when computing an entity's overall health. Available since v11.
   * @return suppressed
  **/
  @ApiModelProperty(value = "Whether this health test is suppressed. A suppressed health test is not considered when computing an entity's overall health. Available since v11.")


  public Boolean isSuppressed() {
    return suppressed;
  }

  public void setSuppressed(Boolean suppressed) {
    this.suppressed = suppressed;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHealthCheck apiHealthCheck = (ApiHealthCheck) o;
    return Objects.equals(this.name, apiHealthCheck.name) &&
        Objects.equals(this.summary, apiHealthCheck.summary) &&
        Objects.equals(this.explanation, apiHealthCheck.explanation) &&
        Objects.equals(this.suppressed, apiHealthCheck.suppressed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, summary, explanation, suppressed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHealthCheck {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
    sb.append("    explanation: ").append(toIndentedString(explanation)).append("\n");
    sb.append("    suppressed: ").append(toIndentedString(suppressed)).append("\n");
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

