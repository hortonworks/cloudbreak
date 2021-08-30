package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * An HBase snapshot descriptor.
 */
@ApiModel(description = "An HBase snapshot descriptor.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiAdhocHBaseSnapshot   {
  @JsonProperty("sourceAccount")
  private String sourceAccount = null;

  @JsonProperty("sourceTable")
  private String sourceTable = null;

  @JsonProperty("sourceSchedulerPool")
  private String sourceSchedulerPool = null;

  @JsonProperty("sourceService")
  private ApiServiceRef sourceService = null;

  @JsonProperty("sourceNumMappers")
  private Integer sourceNumMappers = null;

  public ApiAdhocHBaseSnapshot sourceAccount(String sourceAccount) {
    this.sourceAccount = sourceAccount;
    return this;
  }

  /**
   * 
   * @return sourceAccount
  **/
  @ApiModelProperty(value = "")


  public String getSourceAccount() {
    return sourceAccount;
  }

  public void setSourceAccount(String sourceAccount) {
    this.sourceAccount = sourceAccount;
  }

  public ApiAdhocHBaseSnapshot sourceTable(String sourceTable) {
    this.sourceTable = sourceTable;
    return this;
  }

  /**
   * 
   * @return sourceTable
  **/
  @ApiModelProperty(value = "")


  public String getSourceTable() {
    return sourceTable;
  }

  public void setSourceTable(String sourceTable) {
    this.sourceTable = sourceTable;
  }

  public ApiAdhocHBaseSnapshot sourceSchedulerPool(String sourceSchedulerPool) {
    this.sourceSchedulerPool = sourceSchedulerPool;
    return this;
  }

  /**
   * 
   * @return sourceSchedulerPool
  **/
  @ApiModelProperty(value = "")


  public String getSourceSchedulerPool() {
    return sourceSchedulerPool;
  }

  public void setSourceSchedulerPool(String sourceSchedulerPool) {
    this.sourceSchedulerPool = sourceSchedulerPool;
  }

  public ApiAdhocHBaseSnapshot sourceService(ApiServiceRef sourceService) {
    this.sourceService = sourceService;
    return this;
  }

  /**
   * 
   * @return sourceService
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiServiceRef getSourceService() {
    return sourceService;
  }

  public void setSourceService(ApiServiceRef sourceService) {
    this.sourceService = sourceService;
  }

  public ApiAdhocHBaseSnapshot sourceNumMappers(Integer sourceNumMappers) {
    this.sourceNumMappers = sourceNumMappers;
    return this;
  }

  /**
   * 
   * @return sourceNumMappers
  **/
  @ApiModelProperty(value = "")


  public Integer getSourceNumMappers() {
    return sourceNumMappers;
  }

  public void setSourceNumMappers(Integer sourceNumMappers) {
    this.sourceNumMappers = sourceNumMappers;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiAdhocHBaseSnapshot apiAdhocHBaseSnapshot = (ApiAdhocHBaseSnapshot) o;
    return Objects.equals(this.sourceAccount, apiAdhocHBaseSnapshot.sourceAccount) &&
        Objects.equals(this.sourceTable, apiAdhocHBaseSnapshot.sourceTable) &&
        Objects.equals(this.sourceSchedulerPool, apiAdhocHBaseSnapshot.sourceSchedulerPool) &&
        Objects.equals(this.sourceService, apiAdhocHBaseSnapshot.sourceService) &&
        Objects.equals(this.sourceNumMappers, apiAdhocHBaseSnapshot.sourceNumMappers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceAccount, sourceTable, sourceSchedulerPool, sourceService, sourceNumMappers);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiAdhocHBaseSnapshot {\n");
    
    sb.append("    sourceAccount: ").append(toIndentedString(sourceAccount)).append("\n");
    sb.append("    sourceTable: ").append(toIndentedString(sourceTable)).append("\n");
    sb.append("    sourceSchedulerPool: ").append(toIndentedString(sourceSchedulerPool)).append("\n");
    sb.append("    sourceService: ").append(toIndentedString(sourceService)).append("\n");
    sb.append("    sourceNumMappers: ").append(toIndentedString(sourceNumMappers)).append("\n");
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

