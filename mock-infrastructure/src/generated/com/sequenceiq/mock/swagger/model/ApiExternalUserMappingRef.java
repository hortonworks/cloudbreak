package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiExternalUserMappingType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * An externalUserMappingRef references an externalUserMapping.
 */
@ApiModel(description = "An externalUserMappingRef references an externalUserMapping.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiExternalUserMappingRef   {
  @JsonProperty("uuid")
  private String uuid = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("type")
  private ApiExternalUserMappingType type = null;

  public ApiExternalUserMappingRef uuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * The uuid of the external user mapping, which uniquely identifies it in a CM installation.
   * @return uuid
  **/
  @ApiModelProperty(value = "The uuid of the external user mapping, which uniquely identifies it in a CM installation.")


  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public ApiExternalUserMappingRef name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the mapping.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the mapping.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiExternalUserMappingRef type(ApiExternalUserMappingType type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the mapping.
   * @return type
  **/
  @ApiModelProperty(value = "The type of the mapping.")

  @Valid

  public ApiExternalUserMappingType getType() {
    return type;
  }

  public void setType(ApiExternalUserMappingType type) {
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
    ApiExternalUserMappingRef apiExternalUserMappingRef = (ApiExternalUserMappingRef) o;
    return Objects.equals(this.uuid, apiExternalUserMappingRef.uuid) &&
        Objects.equals(this.name, apiExternalUserMappingRef.name) &&
        Objects.equals(this.type, apiExternalUserMappingRef.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, name, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiExternalUserMappingRef {\n");
    
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

