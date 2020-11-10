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
 * An impala UDF identifier.
 */
@ApiModel(description = "An impala UDF identifier.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiImpalaUDF   {
  @JsonProperty("database")
  private String database = null;

  @JsonProperty("signature")
  private String signature = null;

  public ApiImpalaUDF database(String database) {
    this.database = database;
    return this;
  }

  /**
   * Name of the database to which this UDF belongs.
   * @return database
  **/
  @ApiModelProperty(value = "Name of the database to which this UDF belongs.")


  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public ApiImpalaUDF signature(String signature) {
    this.signature = signature;
    return this;
  }

  /**
   * UDF signature, includes the UDF name and parameter types.
   * @return signature
  **/
  @ApiModelProperty(value = "UDF signature, includes the UDF name and parameter types.")


  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiImpalaUDF apiImpalaUDF = (ApiImpalaUDF) o;
    return Objects.equals(this.database, apiImpalaUDF.database) &&
        Objects.equals(this.signature, apiImpalaUDF.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(database, signature);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaUDF {\n");
    
    sb.append("    database: ").append(toIndentedString(database)).append("\n");
    sb.append("    signature: ").append(toIndentedString(signature)).append("\n");
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

