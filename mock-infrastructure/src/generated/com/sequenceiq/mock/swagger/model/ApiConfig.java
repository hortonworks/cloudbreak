package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ValidationState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Model for a configuration parameter.  When an entry&#39;s &lt;i&gt;value&lt;/i&gt; property is not available, it means the entry is not configured. This means that the default value for the entry, if any, will be used. Setting a value to &lt;i&gt;null&lt;/i&gt; also can be used to unset any previously set value for the parameter, reverting to the default value (if any).
 */
@ApiModel(description = "Model for a configuration parameter.  When an entry's <i>value</i> property is not available, it means the entry is not configured. This means that the default value for the entry, if any, will be used. Setting a value to <i>null</i> also can be used to unset any previously set value for the parameter, reverting to the default value (if any).")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiConfig   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("value")
  private String value = null;

  @JsonProperty("required")
  private Boolean required = null;

  @JsonProperty("default")
  private String _default = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("relatedName")
  private String relatedName = null;

  @JsonProperty("sensitive")
  private Boolean sensitive = null;

  @JsonProperty("validationState")
  private ValidationState validationState = null;

  @JsonProperty("validationMessage")
  private String validationMessage = null;

  @JsonProperty("validationWarningsSuppressed")
  private Boolean validationWarningsSuppressed = null;

  public ApiConfig name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Readonly. The canonical name that identifies this configuration parameter.
   * @return name
  **/
  @ApiModelProperty(value = "Readonly. The canonical name that identifies this configuration parameter.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiConfig value(String value) {
    this.value = value;
    return this;
  }

  /**
   * The user-defined value. When absent, the default value (if any) will be used. Can also be absent, when enumerating allowed configs.
   * @return value
  **/
  @ApiModelProperty(value = "The user-defined value. When absent, the default value (if any) will be used. Can also be absent, when enumerating allowed configs.")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public ApiConfig required(Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. Whether this configuration is required for the object. If any required configuration is not set, operations on the object may not work.
   * @return required
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. Whether this configuration is required for the object. If any required configuration is not set, operations on the object may not work.")


  public Boolean isRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public ApiConfig _default(String _default) {
    this._default = _default;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. The default value.
   * @return _default
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. The default value.")


  public String getDefault() {
    return _default;
  }

  public void setDefault(String _default) {
    this._default = _default;
  }

  public ApiConfig displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. A user-friendly name of the parameters, as would have been shown in the web UI.
   * @return displayName
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. A user-friendly name of the parameters, as would have been shown in the web UI.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiConfig description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. A textual description of the parameter.
   * @return description
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. A textual description of the parameter.")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ApiConfig relatedName(String relatedName) {
    this.relatedName = relatedName;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. If applicable, contains the related configuration variable used by the source project.
   * @return relatedName
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. If applicable, contains the related configuration variable used by the source project.")


  public String getRelatedName() {
    return relatedName;
  }

  public void setRelatedName(String relatedName) {
    this.relatedName = relatedName;
  }

  public ApiConfig sensitive(Boolean sensitive) {
    this.sensitive = sensitive;
    return this;
  }

  /**
   * Readonly. Whether this configuration is sensitive, i.e. contains information such as passwords, which might affect how the value of this configuration might be shared by the caller.  Available since v14.
   * @return sensitive
  **/
  @ApiModelProperty(value = "Readonly. Whether this configuration is sensitive, i.e. contains information such as passwords, which might affect how the value of this configuration might be shared by the caller.  Available since v14.")


  public Boolean isSensitive() {
    return sensitive;
  }

  public void setSensitive(Boolean sensitive) {
    this.sensitive = sensitive;
  }

  public ApiConfig validationState(ValidationState validationState) {
    this.validationState = validationState;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. State of the configuration parameter after validation.
   * @return validationState
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. State of the configuration parameter after validation.")

  @Valid

  public ValidationState getValidationState() {
    return validationState;
  }

  public void setValidationState(ValidationState validationState) {
    this.validationState = validationState;
  }

  public ApiConfig validationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. A message explaining the parameter's validation state.
   * @return validationMessage
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. A message explaining the parameter's validation state.")


  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }

  public ApiConfig validationWarningsSuppressed(Boolean validationWarningsSuppressed) {
    this.validationWarningsSuppressed = validationWarningsSuppressed;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. Whether validation warnings associated with this parameter are suppressed. In general, suppressed validation warnings are hidden in the Cloudera Manager UI. Configurations that do not produce warnings will not contain this field.
   * @return validationWarningsSuppressed
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. Whether validation warnings associated with this parameter are suppressed. In general, suppressed validation warnings are hidden in the Cloudera Manager UI. Configurations that do not produce warnings will not contain this field.")


  public Boolean isValidationWarningsSuppressed() {
    return validationWarningsSuppressed;
  }

  public void setValidationWarningsSuppressed(Boolean validationWarningsSuppressed) {
    this.validationWarningsSuppressed = validationWarningsSuppressed;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiConfig apiConfig = (ApiConfig) o;
    return Objects.equals(this.name, apiConfig.name) &&
        Objects.equals(this.value, apiConfig.value) &&
        Objects.equals(this.required, apiConfig.required) &&
        Objects.equals(this._default, apiConfig._default) &&
        Objects.equals(this.displayName, apiConfig.displayName) &&
        Objects.equals(this.description, apiConfig.description) &&
        Objects.equals(this.relatedName, apiConfig.relatedName) &&
        Objects.equals(this.sensitive, apiConfig.sensitive) &&
        Objects.equals(this.validationState, apiConfig.validationState) &&
        Objects.equals(this.validationMessage, apiConfig.validationMessage) &&
        Objects.equals(this.validationWarningsSuppressed, apiConfig.validationWarningsSuppressed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value, required, _default, displayName, description, relatedName, sensitive, validationState, validationMessage, validationWarningsSuppressed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiConfig {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
    sb.append("    _default: ").append(toIndentedString(_default)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    relatedName: ").append(toIndentedString(relatedName)).append("\n");
    sb.append("    sensitive: ").append(toIndentedString(sensitive)).append("\n");
    sb.append("    validationState: ").append(toIndentedString(validationState)).append("\n");
    sb.append("    validationMessage: ").append(toIndentedString(validationMessage)).append("\n");
    sb.append("    validationWarningsSuppressed: ").append(toIndentedString(validationWarningsSuppressed)).append("\n");
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

