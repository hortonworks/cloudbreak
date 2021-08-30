package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * HDFS specific snapshot policy arguments.
 */
@ApiModel(description = "HDFS specific snapshot policy arguments.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsSnapshotPolicyArguments   {
  @JsonProperty("pathPatterns")
  @Valid
  private List<String> pathPatterns = null;

  public ApiHdfsSnapshotPolicyArguments pathPatterns(List<String> pathPatterns) {
    this.pathPatterns = pathPatterns;
    return this;
  }

  public ApiHdfsSnapshotPolicyArguments addPathPatternsItem(String pathPatternsItem) {
    if (this.pathPatterns == null) {
      this.pathPatterns = new ArrayList<>();
    }
    this.pathPatterns.add(pathPatternsItem);
    return this;
  }

  /**
   * The path patterns specifying the paths. Paths matching any of them will be eligible for snapshot creation. <p/> The pattern matching characters that can be specific are those supported by HDFS. please see the documentation for HDFS globs for more details.
   * @return pathPatterns
  **/
  @ApiModelProperty(value = "The path patterns specifying the paths. Paths matching any of them will be eligible for snapshot creation. <p/> The pattern matching characters that can be specific are those supported by HDFS. please see the documentation for HDFS globs for more details.")


  public List<String> getPathPatterns() {
    return pathPatterns;
  }

  public void setPathPatterns(List<String> pathPatterns) {
    this.pathPatterns = pathPatterns;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsSnapshotPolicyArguments apiHdfsSnapshotPolicyArguments = (ApiHdfsSnapshotPolicyArguments) o;
    return Objects.equals(this.pathPatterns, apiHdfsSnapshotPolicyArguments.pathPatterns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pathPatterns);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsSnapshotPolicyArguments {\n");
    
    sb.append("    pathPatterns: ").append(toIndentedString(pathPatterns)).append("\n");
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

