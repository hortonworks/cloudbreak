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
 * Variable that is referred in cluster template.
 */
@ApiModel(description = "Variable that is referred in cluster template.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateVariable   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("value")
  private String value = null;

  public ApiClusterTemplateVariable name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Variable name that are referred in cluster template
   * @return name
  **/
  @ApiModelProperty(value = "Variable name that are referred in cluster template")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiClusterTemplateVariable value(String value) {
    this.value = value;
    return this;
  }

  /**
   * This value will be placed whereever the variable is referred in the cluster template
   * @return value
  **/
  @ApiModelProperty(value = "This value will be placed whereever the variable is referred in the cluster template")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateVariable apiClusterTemplateVariable = (ApiClusterTemplateVariable) o;
    return Objects.equals(this.name, apiClusterTemplateVariable.name) &&
        Objects.equals(this.value, apiClusterTemplateVariable.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateVariable {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

