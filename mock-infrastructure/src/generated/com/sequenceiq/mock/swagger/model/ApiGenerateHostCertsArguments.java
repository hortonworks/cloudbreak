package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.BaseApiSshCmdArguments;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments to install certificates on a host
 */
@ApiModel(description = "Arguments to install certificates on a host")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiGenerateHostCertsArguments extends BaseApiSshCmdArguments  {
  @JsonProperty("subjectAltName")
  @Valid
  private List<String> subjectAltName = null;

  public ApiGenerateHostCertsArguments subjectAltName(List<String> subjectAltName) {
    this.subjectAltName = subjectAltName;
    return this;
  }

  public ApiGenerateHostCertsArguments addSubjectAltNameItem(String subjectAltNameItem) {
    if (this.subjectAltName == null) {
      this.subjectAltName = new ArrayList<>();
    }
    this.subjectAltName.add(subjectAltNameItem);
    return this;
  }

  /**
   * 
   * @return subjectAltName
  **/
  @ApiModelProperty(value = "")


  public List<String> getSubjectAltName() {
    return subjectAltName;
  }

  public void setSubjectAltName(List<String> subjectAltName) {
    this.subjectAltName = subjectAltName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiGenerateHostCertsArguments apiGenerateHostCertsArguments = (ApiGenerateHostCertsArguments) o;
    return Objects.equals(this.subjectAltName, apiGenerateHostCertsArguments.subjectAltName) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectAltName, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiGenerateHostCertsArguments {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    subjectAltName: ").append(toIndentedString(subjectAltName)).append("\n");
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

