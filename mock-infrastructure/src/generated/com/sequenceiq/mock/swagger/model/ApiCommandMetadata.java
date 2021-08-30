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
 * Provides metadata information about a command.
 */
@ApiModel(description = "Provides metadata information about a command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiCommandMetadata   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("argSchema")
  private String argSchema = null;

  public ApiCommandMetadata name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of of the command.
   * @return name
  **/
  @ApiModelProperty(value = "The name of of the command.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiCommandMetadata argSchema(String argSchema) {
    this.argSchema = argSchema;
    return this;
  }

  /**
   * The command arguments schema.  This is in the form of json schema and describes the structure of the command arguments. If null, the command does not take arguments.
   * @return argSchema
  **/
  @ApiModelProperty(value = "The command arguments schema.  This is in the form of json schema and describes the structure of the command arguments. If null, the command does not take arguments.")


  public String getArgSchema() {
    return argSchema;
  }

  public void setArgSchema(String argSchema) {
    this.argSchema = argSchema;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCommandMetadata apiCommandMetadata = (ApiCommandMetadata) o;
    return Objects.equals(this.name, apiCommandMetadata.name) &&
        Objects.equals(this.argSchema, apiCommandMetadata.argSchema);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, argSchema);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCommandMetadata {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    argSchema: ").append(toIndentedString(argSchema)).append("\n");
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

