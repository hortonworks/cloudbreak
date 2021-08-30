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
 * A query details response.
 */
@ApiModel(description = "A query details response.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiImpalaQueryDetailsResponse   {
  @JsonProperty("details")
  private String details = null;

  public ApiImpalaQueryDetailsResponse details(String details) {
    this.details = details;
    return this;
  }

  /**
   * The details for this query. Two formats are supported: <ul> <li> 'text': this is a text based, human readable representation of the Impala runtime profile. </li> <li> 'thrift_encoded': this a compact-thrift, base64 encoded representation of the impala RuntimeProfile.thrift object. See the Impala documentation for more details. </li> </ul>
   * @return details
  **/
  @ApiModelProperty(value = "The details for this query. Two formats are supported: <ul> <li> 'text': this is a text based, human readable representation of the Impala runtime profile. </li> <li> 'thrift_encoded': this a compact-thrift, base64 encoded representation of the impala RuntimeProfile.thrift object. See the Impala documentation for more details. </li> </ul>")


  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiImpalaQueryDetailsResponse apiImpalaQueryDetailsResponse = (ApiImpalaQueryDetailsResponse) o;
    return Objects.equals(this.details, apiImpalaQueryDetailsResponse.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(details);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaQueryDetailsResponse {\n");
    
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
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

