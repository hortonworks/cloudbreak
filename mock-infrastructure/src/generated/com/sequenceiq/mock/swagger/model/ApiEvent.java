package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiEventAttribute;
import com.sequenceiq.mock.swagger.model.ApiEventCategory;
import com.sequenceiq.mock.swagger.model.ApiEventSeverity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Events model noteworthy incidents in Cloudera Manager or the managed Hadoop cluster. An event carries its event category, severity, and a string content. They also have generic attributes, which are free-form key value pairs. Important events may be promoted into alerts.
 */
@ApiModel(description = "Events model noteworthy incidents in Cloudera Manager or the managed Hadoop cluster. An event carries its event category, severity, and a string content. They also have generic attributes, which are free-form key value pairs. Important events may be promoted into alerts.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEvent   {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("content")
  private String content = null;

  @JsonProperty("timeOccurred")
  private String timeOccurred = null;

  @JsonProperty("timeReceived")
  private String timeReceived = null;

  @JsonProperty("category")
  private ApiEventCategory category = null;

  @JsonProperty("severity")
  private ApiEventSeverity severity = null;

  @JsonProperty("alert")
  private Boolean alert = null;

  @JsonProperty("attributes")
  @Valid
  private List<ApiEventAttribute> attributes = null;

  public ApiEvent id(String id) {
    this.id = id;
    return this;
  }

  /**
   * A unique ID for this event.
   * @return id
  **/
  @ApiModelProperty(value = "A unique ID for this event.")


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ApiEvent content(String content) {
    this.content = content;
    return this;
  }

  /**
   * The content payload of this event.
   * @return content
  **/
  @ApiModelProperty(value = "The content payload of this event.")


  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public ApiEvent timeOccurred(String timeOccurred) {
    this.timeOccurred = timeOccurred;
    return this;
  }

  /**
   * When the event was generated.
   * @return timeOccurred
  **/
  @ApiModelProperty(value = "When the event was generated.")


  public String getTimeOccurred() {
    return timeOccurred;
  }

  public void setTimeOccurred(String timeOccurred) {
    this.timeOccurred = timeOccurred;
  }

  public ApiEvent timeReceived(String timeReceived) {
    this.timeReceived = timeReceived;
    return this;
  }

  /**
   * When the event was stored by Cloudera Manager. Events do not arrive in the order that they are generated. If you are writing an event poller, this is a useful field to query.
   * @return timeReceived
  **/
  @ApiModelProperty(value = "When the event was stored by Cloudera Manager. Events do not arrive in the order that they are generated. If you are writing an event poller, this is a useful field to query.")


  public String getTimeReceived() {
    return timeReceived;
  }

  public void setTimeReceived(String timeReceived) {
    this.timeReceived = timeReceived;
  }

  public ApiEvent category(ApiEventCategory category) {
    this.category = category;
    return this;
  }

  /**
   * The category of this event -- whether it is a health event, an audit event, an activity event, etc.
   * @return category
  **/
  @ApiModelProperty(value = "The category of this event -- whether it is a health event, an audit event, an activity event, etc.")

  @Valid

  public ApiEventCategory getCategory() {
    return category;
  }

  public void setCategory(ApiEventCategory category) {
    this.category = category;
  }

  public ApiEvent severity(ApiEventSeverity severity) {
    this.severity = severity;
    return this;
  }

  /**
   * The severity of the event.
   * @return severity
  **/
  @ApiModelProperty(value = "The severity of the event.")

  @Valid

  public ApiEventSeverity getSeverity() {
    return severity;
  }

  public void setSeverity(ApiEventSeverity severity) {
    this.severity = severity;
  }

  public ApiEvent alert(Boolean alert) {
    this.alert = alert;
    return this;
  }

  /**
   * Whether the event is promoted to an alert according to configuration. Defaults to false
   * @return alert
  **/
  @ApiModelProperty(value = "Whether the event is promoted to an alert according to configuration. Defaults to false")


  public Boolean isAlert() {
    return alert;
  }

  public void setAlert(Boolean alert) {
    this.alert = alert;
  }

  public ApiEvent attributes(List<ApiEventAttribute> attributes) {
    this.attributes = attributes;
    return this;
  }

  public ApiEvent addAttributesItem(ApiEventAttribute attributesItem) {
    if (this.attributes == null) {
      this.attributes = new ArrayList<>();
    }
    this.attributes.add(attributesItem);
    return this;
  }

  /**
   * A list of key-value attribute pairs.
   * @return attributes
  **/
  @ApiModelProperty(value = "A list of key-value attribute pairs.")

  @Valid

  public List<ApiEventAttribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<ApiEventAttribute> attributes) {
    this.attributes = attributes;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEvent apiEvent = (ApiEvent) o;
    return Objects.equals(this.id, apiEvent.id) &&
        Objects.equals(this.content, apiEvent.content) &&
        Objects.equals(this.timeOccurred, apiEvent.timeOccurred) &&
        Objects.equals(this.timeReceived, apiEvent.timeReceived) &&
        Objects.equals(this.category, apiEvent.category) &&
        Objects.equals(this.severity, apiEvent.severity) &&
        Objects.equals(this.alert, apiEvent.alert) &&
        Objects.equals(this.attributes, apiEvent.attributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, content, timeOccurred, timeReceived, category, severity, alert, attributes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEvent {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    timeOccurred: ").append(toIndentedString(timeOccurred)).append("\n");
    sb.append("    timeReceived: ").append(toIndentedString(timeReceived)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    alert: ").append(toIndentedString(alert)).append("\n");
    sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
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

