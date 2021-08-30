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
 * Arguments used as part of ApiEnableNnHaArguments to specify JournalNodes.
 */
@ApiModel(description = "Arguments used as part of ApiEnableNnHaArguments to specify JournalNodes.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiJournalNodeArguments   {
  @JsonProperty("jnName")
  private String jnName = null;

  @JsonProperty("jnHostId")
  private String jnHostId = null;

  @JsonProperty("jnEditsDir")
  private String jnEditsDir = null;

  public ApiJournalNodeArguments jnName(String jnName) {
    this.jnName = jnName;
    return this;
  }

  /**
   * Name of new JournalNode to be created. (Optional)
   * @return jnName
  **/
  @ApiModelProperty(value = "Name of new JournalNode to be created. (Optional)")


  public String getJnName() {
    return jnName;
  }

  public void setJnName(String jnName) {
    this.jnName = jnName;
  }

  public ApiJournalNodeArguments jnHostId(String jnHostId) {
    this.jnHostId = jnHostId;
    return this;
  }

  /**
   * ID of the host where the new JournalNode will be created.
   * @return jnHostId
  **/
  @ApiModelProperty(value = "ID of the host where the new JournalNode will be created.")


  public String getJnHostId() {
    return jnHostId;
  }

  public void setJnHostId(String jnHostId) {
    this.jnHostId = jnHostId;
  }

  public ApiJournalNodeArguments jnEditsDir(String jnEditsDir) {
    this.jnEditsDir = jnEditsDir;
    return this;
  }

  /**
   * Path to the JournalNode edits directory. Need not be specified if it is already set at RoleConfigGroup level.
   * @return jnEditsDir
  **/
  @ApiModelProperty(value = "Path to the JournalNode edits directory. Need not be specified if it is already set at RoleConfigGroup level.")


  public String getJnEditsDir() {
    return jnEditsDir;
  }

  public void setJnEditsDir(String jnEditsDir) {
    this.jnEditsDir = jnEditsDir;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiJournalNodeArguments apiJournalNodeArguments = (ApiJournalNodeArguments) o;
    return Objects.equals(this.jnName, apiJournalNodeArguments.jnName) &&
        Objects.equals(this.jnHostId, apiJournalNodeArguments.jnHostId) &&
        Objects.equals(this.jnEditsDir, apiJournalNodeArguments.jnEditsDir);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jnName, jnHostId, jnEditsDir);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiJournalNodeArguments {\n");
    
    sb.append("    jnName: ").append(toIndentedString(jnName)).append("\n");
    sb.append("    jnHostId: ").append(toIndentedString(jnHostId)).append("\n");
    sb.append("    jnEditsDir: ").append(toIndentedString(jnEditsDir)).append("\n");
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

