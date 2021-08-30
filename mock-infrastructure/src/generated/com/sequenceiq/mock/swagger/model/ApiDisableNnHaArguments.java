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
 * Arguments used for Disable NameNode High Availability command.
 */
@ApiModel(description = "Arguments used for Disable NameNode High Availability command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiDisableNnHaArguments   {
  @JsonProperty("activeNnName")
  private String activeNnName = null;

  @JsonProperty("snnHostId")
  private String snnHostId = null;

  @JsonProperty("snnCheckpointDirList")
  @Valid
  private List<String> snnCheckpointDirList = null;

  @JsonProperty("snnName")
  private String snnName = null;

  public ApiDisableNnHaArguments activeNnName(String activeNnName) {
    this.activeNnName = activeNnName;
    return this;
  }

  /**
   * Name of the NamdeNode role that is going to be active after High Availability is disabled.
   * @return activeNnName
  **/
  @ApiModelProperty(value = "Name of the NamdeNode role that is going to be active after High Availability is disabled.")


  public String getActiveNnName() {
    return activeNnName;
  }

  public void setActiveNnName(String activeNnName) {
    this.activeNnName = activeNnName;
  }

  public ApiDisableNnHaArguments snnHostId(String snnHostId) {
    this.snnHostId = snnHostId;
    return this;
  }

  /**
   * Id of the host where the new SecondaryNameNode will be created.
   * @return snnHostId
  **/
  @ApiModelProperty(value = "Id of the host where the new SecondaryNameNode will be created.")


  public String getSnnHostId() {
    return snnHostId;
  }

  public void setSnnHostId(String snnHostId) {
    this.snnHostId = snnHostId;
  }

  public ApiDisableNnHaArguments snnCheckpointDirList(List<String> snnCheckpointDirList) {
    this.snnCheckpointDirList = snnCheckpointDirList;
    return this;
  }

  public ApiDisableNnHaArguments addSnnCheckpointDirListItem(String snnCheckpointDirListItem) {
    if (this.snnCheckpointDirList == null) {
      this.snnCheckpointDirList = new ArrayList<>();
    }
    this.snnCheckpointDirList.add(snnCheckpointDirListItem);
    return this;
  }

  /**
   * List of directories used for checkpointing by the new SecondaryNameNode.
   * @return snnCheckpointDirList
  **/
  @ApiModelProperty(value = "List of directories used for checkpointing by the new SecondaryNameNode.")


  public List<String> getSnnCheckpointDirList() {
    return snnCheckpointDirList;
  }

  public void setSnnCheckpointDirList(List<String> snnCheckpointDirList) {
    this.snnCheckpointDirList = snnCheckpointDirList;
  }

  public ApiDisableNnHaArguments snnName(String snnName) {
    this.snnName = snnName;
    return this;
  }

  /**
   * Name of the new SecondaryNameNode role (Optional).
   * @return snnName
  **/
  @ApiModelProperty(value = "Name of the new SecondaryNameNode role (Optional).")


  public String getSnnName() {
    return snnName;
  }

  public void setSnnName(String snnName) {
    this.snnName = snnName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiDisableNnHaArguments apiDisableNnHaArguments = (ApiDisableNnHaArguments) o;
    return Objects.equals(this.activeNnName, apiDisableNnHaArguments.activeNnName) &&
        Objects.equals(this.snnHostId, apiDisableNnHaArguments.snnHostId) &&
        Objects.equals(this.snnCheckpointDirList, apiDisableNnHaArguments.snnCheckpointDirList) &&
        Objects.equals(this.snnName, apiDisableNnHaArguments.snnName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activeNnName, snnHostId, snnCheckpointDirList, snnName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiDisableNnHaArguments {\n");
    
    sb.append("    activeNnName: ").append(toIndentedString(activeNnName)).append("\n");
    sb.append("    snnHostId: ").append(toIndentedString(snnHostId)).append("\n");
    sb.append("    snnCheckpointDirList: ").append(toIndentedString(snnCheckpointDirList)).append("\n");
    sb.append("    snnName: ").append(toIndentedString(snnName)).append("\n");
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

