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
 * Config Details: The config can either have a value or ref or variable.
 */
@ApiModel(description = "Config Details: The config can either have a value or ref or variable.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateConfig   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("value")
  private String value = null;

  @JsonProperty("ref")
  private String ref = null;

  @JsonProperty("variable")
  private String variable = null;

  @JsonProperty("autoConfig")
  private Boolean autoConfig = null;

  public ApiClusterTemplateConfig name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Config name
   * @return name
  **/
  @ApiModelProperty(value = "Config name")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiClusterTemplateConfig value(String value) {
    this.value = value;
    return this;
  }

  /**
   * Config value
   * @return value
  **/
  @ApiModelProperty(value = "Config value")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public ApiClusterTemplateConfig ref(String ref) {
    this.ref = ref;
    return this;
  }

  /**
   * Name of the reference. If referring to a service then it will be replaced with actual service name at import time. If referring to a role then it will be replaced with the host name containing that role at import time.
   * @return ref
  **/
  @ApiModelProperty(value = "Name of the reference. If referring to a service then it will be replaced with actual service name at import time. If referring to a role then it will be replaced with the host name containing that role at import time.")


  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public ApiClusterTemplateConfig variable(String variable) {
    this.variable = variable;
    return this;
  }

  /**
   * Referring a variable. The variable value will be provided by the user at import time. Variable name for this config. At import time the value of this variable will be provided by the `   * #ApiClusterTemplateInstantiator.Variables
   * @return variable
  **/
  @ApiModelProperty(value = "Referring a variable. The variable value will be provided by the user at import time. Variable name for this config. At import time the value of this variable will be provided by the `   * #ApiClusterTemplateInstantiator.Variables")


  public String getVariable() {
    return variable;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public ApiClusterTemplateConfig autoConfig(Boolean autoConfig) {
    this.autoConfig = autoConfig;
    return this;
  }

  /**
   * This indicates that the value was automatically configured.
   * @return autoConfig
  **/
  @ApiModelProperty(value = "This indicates that the value was automatically configured.")


  public Boolean isAutoConfig() {
    return autoConfig;
  }

  public void setAutoConfig(Boolean autoConfig) {
    this.autoConfig = autoConfig;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateConfig apiClusterTemplateConfig = (ApiClusterTemplateConfig) o;
    return Objects.equals(this.name, apiClusterTemplateConfig.name) &&
        Objects.equals(this.value, apiClusterTemplateConfig.value) &&
        Objects.equals(this.ref, apiClusterTemplateConfig.ref) &&
        Objects.equals(this.variable, apiClusterTemplateConfig.variable) &&
        Objects.equals(this.autoConfig, apiClusterTemplateConfig.autoConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value, ref, variable, autoConfig);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateConfig {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    ref: ").append(toIndentedString(ref)).append("\n");
    sb.append("    variable: ").append(toIndentedString(variable)).append("\n");
    sb.append("    autoConfig: ").append(toIndentedString(autoConfig)).append("\n");
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

