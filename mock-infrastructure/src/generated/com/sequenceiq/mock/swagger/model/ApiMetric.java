package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiMetricData;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A metric represents a specific metric monitored by the Cloudera Management Services, and a list of values matching a user query. &lt;p&gt; These fields are available only in the \&quot;full\&quot; view: &lt;ul&gt; &lt;li&gt;displayName&lt;/li&gt; &lt;li&gt;description&lt;/li&gt; &lt;/ul&gt;
 */
@ApiModel(description = "A metric represents a specific metric monitored by the Cloudera Management Services, and a list of values matching a user query. <p> These fields are available only in the \"full\" view: <ul> <li>displayName</li> <li>description</li> </ul>")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiMetric   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("context")
  private String context = null;

  @JsonProperty("unit")
  private String unit = null;

  @JsonProperty("data")
  @Valid
  private List<ApiMetricData> data = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("description")
  private String description = null;

  public ApiMetric name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Name of the metric.
   * @return name
  **/
  @ApiModelProperty(value = "Name of the metric.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiMetric context(String context) {
    this.context = context;
    return this;
  }

  /**
   * Context the metric is associated with.
   * @return context
  **/
  @ApiModelProperty(value = "Context the metric is associated with.")


  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public ApiMetric unit(String unit) {
    this.unit = unit;
    return this;
  }

  /**
   * Unit of the metric values.
   * @return unit
  **/
  @ApiModelProperty(value = "Unit of the metric values.")


  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public ApiMetric data(List<ApiMetricData> data) {
    this.data = data;
    return this;
  }

  public ApiMetric addDataItem(ApiMetricData dataItem) {
    if (this.data == null) {
      this.data = new ArrayList<>();
    }
    this.data.add(dataItem);
    return this;
  }

  /**
   * List of readings retrieved from the monitors.
   * @return data
  **/
  @ApiModelProperty(value = "List of readings retrieved from the monitors.")

  @Valid

  public List<ApiMetricData> getData() {
    return data;
  }

  public void setData(List<ApiMetricData> data) {
    this.data = data;
  }

  public ApiMetric displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Requires \"full\" view. User-friendly display name for the metric.
   * @return displayName
  **/
  @ApiModelProperty(value = "Requires \"full\" view. User-friendly display name for the metric.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiMetric description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Requires \"full\" view. Description of the metric.
   * @return description
  **/
  @ApiModelProperty(value = "Requires \"full\" view. Description of the metric.")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiMetric apiMetric = (ApiMetric) o;
    return Objects.equals(this.name, apiMetric.name) &&
        Objects.equals(this.context, apiMetric.context) &&
        Objects.equals(this.unit, apiMetric.unit) &&
        Objects.equals(this.data, apiMetric.data) &&
        Objects.equals(this.displayName, apiMetric.displayName) &&
        Objects.equals(this.description, apiMetric.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, context, unit, data, displayName, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiMetric {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    context: ").append(toIndentedString(context)).append("\n");
    sb.append("    unit: ").append(toIndentedString(unit)).append("\n");
    sb.append("    data: ").append(toIndentedString(data)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

