package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Encapsulates information needed to utilize the s3 Gateway API- namely, the AWS credentials, the rest url of the s3 Gateway and the S3 bucket.
 */
@ApiModel(description = "Encapsulates information needed to utilize the s3 Gateway API- namely, the AWS credentials, the rest url of the s3 Gateway and the S3 bucket.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiOzoneS3GatewayInfo   {
  @JsonProperty("awsAccessKey")
  private String awsAccessKey = null;

  @JsonProperty("awsSecret")
  private String awsSecret = null;

  @JsonProperty("restUrl")
  private String restUrl = null;

  @JsonProperty("bucket")
  private String bucket = null;

  public ApiOzoneS3GatewayInfo awsAccessKey(String awsAccessKey) {
    this.awsAccessKey = awsAccessKey;
    return this;
  }

  /**
   * The AWS access key for a particular Ozone service
   * @return awsAccessKey
  **/
  @ApiModelProperty(value = "The AWS access key for a particular Ozone service")


  public String getAwsAccessKey() {
    return awsAccessKey;
  }

  public void setAwsAccessKey(String awsAccessKey) {
    this.awsAccessKey = awsAccessKey;
  }

  public ApiOzoneS3GatewayInfo awsSecret(String awsSecret) {
    this.awsSecret = awsSecret;
    return this;
  }

  /**
   * The AWS secret associated with the access key
   * @return awsSecret
  **/
  @ApiModelProperty(value = "The AWS secret associated with the access key")


  public String getAwsSecret() {
    return awsSecret;
  }

  public void setAwsSecret(String awsSecret) {
    this.awsSecret = awsSecret;
  }

  public ApiOzoneS3GatewayInfo restUrl(String restUrl) {
    this.restUrl = restUrl;
    return this;
  }

  /**
   * The rest url, in host:port format, of the Ozone S3 Gateway
   * @return restUrl
  **/
  @ApiModelProperty(value = "The rest url, in host:port format, of the Ozone S3 Gateway")


  public String getRestUrl() {
    return restUrl;
  }

  public void setRestUrl(String restUrl) {
    this.restUrl = restUrl;
  }

  public ApiOzoneS3GatewayInfo bucket(String bucket) {
    this.bucket = bucket;
    return this;
  }

  /**
   * The Ozone address of the created S3 bucket
   * @return bucket
  **/
  @ApiModelProperty(value = "The Ozone address of the created S3 bucket")


  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiOzoneS3GatewayInfo apiOzoneS3GatewayInfo = (ApiOzoneS3GatewayInfo) o;
    return Objects.equals(this.awsAccessKey, apiOzoneS3GatewayInfo.awsAccessKey) &&
        Objects.equals(this.awsSecret, apiOzoneS3GatewayInfo.awsSecret) &&
        Objects.equals(this.restUrl, apiOzoneS3GatewayInfo.restUrl) &&
        Objects.equals(this.bucket, apiOzoneS3GatewayInfo.bucket);
  }

  @Override
  public int hashCode() {
    return Objects.hash(awsAccessKey, awsSecret, restUrl, bucket);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiOzoneS3GatewayInfo {\n");
    
    sb.append("    awsAccessKey: ").append(toIndentedString(awsAccessKey)).append("\n");
    sb.append("    awsSecret: ").append(toIndentedString(awsSecret)).append("\n");
    sb.append("    restUrl: ").append(toIndentedString(restUrl)).append("\n");
    sb.append("    bucket: ").append(toIndentedString(bucket)).append("\n");
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

