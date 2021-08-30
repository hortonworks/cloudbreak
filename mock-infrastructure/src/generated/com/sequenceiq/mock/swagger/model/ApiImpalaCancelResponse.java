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
 * The response from an Impala cancel query response.
 */
@ApiModel(description = "The response from an Impala cancel query response.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiImpalaCancelResponse   {
  @JsonProperty("warning")
  private String warning = null;

  public ApiImpalaCancelResponse warning(String warning) {
    this.warning = warning;
    return this;
  }

  /**
   * The warning response. If there was no warning this will be null.
   * @return warning
  **/
  @ApiModelProperty(value = "The warning response. If there was no warning this will be null.")


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
    ApiImpalaCancelResponse apiImpalaCancelResponse = (ApiImpalaCancelResponse) o;
    return Objects.equals(this.warning, apiImpalaCancelResponse.warning);
  }

  @Override
  public int hashCode() {
    return Objects.hash(warning);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaCancelResponse {\n");
    
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

