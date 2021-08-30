package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiMapEntry;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A host endPoint for a service.
 */
@ApiModel(description = "A host endPoint for a service.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEndPointHost   {
  @JsonProperty("uri")
  private String uri = null;

  @JsonProperty("endPointConfigs")
  @Valid
  private List<ApiMapEntry> endPointConfigs = null;

  @JsonProperty("type")
  private String type = null;

  public ApiEndPointHost uri(String uri) {
    this.uri = uri;
    return this;
  }

  /**
   * Uri for the endPoint.
   * @return uri
  **/
  @ApiModelProperty(value = "Uri for the endPoint.")


  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public ApiEndPointHost endPointConfigs(List<ApiMapEntry> endPointConfigs) {
    this.endPointConfigs = endPointConfigs;
    return this;
  }

  public ApiEndPointHost addEndPointConfigsItem(ApiMapEntry endPointConfigsItem) {
    if (this.endPointConfigs == null) {
      this.endPointConfigs = new ArrayList<>();
    }
    this.endPointConfigs.add(endPointConfigsItem);
    return this;
  }

  /**
   * EndPointHost specific configs.
   * @return endPointConfigs
  **/
  @ApiModelProperty(value = "EndPointHost specific configs.")

  @Valid

  public List<ApiMapEntry> getEndPointConfigs() {
    return endPointConfigs;
  }

  public void setEndPointConfigs(List<ApiMapEntry> endPointConfigs) {
    this.endPointConfigs = endPointConfigs;
  }

  public ApiEndPointHost type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get endPointHost type.
   * @return type
  **/
  @ApiModelProperty(value = "Get endPointHost type.")


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEndPointHost apiEndPointHost = (ApiEndPointHost) o;
    return Objects.equals(this.uri, apiEndPointHost.uri) &&
        Objects.equals(this.endPointConfigs, apiEndPointHost.endPointConfigs) &&
        Objects.equals(this.type, apiEndPointHost.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, endPointConfigs, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEndPointHost {\n");
    
    sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
    sb.append("    endPointConfigs: ").append(toIndentedString(endPointConfigs)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

