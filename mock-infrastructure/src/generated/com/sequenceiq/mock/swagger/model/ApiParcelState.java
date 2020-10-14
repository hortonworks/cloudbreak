package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * The ApiParcelState encapsulates the state of a parcel while it is in transition and reports any errors that may have occurred.. &lt;p&gt; The complete progress of a parcel is broken up into two different reporting indicators - progress and count. Progress is the primary indicator that reports the global state of transitions. For example, when downloading, progress and totalProgress will show the current number of bytes downloaded and the total number of bytes needed to be downloaded respectively. &lt;/p&gt; &lt;p&gt; The count and totalCount indicator is used when a state transition affects multiple hosts. The count and totalCount show the current number of hosts completed and the total number of hosts respectively. For example, during distribution, the progress and totalProgress will show how many bytes have been transferred to each host and the count will indicate how many hosts of of totalCount have had parcels unpacked. &lt;/p&gt; &lt;p&gt; Along with the two progress indicators, the ApiParcelState shows both errors and warnings that may have turned up during a state transition. &lt;/p&gt;
 */
@ApiModel(description = "The ApiParcelState encapsulates the state of a parcel while it is in transition and reports any errors that may have occurred.. <p> The complete progress of a parcel is broken up into two different reporting indicators - progress and count. Progress is the primary indicator that reports the global state of transitions. For example, when downloading, progress and totalProgress will show the current number of bytes downloaded and the total number of bytes needed to be downloaded respectively. </p> <p> The count and totalCount indicator is used when a state transition affects multiple hosts. The count and totalCount show the current number of hosts completed and the total number of hosts respectively. For example, during distribution, the progress and totalProgress will show how many bytes have been transferred to each host and the count will indicate how many hosts of of totalCount have had parcels unpacked. </p> <p> Along with the two progress indicators, the ApiParcelState shows both errors and warnings that may have turned up during a state transition. </p>")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiParcelState   {
  @JsonProperty("progress")
  private BigDecimal progress = null;

  @JsonProperty("totalProgress")
  private BigDecimal totalProgress = null;

  @JsonProperty("count")
  private BigDecimal count = null;

  @JsonProperty("totalCount")
  private BigDecimal totalCount = null;

  @JsonProperty("errors")
  @Valid
  private List<String> errors = null;

  @JsonProperty("warnings")
  @Valid
  private List<String> warnings = null;

  public ApiParcelState progress(BigDecimal progress) {
    this.progress = progress;
    return this;
  }

  /**
   * The progress of the state transition.
   * @return progress
  **/
  @ApiModelProperty(value = "The progress of the state transition.")

  @Valid

  public BigDecimal getProgress() {
    return progress;
  }

  public void setProgress(BigDecimal progress) {
    this.progress = progress;
  }

  public ApiParcelState totalProgress(BigDecimal totalProgress) {
    this.totalProgress = totalProgress;
    return this;
  }

  /**
   * The total amount that #getProgress() needs to get to.
   * @return totalProgress
  **/
  @ApiModelProperty(value = "The total amount that #getProgress() needs to get to.")

  @Valid

  public BigDecimal getTotalProgress() {
    return totalProgress;
  }

  public void setTotalProgress(BigDecimal totalProgress) {
    this.totalProgress = totalProgress;
  }

  public ApiParcelState count(BigDecimal count) {
    this.count = count;
    return this;
  }

  /**
   * The current hosts that have completed.
   * @return count
  **/
  @ApiModelProperty(value = "The current hosts that have completed.")

  @Valid

  public BigDecimal getCount() {
    return count;
  }

  public void setCount(BigDecimal count) {
    this.count = count;
  }

  public ApiParcelState totalCount(BigDecimal totalCount) {
    this.totalCount = totalCount;
    return this;
  }

  /**
   * The total amount that #getCount() needs to get to.
   * @return totalCount
  **/
  @ApiModelProperty(value = "The total amount that #getCount() needs to get to.")

  @Valid

  public BigDecimal getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(BigDecimal totalCount) {
    this.totalCount = totalCount;
  }

  public ApiParcelState errors(List<String> errors) {
    this.errors = errors;
    return this;
  }

  public ApiParcelState addErrorsItem(String errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }

  /**
   * The errors that exist for this parcel.
   * @return errors
  **/
  @ApiModelProperty(example = "\"null\"", value = "The errors that exist for this parcel.")


  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  public ApiParcelState warnings(List<String> warnings) {
    this.warnings = warnings;
    return this;
  }

  public ApiParcelState addWarningsItem(String warningsItem) {
    if (this.warnings == null) {
      this.warnings = new ArrayList<>();
    }
    this.warnings.add(warningsItem);
    return this;
  }

  /**
   * The warnings that exist for this parcel.
   * @return warnings
  **/
  @ApiModelProperty(example = "\"null\"", value = "The warnings that exist for this parcel.")


  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiParcelState apiParcelState = (ApiParcelState) o;
    return Objects.equals(this.progress, apiParcelState.progress) &&
        Objects.equals(this.totalProgress, apiParcelState.totalProgress) &&
        Objects.equals(this.count, apiParcelState.count) &&
        Objects.equals(this.totalCount, apiParcelState.totalCount) &&
        Objects.equals(this.errors, apiParcelState.errors) &&
        Objects.equals(this.warnings, apiParcelState.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(progress, totalProgress, count, totalCount, errors, warnings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiParcelState {\n");
    
    sb.append("    progress: ").append(toIndentedString(progress)).append("\n");
    sb.append("    totalProgress: ").append(toIndentedString(totalProgress)).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
    sb.append("    totalCount: ").append(toIndentedString(totalCount)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
    sb.append("    warnings: ").append(toIndentedString(warnings)).append("\n");
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

