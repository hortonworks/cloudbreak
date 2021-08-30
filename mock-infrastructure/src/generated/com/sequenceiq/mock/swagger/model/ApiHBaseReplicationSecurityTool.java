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
 * HBase Replication Security Tool Management
 */
@ApiModel(description = "HBase Replication Security Tool Management")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHBaseReplicationSecurityTool   {
  @JsonProperty("password")
  private String password = null;

  @JsonProperty("keystoreFilePath")
  private String keystoreFilePath = null;

  public ApiHBaseReplicationSecurityTool password(String password) {
    this.password = password;
    return this;
  }

  /**
   * 
   * @return password
  **/
  @ApiModelProperty(value = "")


  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public ApiHBaseReplicationSecurityTool keystoreFilePath(String keystoreFilePath) {
    this.keystoreFilePath = keystoreFilePath;
    return this;
  }

  /**
   * 
   * @return keystoreFilePath
  **/
  @ApiModelProperty(value = "")


  public String getKeystoreFilePath() {
    return keystoreFilePath;
  }

  public void setKeystoreFilePath(String keystoreFilePath) {
    this.keystoreFilePath = keystoreFilePath;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHBaseReplicationSecurityTool apiHBaseReplicationSecurityTool = (ApiHBaseReplicationSecurityTool) o;
    return Objects.equals(this.password, apiHBaseReplicationSecurityTool.password) &&
        Objects.equals(this.keystoreFilePath, apiHBaseReplicationSecurityTool.keystoreFilePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(password, keystoreFilePath);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseReplicationSecurityTool {\n");
    
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    keystoreFilePath: ").append(toIndentedString(keystoreFilePath)).append("\n");
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

