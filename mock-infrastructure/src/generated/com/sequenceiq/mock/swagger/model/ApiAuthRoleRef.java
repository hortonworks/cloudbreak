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
 * An authRoleRef to operate on ApiAuthRole object
 */
@ApiModel(description = "An authRoleRef to operate on ApiAuthRole object")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiAuthRoleRef   {
  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("uuid")
  private String uuid = null;

  public ApiAuthRoleRef displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * The display name of the authRole. displayName is optional. If a changed displayName is passed in, it will be ignored.
   * @return displayName
  **/
  @ApiModelProperty(value = "The display name of the authRole. displayName is optional. If a changed displayName is passed in, it will be ignored.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiAuthRoleRef name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the authRole. name is available from v32. It is optional, and cannot be modified. Name takes precedence over uuid. If name is absent, uuid will be used for look up.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the authRole. name is available from v32. It is optional, and cannot be modified. Name takes precedence over uuid. If name is absent, uuid will be used for look up.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiAuthRoleRef uuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * The uuid of the authRole, which uniquely identifies it in a CM installation.
   * @return uuid
  **/
  @ApiModelProperty(value = "The uuid of the authRole, which uniquely identifies it in a CM installation.")


  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiAuthRoleRef apiAuthRoleRef = (ApiAuthRoleRef) o;
    return Objects.equals(this.displayName, apiAuthRoleRef.displayName) &&
        Objects.equals(this.name, apiAuthRoleRef.name) &&
        Objects.equals(this.uuid, apiAuthRoleRef.uuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, name, uuid);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiAuthRoleRef {\n");
    
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
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

