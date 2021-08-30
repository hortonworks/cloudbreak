package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ScmDbType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Cloudera Manager server&#39;s database information
 */
@ApiModel(description = "Cloudera Manager server's database information")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiScmDbInfo   {
  @JsonProperty("scmDbType")
  private ScmDbType scmDbType = null;

  @JsonProperty("embeddedDbUsed")
  private Boolean embeddedDbUsed = null;

  public ApiScmDbInfo scmDbType(ScmDbType scmDbType) {
    this.scmDbType = scmDbType;
    return this;
  }

  /**
   * Cloudera Manager server's db type
   * @return scmDbType
  **/
  @ApiModelProperty(value = "Cloudera Manager server's db type")

  @Valid

  public ScmDbType getScmDbType() {
    return scmDbType;
  }

  public void setScmDbType(ScmDbType scmDbType) {
    this.scmDbType = scmDbType;
  }

  public ApiScmDbInfo embeddedDbUsed(Boolean embeddedDbUsed) {
    this.embeddedDbUsed = embeddedDbUsed;
    return this;
  }

  /**
   * Whether Cloudera Manager server is using embedded DB
   * @return embeddedDbUsed
  **/
  @ApiModelProperty(value = "Whether Cloudera Manager server is using embedded DB")


  public Boolean isEmbeddedDbUsed() {
    return embeddedDbUsed;
  }

  public void setEmbeddedDbUsed(Boolean embeddedDbUsed) {
    this.embeddedDbUsed = embeddedDbUsed;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiScmDbInfo apiScmDbInfo = (ApiScmDbInfo) o;
    return Objects.equals(this.scmDbType, apiScmDbInfo.scmDbType) &&
        Objects.equals(this.embeddedDbUsed, apiScmDbInfo.embeddedDbUsed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scmDbType, embeddedDbUsed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiScmDbInfo {\n");
    
    sb.append("    scmDbType: ").append(toIndentedString(scmDbType)).append("\n");
    sb.append("    embeddedDbUsed: ").append(toIndentedString(embeddedDbUsed)).append("\n");
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

