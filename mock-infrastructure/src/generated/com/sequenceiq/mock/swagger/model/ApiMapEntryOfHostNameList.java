package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHostNameList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Similar to ApiMapEntry but the value is an ApiHostNameList.
 */
@ApiModel(description = "Similar to ApiMapEntry but the value is an ApiHostNameList.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiMapEntryOfHostNameList   {
  @JsonProperty("key")
  private String key = null;

  @JsonProperty("value")
  private ApiHostNameList value = null;

  public ApiMapEntryOfHostNameList key(String key) {
    this.key = key;
    return this;
  }

  /**
   * 
   * @return key
  **/
  @ApiModelProperty(value = "")


  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public ApiMapEntryOfHostNameList value(ApiHostNameList value) {
    this.value = value;
    return this;
  }

  /**
   * 
   * @return value
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHostNameList getValue() {
    return value;
  }

  public void setValue(ApiHostNameList value) {
    this.value = value;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiMapEntryOfHostNameList apiMapEntryOfHostNameList = (ApiMapEntryOfHostNameList) o;
    return Objects.equals(this.key, apiMapEntryOfHostNameList.key) &&
        Objects.equals(this.value, apiMapEntryOfHostNameList.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiMapEntryOfHostNameList {\n");
    
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

