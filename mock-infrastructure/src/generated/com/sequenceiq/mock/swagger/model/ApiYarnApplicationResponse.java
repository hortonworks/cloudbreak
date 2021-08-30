package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiYarnApplication;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * The response contains a list of applications and warnings.
 */
@ApiModel(description = "The response contains a list of applications and warnings.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiYarnApplicationResponse   {
  @JsonProperty("applications")
  @Valid
  private List<ApiYarnApplication> applications = null;

  @JsonProperty("warnings")
  @Valid
  private List<String> warnings = null;

  public ApiYarnApplicationResponse applications(List<ApiYarnApplication> applications) {
    this.applications = applications;
    return this;
  }

  public ApiYarnApplicationResponse addApplicationsItem(ApiYarnApplication applicationsItem) {
    if (this.applications == null) {
      this.applications = new ArrayList<>();
    }
    this.applications.add(applicationsItem);
    return this;
  }

  /**
   * The list of applications for this response.
   * @return applications
  **/
  @ApiModelProperty(value = "The list of applications for this response.")

  @Valid

  public List<ApiYarnApplication> getApplications() {
    return applications;
  }

  public void setApplications(List<ApiYarnApplication> applications) {
    this.applications = applications;
  }

  public ApiYarnApplicationResponse warnings(List<String> warnings) {
    this.warnings = warnings;
    return this;
  }

  public ApiYarnApplicationResponse addWarningsItem(String warningsItem) {
    if (this.warnings == null) {
      this.warnings = new ArrayList<>();
    }
    this.warnings.add(warningsItem);
    return this;
  }

  /**
   * This list of warnings for this response.
   * @return warnings
  **/
  @ApiModelProperty(value = "This list of warnings for this response.")


  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiYarnApplicationResponse apiYarnApplicationResponse = (ApiYarnApplicationResponse) o;
    return Objects.equals(this.applications, apiYarnApplicationResponse.applications) &&
        Objects.equals(this.warnings, apiYarnApplicationResponse.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(applications, warnings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiYarnApplicationResponse {\n");
    
    sb.append("    applications: ").append(toIndentedString(applications)).append("\n");
    sb.append("    warnings: ").append(toIndentedString(warnings)).append("\n");
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

