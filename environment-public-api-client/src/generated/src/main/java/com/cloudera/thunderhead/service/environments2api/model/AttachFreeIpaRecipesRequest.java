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

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Request object for attaching recipes to FreeIPA.
 */
@JsonPropertyOrder({
  AttachFreeIpaRecipesRequest.JSON_PROPERTY_ENVIRONMENT,
  AttachFreeIpaRecipesRequest.JSON_PROPERTY_RECIPES
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class AttachFreeIpaRecipesRequest {
  public static final String JSON_PROPERTY_ENVIRONMENT = "environment";
  private String environment;

  public static final String JSON_PROPERTY_RECIPES = "recipes";
  private List<String> recipes = new ArrayList<>();

  public AttachFreeIpaRecipesRequest() {
  }

  public AttachFreeIpaRecipesRequest environment(String environment) {
    
    this.environment = environment;
    return this;
  }

   /**
   * The environment name or CRN of the FreeIPA.
   * @return environment
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ENVIRONMENT)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getEnvironment() {
    return environment;
  }


  @JsonProperty(JSON_PROPERTY_ENVIRONMENT)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setEnvironment(String environment) {
    this.environment = environment;
  }


  public AttachFreeIpaRecipesRequest recipes(List<String> recipes) {
    
    this.recipes = recipes;
    return this;
  }

  public AttachFreeIpaRecipesRequest addRecipesItem(String recipesItem) {
    if (this.recipes == null) {
      this.recipes = new ArrayList<>();
    }
    this.recipes.add(recipesItem);
    return this;
  }

   /**
   * The list of recipes to attach.
   * @return recipes
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_RECIPES)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public List<String> getRecipes() {
    return recipes;
  }


  @JsonProperty(JSON_PROPERTY_RECIPES)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setRecipes(List<String> recipes) {
    this.recipes = recipes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttachFreeIpaRecipesRequest attachFreeIpaRecipesRequest = (AttachFreeIpaRecipesRequest) o;
    return Objects.equals(this.environment, attachFreeIpaRecipesRequest.environment) &&
        Objects.equals(this.recipes, attachFreeIpaRecipesRequest.recipes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environment, recipes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttachFreeIpaRecipesRequest {\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    recipes: ").append(toIndentedString(recipes)).append("\n");
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

}

