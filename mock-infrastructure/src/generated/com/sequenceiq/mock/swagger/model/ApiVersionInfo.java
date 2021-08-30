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
 * Version information of Cloudera Manager itself.
 */
@ApiModel(description = "Version information of Cloudera Manager itself.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiVersionInfo   {
  @JsonProperty("version")
  private String version = null;

  @JsonProperty("snapshot")
  private Boolean snapshot = null;

  @JsonProperty("buildUser")
  private String buildUser = null;

  @JsonProperty("buildTimestamp")
  private String buildTimestamp = null;

  @JsonProperty("gitHash")
  private String gitHash = null;

  public ApiVersionInfo version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Version.
   * @return version
  **/
  @ApiModelProperty(value = "Version.")


  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public ApiVersionInfo snapshot(Boolean snapshot) {
    this.snapshot = snapshot;
    return this;
  }

  /**
   * Whether this build is a development snapshot.
   * @return snapshot
  **/
  @ApiModelProperty(value = "Whether this build is a development snapshot.")


  public Boolean isSnapshot() {
    return snapshot;
  }

  public void setSnapshot(Boolean snapshot) {
    this.snapshot = snapshot;
  }

  public ApiVersionInfo buildUser(String buildUser) {
    this.buildUser = buildUser;
    return this;
  }

  /**
   * The user performing the build.
   * @return buildUser
  **/
  @ApiModelProperty(value = "The user performing the build.")


  public String getBuildUser() {
    return buildUser;
  }

  public void setBuildUser(String buildUser) {
    this.buildUser = buildUser;
  }

  public ApiVersionInfo buildTimestamp(String buildTimestamp) {
    this.buildTimestamp = buildTimestamp;
    return this;
  }

  /**
   * Build timestamp.
   * @return buildTimestamp
  **/
  @ApiModelProperty(value = "Build timestamp.")


  public String getBuildTimestamp() {
    return buildTimestamp;
  }

  public void setBuildTimestamp(String buildTimestamp) {
    this.buildTimestamp = buildTimestamp;
  }

  public ApiVersionInfo gitHash(String gitHash) {
    this.gitHash = gitHash;
    return this;
  }

  /**
   * Source control management hash.
   * @return gitHash
  **/
  @ApiModelProperty(value = "Source control management hash.")


  public String getGitHash() {
    return gitHash;
  }

  public void setGitHash(String gitHash) {
    this.gitHash = gitHash;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiVersionInfo apiVersionInfo = (ApiVersionInfo) o;
    return Objects.equals(this.version, apiVersionInfo.version) &&
        Objects.equals(this.snapshot, apiVersionInfo.snapshot) &&
        Objects.equals(this.buildUser, apiVersionInfo.buildUser) &&
        Objects.equals(this.buildTimestamp, apiVersionInfo.buildTimestamp) &&
        Objects.equals(this.gitHash, apiVersionInfo.gitHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, snapshot, buildUser, buildTimestamp, gitHash);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiVersionInfo {\n");
    
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    snapshot: ").append(toIndentedString(snapshot)).append("\n");
    sb.append("    buildUser: ").append(toIndentedString(buildUser)).append("\n");
    sb.append("    buildTimestamp: ").append(toIndentedString(buildTimestamp)).append("\n");
    sb.append("    gitHash: ").append(toIndentedString(gitHash)).append("\n");
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

