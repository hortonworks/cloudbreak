package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ShutdownReadinessState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Cloudera Manager server&#39;s shutdown readiness
 */
@ApiModel(description = "Cloudera Manager server's shutdown readiness")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiShutdownReadiness   {
  @JsonProperty("state")
  private ShutdownReadinessState state = null;

  public ApiShutdownReadiness state(ShutdownReadinessState state) {
    this.state = state;
    return this;
  }

  /**
   * Shutdown readiness state
   * @return state
  **/
  @ApiModelProperty(value = "Shutdown readiness state")

  @Valid

  public ShutdownReadinessState getState() {
    return state;
  }

  public void setState(ShutdownReadinessState state) {
    this.state = state;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiShutdownReadiness apiShutdownReadiness = (ApiShutdownReadiness) o;
    return Objects.equals(this.state, apiShutdownReadiness.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(state);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiShutdownReadiness {\n");
    
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
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

