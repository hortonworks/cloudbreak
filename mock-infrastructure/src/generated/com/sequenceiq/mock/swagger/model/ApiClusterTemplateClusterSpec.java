package com.sequenceiq.mock.swagger.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Specify type of cluster to create. If one or more ApiDataContextRef are specified, the created cluster upon import will be a cluster with clusterType \&quot;COMPUTE\&quot;.
 */
@ApiModel(description = "Specify type of cluster to create. If one or more ApiDataContextRef are specified, the created cluster upon import will be a cluster with clusterType \"COMPUTE\".")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateClusterSpec   {
  @JsonProperty("dataContextRefs")
  @Valid
  private List<ApiDataContextRef> dataContextRefs = null;

  public ApiClusterTemplateClusterSpec dataContextRefs(List<ApiDataContextRef> dataContextRefs) {
    this.dataContextRefs = dataContextRefs;
    return this;
  }

  public ApiClusterTemplateClusterSpec addDataContextRefsItem(ApiDataContextRef dataContextRefsItem) {
    if (this.dataContextRefs == null) {
      this.dataContextRefs = new ArrayList<>();
    }
    this.dataContextRefs.add(dataContextRefsItem);
    return this;
  }

  /**
   *
   * @return dataContextRefs
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiDataContextRef> getDataContextRefs() {
    return dataContextRefs;
  }

  public void setDataContextRefs(List<ApiDataContextRef> dataContextRefs) {
    this.dataContextRefs = dataContextRefs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateClusterSpec apiClusterTemplateClusterSpec = (ApiClusterTemplateClusterSpec) o;
    return Objects.equals(this.dataContextRefs, apiClusterTemplateClusterSpec.dataContextRefs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataContextRefs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateClusterSpec {\n");

    sb.append("    dataContextRefs: ").append(toIndentedString(dataContextRefs)).append("\n");
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

