package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiEvent;
import com.sequenceiq.mock.swagger.model.ApiListBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiEventQueryResult extends ApiListBase  {
  @JsonProperty("totalResults")
  private BigDecimal totalResults = null;

  @JsonProperty("items")
  @Valid
  private List<ApiEvent> items = null;

  public ApiEventQueryResult totalResults(BigDecimal totalResults) {
    this.totalResults = totalResults;
    return this;
  }

  /**
   * The total number of matched results. Some are possibly not shown due to pagination.
   * @return totalResults
  **/
  @ApiModelProperty(value = "The total number of matched results. Some are possibly not shown due to pagination.")

  @Valid

  public BigDecimal getTotalResults() {
    return totalResults;
  }

  public void setTotalResults(BigDecimal totalResults) {
    this.totalResults = totalResults;
  }

  public ApiEventQueryResult items(List<ApiEvent> items) {
    this.items = items;
    return this;
  }

  public ApiEventQueryResult addItemsItem(ApiEvent itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * 
   * @return items
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiEvent> getItems() {
    return items;
  }

  public void setItems(List<ApiEvent> items) {
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
    ApiEventQueryResult apiEventQueryResult = (ApiEventQueryResult) o;
    return Objects.equals(this.totalResults, apiEventQueryResult.totalResults) &&
        Objects.equals(this.items, apiEventQueryResult.items) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalResults, items, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEventQueryResult {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    totalResults: ").append(toIndentedString(totalResults)).append("\n");
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

