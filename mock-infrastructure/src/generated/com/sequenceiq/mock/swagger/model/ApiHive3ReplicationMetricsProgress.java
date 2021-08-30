package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationMetricsStage;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationMetricsStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * 
 */
@ApiModel(description = "")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHive3ReplicationMetricsProgress   {
  @JsonProperty("status")
  private ApiHive3ReplicationMetricsStatus status = null;

  @JsonProperty("stages")
  @Valid
  private List<ApiHive3ReplicationMetricsStage> stages = null;

  public ApiHive3ReplicationMetricsProgress status(ApiHive3ReplicationMetricsStatus status) {
    this.status = status;
    return this;
  }

  /**
   * 
   * @return status
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHive3ReplicationMetricsStatus getStatus() {
    return status;
  }

  public void setStatus(ApiHive3ReplicationMetricsStatus status) {
    this.status = status;
  }

  public ApiHive3ReplicationMetricsProgress stages(List<ApiHive3ReplicationMetricsStage> stages) {
    this.stages = stages;
    return this;
  }

  public ApiHive3ReplicationMetricsProgress addStagesItem(ApiHive3ReplicationMetricsStage stagesItem) {
    if (this.stages == null) {
      this.stages = new ArrayList<>();
    }
    this.stages.add(stagesItem);
    return this;
  }

  /**
   * 
   * @return stages
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiHive3ReplicationMetricsStage> getStages() {
    return stages;
  }

  public void setStages(List<ApiHive3ReplicationMetricsStage> stages) {
    this.stages = stages;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHive3ReplicationMetricsProgress apiHive3ReplicationMetricsProgress = (ApiHive3ReplicationMetricsProgress) o;
    return Objects.equals(this.status, apiHive3ReplicationMetricsProgress.status) &&
        Objects.equals(this.stages, apiHive3ReplicationMetricsProgress.stages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, stages);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHive3ReplicationMetricsProgress {\n");
    
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    stages: ").append(toIndentedString(stages)).append("\n");
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

