package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterRef;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiRoleRef;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * API object which groups types of entities as per the associated tag
 */
@ApiModel(description = "API object which groups types of entities as per the associated tag")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTagToEntities   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("value")
  private String value = null;

  @JsonProperty("apiClusterRefs")
  @Valid
  private List<ApiClusterRef> apiClusterRefs = null;

  @JsonProperty("apiServiceRefs")
  @Valid
  private List<ApiServiceRef> apiServiceRefs = null;

  @JsonProperty("apiRoleRefs")
  @Valid
  private List<ApiRoleRef> apiRoleRefs = null;

  @JsonProperty("apiHostRefs")
  @Valid
  private List<ApiHostRef> apiHostRefs = null;

  public ApiTagToEntities name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 
   * @return name
  **/
  @ApiModelProperty(value = "")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiTagToEntities value(String value) {
    this.value = value;
    return this;
  }

  /**
   * 
   * @return value
  **/
  @ApiModelProperty(value = "")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public ApiTagToEntities apiClusterRefs(List<ApiClusterRef> apiClusterRefs) {
    this.apiClusterRefs = apiClusterRefs;
    return this;
  }

  public ApiTagToEntities addApiClusterRefsItem(ApiClusterRef apiClusterRefsItem) {
    if (this.apiClusterRefs == null) {
      this.apiClusterRefs = new ArrayList<>();
    }
    this.apiClusterRefs.add(apiClusterRefsItem);
    return this;
  }

  /**
   * 
   * @return apiClusterRefs
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiClusterRef> getApiClusterRefs() {
    return apiClusterRefs;
  }

  public void setApiClusterRefs(List<ApiClusterRef> apiClusterRefs) {
    this.apiClusterRefs = apiClusterRefs;
  }

  public ApiTagToEntities apiServiceRefs(List<ApiServiceRef> apiServiceRefs) {
    this.apiServiceRefs = apiServiceRefs;
    return this;
  }

  public ApiTagToEntities addApiServiceRefsItem(ApiServiceRef apiServiceRefsItem) {
    if (this.apiServiceRefs == null) {
      this.apiServiceRefs = new ArrayList<>();
    }
    this.apiServiceRefs.add(apiServiceRefsItem);
    return this;
  }

  /**
   * 
   * @return apiServiceRefs
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiServiceRef> getApiServiceRefs() {
    return apiServiceRefs;
  }

  public void setApiServiceRefs(List<ApiServiceRef> apiServiceRefs) {
    this.apiServiceRefs = apiServiceRefs;
  }

  public ApiTagToEntities apiRoleRefs(List<ApiRoleRef> apiRoleRefs) {
    this.apiRoleRefs = apiRoleRefs;
    return this;
  }

  public ApiTagToEntities addApiRoleRefsItem(ApiRoleRef apiRoleRefsItem) {
    if (this.apiRoleRefs == null) {
      this.apiRoleRefs = new ArrayList<>();
    }
    this.apiRoleRefs.add(apiRoleRefsItem);
    return this;
  }

  /**
   * 
   * @return apiRoleRefs
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiRoleRef> getApiRoleRefs() {
    return apiRoleRefs;
  }

  public void setApiRoleRefs(List<ApiRoleRef> apiRoleRefs) {
    this.apiRoleRefs = apiRoleRefs;
  }

  public ApiTagToEntities apiHostRefs(List<ApiHostRef> apiHostRefs) {
    this.apiHostRefs = apiHostRefs;
    return this;
  }

  public ApiTagToEntities addApiHostRefsItem(ApiHostRef apiHostRefsItem) {
    if (this.apiHostRefs == null) {
      this.apiHostRefs = new ArrayList<>();
    }
    this.apiHostRefs.add(apiHostRefsItem);
    return this;
  }

  /**
   * 
   * @return apiHostRefs
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiHostRef> getApiHostRefs() {
    return apiHostRefs;
  }

  public void setApiHostRefs(List<ApiHostRef> apiHostRefs) {
    this.apiHostRefs = apiHostRefs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTagToEntities apiTagToEntities = (ApiTagToEntities) o;
    return Objects.equals(this.name, apiTagToEntities.name) &&
        Objects.equals(this.value, apiTagToEntities.value) &&
        Objects.equals(this.apiClusterRefs, apiTagToEntities.apiClusterRefs) &&
        Objects.equals(this.apiServiceRefs, apiTagToEntities.apiServiceRefs) &&
        Objects.equals(this.apiRoleRefs, apiTagToEntities.apiRoleRefs) &&
        Objects.equals(this.apiHostRefs, apiTagToEntities.apiHostRefs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value, apiClusterRefs, apiServiceRefs, apiRoleRefs, apiHostRefs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTagToEntities {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    apiClusterRefs: ").append(toIndentedString(apiClusterRefs)).append("\n");
    sb.append("    apiServiceRefs: ").append(toIndentedString(apiServiceRefs)).append("\n");
    sb.append("    apiRoleRefs: ").append(toIndentedString(apiRoleRefs)).append("\n");
    sb.append("    apiHostRefs: ").append(toIndentedString(apiHostRefs)).append("\n");
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

