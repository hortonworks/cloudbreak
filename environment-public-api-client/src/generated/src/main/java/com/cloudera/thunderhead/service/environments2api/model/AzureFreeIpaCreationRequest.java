/*
 * Cloudera Environments Service
 * Cloudera Environments Service is a web service that manages cloud provider access.
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.thunderhead.service.environments2api.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Request object for creating FreeIPA in the environment.
 */
@JsonPropertyOrder({
  AzureFreeIpaCreationRequest.JSON_PROPERTY_INSTANCE_COUNT_BY_GROUP,
  AzureFreeIpaCreationRequest.JSON_PROPERTY_RECIPES,
  AzureFreeIpaCreationRequest.JSON_PROPERTY_INSTANCE_TYPE,
  AzureFreeIpaCreationRequest.JSON_PROPERTY_MULTI_AZ
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class AzureFreeIpaCreationRequest {
  public static final String JSON_PROPERTY_INSTANCE_COUNT_BY_GROUP = "instanceCountByGroup";
  private Integer instanceCountByGroup;

  public static final String JSON_PROPERTY_RECIPES = "recipes";
  private List<String> recipes = new ArrayList<>();

  public static final String JSON_PROPERTY_INSTANCE_TYPE = "instanceType";
  private String instanceType;

  public static final String JSON_PROPERTY_MULTI_AZ = "multiAz";
  private Boolean multiAz = false;

  public AzureFreeIpaCreationRequest() { 
  }

  public AzureFreeIpaCreationRequest instanceCountByGroup(Integer instanceCountByGroup) {
    this.instanceCountByGroup = instanceCountByGroup;
    return this;
  }

   /**
   * The number of FreeIPA instances to create per group when creating FreeIPA in the environment
   * @return instanceCountByGroup
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INSTANCE_COUNT_BY_GROUP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getInstanceCountByGroup() {
    return instanceCountByGroup;
  }


  @JsonProperty(JSON_PROPERTY_INSTANCE_COUNT_BY_GROUP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setInstanceCountByGroup(Integer instanceCountByGroup) {
    this.instanceCountByGroup = instanceCountByGroup;
  }


  public AzureFreeIpaCreationRequest recipes(List<String> recipes) {
    this.recipes = recipes;
    return this;
  }

  public AzureFreeIpaCreationRequest addRecipesItem(String recipesItem) {
    if (this.recipes == null) {
      this.recipes = new ArrayList<>();
    }
    this.recipes.add(recipesItem);
    return this;
  }

   /**
   * The recipes for the FreeIPA cluster.
   * @return recipes
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RECIPES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getRecipes() {
    return recipes;
  }


  @JsonProperty(JSON_PROPERTY_RECIPES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setRecipes(List<String> recipes) {
    this.recipes = recipes;
  }


  public AzureFreeIpaCreationRequest instanceType(String instanceType) {
    this.instanceType = instanceType;
    return this;
  }

   /**
   * Custom instance type of FreeIPA instances.
   * @return instanceType
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INSTANCE_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getInstanceType() {
    return instanceType;
  }


  @JsonProperty(JSON_PROPERTY_INSTANCE_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setInstanceType(String instanceType) {
    this.instanceType = instanceType;
  }


  public AzureFreeIpaCreationRequest multiAz(Boolean multiAz) {
    this.multiAz = multiAz;
    return this;
  }

   /**
   * Flag that enables deployment of the FreeIPA in a multi-availability zone.
   * @return multiAz
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MULTI_AZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getMultiAz() {
    return multiAz;
  }


  @JsonProperty(JSON_PROPERTY_MULTI_AZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMultiAz(Boolean multiAz) {
    this.multiAz = multiAz;
  }


  /**
   * Return true if this AzureFreeIpaCreationRequest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AzureFreeIpaCreationRequest azureFreeIpaCreationRequest = (AzureFreeIpaCreationRequest) o;
    return Objects.equals(this.instanceCountByGroup, azureFreeIpaCreationRequest.instanceCountByGroup) &&
        Objects.equals(this.recipes, azureFreeIpaCreationRequest.recipes) &&
        Objects.equals(this.instanceType, azureFreeIpaCreationRequest.instanceType) &&
        Objects.equals(this.multiAz, azureFreeIpaCreationRequest.multiAz);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceCountByGroup, recipes, instanceType, multiAz);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AzureFreeIpaCreationRequest {\n");
    sb.append("    instanceCountByGroup: ").append(toIndentedString(instanceCountByGroup)).append("\n");
    sb.append("    recipes: ").append(toIndentedString(recipes)).append("\n");
    sb.append("    instanceType: ").append(toIndentedString(instanceType)).append("\n");
    sb.append("    multiAz: ").append(toIndentedString(multiAz)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `instanceCountByGroup` to the URL query string
    if (getInstanceCountByGroup() != null) {
      joiner.add(String.format("%sinstanceCountByGroup%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getInstanceCountByGroup()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `recipes` to the URL query string
    if (getRecipes() != null) {
      for (int i = 0; i < getRecipes().size(); i++) {
        joiner.add(String.format("%srecipes%s%s=%s", prefix, suffix,
            "".equals(suffix) ? "" : String.format("%s%d%s", containerPrefix, i, containerSuffix),
            URLEncoder.encode(String.valueOf(getRecipes().get(i)), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
      }
    }

    // add `instanceType` to the URL query string
    if (getInstanceType() != null) {
      joiner.add(String.format("%sinstanceType%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getInstanceType()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `multiAz` to the URL query string
    if (getMultiAz() != null) {
      joiner.add(String.format("%smultiAz%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getMultiAz()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

