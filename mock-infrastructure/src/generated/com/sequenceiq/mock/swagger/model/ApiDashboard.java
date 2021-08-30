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
 * A dashboard definition. Dashboards are composed of tsquery-based charts.
 */
@ApiModel(description = "A dashboard definition. Dashboards are composed of tsquery-based charts.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiDashboard   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("json")
  private String json = null;

  public ApiDashboard name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Returns the dashboard name.
   * @return name
  **/
  @ApiModelProperty(value = "Returns the dashboard name.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiDashboard json(String json) {
    this.json = json;
    return this;
  }

  /**
   * Returns the json structure for the dashboard. This should be treated as an opaque blob.
   * @return json
  **/
  @ApiModelProperty(value = "Returns the json structure for the dashboard. This should be treated as an opaque blob.")


  public String getJson() {
    return json;
  }

  public void setJson(String json) {
    this.json = json;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiDashboard apiDashboard = (ApiDashboard) o;
    return Objects.equals(this.name, apiDashboard.name) &&
        Objects.equals(this.json, apiDashboard.json);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, json);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiDashboard {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    json: ").append(toIndentedString(json)).append("\n");
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

