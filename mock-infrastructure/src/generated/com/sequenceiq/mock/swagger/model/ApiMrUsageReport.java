package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiListBase;
import com.sequenceiq.mock.swagger.model.ApiMrUsageReportRow;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A generic list.
 */
@ApiModel(description = "A generic list.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiMrUsageReport extends ApiListBase  {
  @JsonProperty("items")
  @Valid
  private List<ApiMrUsageReportRow> items = null;

  public ApiMrUsageReport items(List<ApiMrUsageReportRow> items) {
    this.items = items;
    return this;
  }

  public ApiMrUsageReport addItemsItem(ApiMrUsageReportRow itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * A list of per-user usage information at the requested time granularity.
   * @return items
  **/
  @ApiModelProperty(value = "A list of per-user usage information at the requested time granularity.")

  @Valid

  public List<ApiMrUsageReportRow> getItems() {
    return items;
  }

  public void setItems(List<ApiMrUsageReportRow> items) {
    this.items = items;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiMrUsageReport apiMrUsageReport = (ApiMrUsageReport) o;
    return Objects.equals(this.items, apiMrUsageReport.items) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiMrUsageReport {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

