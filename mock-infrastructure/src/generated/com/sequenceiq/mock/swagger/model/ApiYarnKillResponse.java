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
 * The response from an Yarn kill application response.
 */
@ApiModel(description = "The response from an Yarn kill application response.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiYarnKillResponse   {
  @JsonProperty("warning")
  private String warning = null;

  public ApiYarnKillResponse warning(String warning) {
    this.warning = warning;
    return this;
  }

  /**
   * The warning, if any, from the call. We will return a warning if the caller attempts to cancel an application that has already completed.
   * @return warning
  **/
  @ApiModelProperty(value = "The warning, if any, from the call. We will return a warning if the caller attempts to cancel an application that has already completed.")


  public String getWarning() {
    return warning;
  }

  public void setWarning(String warning) {
    this.warning = warning;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiYarnKillResponse apiYarnKillResponse = (ApiYarnKillResponse) o;
    return Objects.equals(this.warning, apiYarnKillResponse.warning);
  }

  @Override
  public int hashCode() {
    return Objects.hash(warning);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiYarnKillResponse {\n");
    
    sb.append("    warning: ").append(toIndentedString(warning)).append("\n");
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

