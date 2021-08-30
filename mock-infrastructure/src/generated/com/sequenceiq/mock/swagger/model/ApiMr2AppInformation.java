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
 * Represents MapReduce2 information for a YARN application.
 */
@ApiModel(description = "Represents MapReduce2 information for a YARN application.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiMr2AppInformation   {
  @JsonProperty("jobState")
  private String jobState = null;

  public ApiMr2AppInformation jobState(String jobState) {
    this.jobState = jobState;
    return this;
  }

  /**
   * The state of the job. This is only set on completed jobs. This can take on the following values: \"NEW\", \"INITED\", \"RUNNING\", \"SUCCEEDED\", \"FAILED\", \"KILLED\", \"ERROR\".
   * @return jobState
  **/
  @ApiModelProperty(value = "The state of the job. This is only set on completed jobs. This can take on the following values: \"NEW\", \"INITED\", \"RUNNING\", \"SUCCEEDED\", \"FAILED\", \"KILLED\", \"ERROR\".")


  public String getJobState() {
    return jobState;
  }

  public void setJobState(String jobState) {
    this.jobState = jobState;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiMr2AppInformation apiMr2AppInformation = (ApiMr2AppInformation) o;
    return Objects.equals(this.jobState, apiMr2AppInformation.jobState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobState);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiMr2AppInformation {\n");
    
    sb.append("    jobState: ").append(toIndentedString(jobState)).append("\n");
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

