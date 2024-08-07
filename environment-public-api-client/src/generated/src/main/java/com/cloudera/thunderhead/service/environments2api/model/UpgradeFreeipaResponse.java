/*
 * Cloudera Environments Service
 * Cloudera Environments Service is a web service that manages cloud provider access.
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.thunderhead.service.environments2api.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.cloudera.thunderhead.service.environments2api.model.ImageInfoResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * The response object for FreeIPA upgrade.
 */
@JsonPropertyOrder({
  UpgradeFreeipaResponse.JSON_PROPERTY_TARGET_IMAGE,
  UpgradeFreeipaResponse.JSON_PROPERTY_ORIGINAL_IMAGE,
  UpgradeFreeipaResponse.JSON_PROPERTY_OPERATION_ID
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class UpgradeFreeipaResponse {
  public static final String JSON_PROPERTY_TARGET_IMAGE = "targetImage";
  private ImageInfoResponse targetImage;

  public static final String JSON_PROPERTY_ORIGINAL_IMAGE = "originalImage";
  private ImageInfoResponse originalImage;

  public static final String JSON_PROPERTY_OPERATION_ID = "operationId";
  private String operationId;

  public UpgradeFreeipaResponse() { 
  }

  public UpgradeFreeipaResponse targetImage(ImageInfoResponse targetImage) {
    this.targetImage = targetImage;
    return this;
  }

   /**
   * Get targetImage
   * @return targetImage
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TARGET_IMAGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public ImageInfoResponse getTargetImage() {
    return targetImage;
  }


  @JsonProperty(JSON_PROPERTY_TARGET_IMAGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTargetImage(ImageInfoResponse targetImage) {
    this.targetImage = targetImage;
  }


  public UpgradeFreeipaResponse originalImage(ImageInfoResponse originalImage) {
    this.originalImage = originalImage;
    return this;
  }

   /**
   * Get originalImage
   * @return originalImage
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ORIGINAL_IMAGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public ImageInfoResponse getOriginalImage() {
    return originalImage;
  }


  @JsonProperty(JSON_PROPERTY_ORIGINAL_IMAGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOriginalImage(ImageInfoResponse originalImage) {
    this.originalImage = originalImage;
  }


  public UpgradeFreeipaResponse operationId(String operationId) {
    this.operationId = operationId;
    return this;
  }

   /**
   * UUID of the request for this operation. This ID can be used for geting status on the operation.
   * @return operationId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OPERATION_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getOperationId() {
    return operationId;
  }


  @JsonProperty(JSON_PROPERTY_OPERATION_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }


  /**
   * Return true if this UpgradeFreeipaResponse object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpgradeFreeipaResponse upgradeFreeipaResponse = (UpgradeFreeipaResponse) o;
    return Objects.equals(this.targetImage, upgradeFreeipaResponse.targetImage) &&
        Objects.equals(this.originalImage, upgradeFreeipaResponse.originalImage) &&
        Objects.equals(this.operationId, upgradeFreeipaResponse.operationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetImage, originalImage, operationId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpgradeFreeipaResponse {\n");
    sb.append("    targetImage: ").append(toIndentedString(targetImage)).append("\n");
    sb.append("    originalImage: ").append(toIndentedString(originalImage)).append("\n");
    sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `targetImage` to the URL query string
    if (getTargetImage() != null) {
      joiner.add(getTargetImage().toUrlQueryString(prefix + "targetImage" + suffix));
    }

    // add `originalImage` to the URL query string
    if (getOriginalImage() != null) {
      joiner.add(getOriginalImage().toUrlQueryString(prefix + "originalImage" + suffix));
    }

    // add `operationId` to the URL query string
    if (getOperationId() != null) {
      joiner.add(String.format("%soperationId%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getOperationId()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

