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
 * Arguments used for the Roll Edits command.
 */
@ApiModel(description = "Arguments used for the Roll Edits command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRollEditsArgs   {
  @JsonProperty("nameservice")
  private String nameservice = null;

  public ApiRollEditsArgs nameservice(String nameservice) {
    this.nameservice = nameservice;
    return this;
  }

  /**
   * Nameservice whose edits need to be rolled. Required only if HDFS service is federated.
   * @return nameservice
  **/
  @ApiModelProperty(value = "Nameservice whose edits need to be rolled. Required only if HDFS service is federated.")


  public String getNameservice() {
    return nameservice;
  }

  public void setNameservice(String nameservice) {
    this.nameservice = nameservice;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiRollEditsArgs apiRollEditsArgs = (ApiRollEditsArgs) o;
    return Objects.equals(this.nameservice, apiRollEditsArgs.nameservice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nameservice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRollEditsArgs {\n");
    
    sb.append("    nameservice: ").append(toIndentedString(nameservice)).append("\n");
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

