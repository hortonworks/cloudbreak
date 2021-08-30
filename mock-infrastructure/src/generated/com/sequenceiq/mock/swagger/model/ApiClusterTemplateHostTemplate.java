package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Host templates will contain information about the role config groups that should be applied to a host. This basically means a host will have a role corresponding to each config group.
 */
@ApiModel(description = "Host templates will contain information about the role config groups that should be applied to a host. This basically means a host will have a role corresponding to each config group.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateHostTemplate   {
  @JsonProperty("refName")
  private String refName = null;

  @JsonProperty("roleConfigGroupsRefNames")
  @Valid
  private List<String> roleConfigGroupsRefNames = null;

  @JsonProperty("cardinality")
  private Integer cardinality = null;

  @JsonProperty("tags")
  @Valid
  private List<ApiEntityTag> tags = null;

  public ApiClusterTemplateHostTemplate refName(String refName) {
    this.refName = refName;
    return this;
  }

  /**
   * Reference name
   * @return refName
  **/
  @ApiModelProperty(value = "Reference name")


  public String getRefName() {
    return refName;
  }

  public void setRefName(String refName) {
    this.refName = refName;
  }

  public ApiClusterTemplateHostTemplate roleConfigGroupsRefNames(List<String> roleConfigGroupsRefNames) {
    this.roleConfigGroupsRefNames = roleConfigGroupsRefNames;
    return this;
  }

  public ApiClusterTemplateHostTemplate addRoleConfigGroupsRefNamesItem(String roleConfigGroupsRefNamesItem) {
    if (this.roleConfigGroupsRefNames == null) {
      this.roleConfigGroupsRefNames = new ArrayList<>();
    }
    this.roleConfigGroupsRefNames.add(roleConfigGroupsRefNamesItem);
    return this;
  }

  /**
   * List of role config groups
   * @return roleConfigGroupsRefNames
  **/
  @ApiModelProperty(value = "List of role config groups")


  public List<String> getRoleConfigGroupsRefNames() {
    return roleConfigGroupsRefNames;
  }

  public void setRoleConfigGroupsRefNames(List<String> roleConfigGroupsRefNames) {
    this.roleConfigGroupsRefNames = roleConfigGroupsRefNames;
  }

  public ApiClusterTemplateHostTemplate cardinality(Integer cardinality) {
    this.cardinality = cardinality;
    return this;
  }

  /**
   * Represent the cardinality of this host template on source. Defaults to 0.
   * @return cardinality
  **/
  @ApiModelProperty(value = "Represent the cardinality of this host template on source. Defaults to 0.")


  public Integer getCardinality() {
    return cardinality;
  }

  public void setCardinality(Integer cardinality) {
    this.cardinality = cardinality;
  }

  public ApiClusterTemplateHostTemplate tags(List<ApiEntityTag> tags) {
    this.tags = tags;
    return this;
  }

  public ApiClusterTemplateHostTemplate addTagsItem(ApiEntityTag tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * The tags to be added to hosts when this template is applied
   * @return tags
  **/
  @ApiModelProperty(value = "The tags to be added to hosts when this template is applied")

  @Valid

  public List<ApiEntityTag> getTags() {
    return tags;
  }

  public void setTags(List<ApiEntityTag> tags) {
    this.tags = tags;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateHostTemplate apiClusterTemplateHostTemplate = (ApiClusterTemplateHostTemplate) o;
    return Objects.equals(this.refName, apiClusterTemplateHostTemplate.refName) &&
        Objects.equals(this.roleConfigGroupsRefNames, apiClusterTemplateHostTemplate.roleConfigGroupsRefNames) &&
        Objects.equals(this.cardinality, apiClusterTemplateHostTemplate.cardinality) &&
        Objects.equals(this.tags, apiClusterTemplateHostTemplate.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refName, roleConfigGroupsRefNames, cardinality, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateHostTemplate {\n");
    
    sb.append("    refName: ").append(toIndentedString(refName)).append("\n");
    sb.append("    roleConfigGroupsRefNames: ").append(toIndentedString(roleConfigGroupsRefNames)).append("\n");
    sb.append("    cardinality: ").append(toIndentedString(cardinality)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

