package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiBatchResponseElement;
import com.sequenceiq.mock.swagger.model.ApiListBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A batch response, comprised of one or more response elements.
 */
@ApiModel(description = "A batch response, comprised of one or more response elements.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiBatchResponse extends ApiListBase  {
  @JsonProperty("items")
  @Valid
  private List<ApiBatchResponseElement> items = null;

  @JsonProperty("success")
  private Boolean success = null;

  public ApiBatchResponse items(List<ApiBatchResponseElement> items) {
    this.items = items;
    return this;
  }

  public ApiBatchResponse addItemsItem(ApiBatchResponseElement itemsItem) {
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

  public List<ApiBatchResponseElement> getItems() {
    return items;
  }

  public void setItems(List<ApiBatchResponseElement> items) {
    this.items = items;
  }

  public ApiBatchResponse success(Boolean success) {
    this.success = success;
    return this;
  }

  /**
   * Read-only. True if every response element's ApiBatchResponseElement#getStatusCode() is in the range [200, 300), false otherwise.
   * @return success
  **/
  @ApiModelProperty(value = "Read-only. True if every response element's ApiBatchResponseElement#getStatusCode() is in the range [200, 300), false otherwise.")


  public Boolean isSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiBatchResponse apiBatchResponse = (ApiBatchResponse) o;
    return Objects.equals(this.items, apiBatchResponse.items) &&
        Objects.equals(this.success, apiBatchResponse.success) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, success, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiBatchResponse {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    success: ").append(toIndentedString(success)).append("\n");
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

