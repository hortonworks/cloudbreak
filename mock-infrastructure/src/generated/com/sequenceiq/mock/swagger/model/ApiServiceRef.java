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
 * A serviceRef references a service. It is identified by the \&quot;serviceName\&quot;, \&quot;clusterName\&quot; (name of the cluster which the service belongs to) and an optional \&quot;peerName\&quot; (to reference a remote service i.e. services managed by other CM instances). To operate on the service object, use the API with those fields as parameters.
 */
@ApiModel(description = "A serviceRef references a service. It is identified by the \"serviceName\", \"clusterName\" (name of the cluster which the service belongs to) and an optional \"peerName\" (to reference a remote service i.e. services managed by other CM instances). To operate on the service object, use the API with those fields as parameters.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiServiceRef   {
  @JsonProperty("peerName")
  private String peerName = null;

  @JsonProperty("clusterName")
  private String clusterName = null;

  @JsonProperty("serviceName")
  private String serviceName = null;

  @JsonProperty("serviceDisplayName")
  private String serviceDisplayName = null;

  @JsonProperty("serviceType")
  private String serviceType = null;

  public ApiServiceRef peerName(String peerName) {
    this.peerName = peerName;
    return this;
  }

  /**
   * The name of the CM peer corresponding to the remote CM that manages the referenced service. This should only be set when referencing a remote service.
   * @return peerName
  **/
  @ApiModelProperty(value = "The name of the CM peer corresponding to the remote CM that manages the referenced service. This should only be set when referencing a remote service.")


  public String getPeerName() {
    return peerName;
  }

  public void setPeerName(String peerName) {
    this.peerName = peerName;
  }

  public ApiServiceRef clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  /**
   * The enclosing cluster for this service.
   * @return clusterName
  **/
  @ApiModelProperty(value = "The enclosing cluster for this service.")


  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public ApiServiceRef serviceName(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  /**
   * The service name.
   * @return serviceName
  **/
  @ApiModelProperty(value = "The service name.")


  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public ApiServiceRef serviceDisplayName(String serviceDisplayName) {
    this.serviceDisplayName = serviceDisplayName;
    return this;
  }

  /**
   * 
   * @return serviceDisplayName
  **/
  @ApiModelProperty(value = "")


  public String getServiceDisplayName() {
    return serviceDisplayName;
  }

  public void setServiceDisplayName(String serviceDisplayName) {
    this.serviceDisplayName = serviceDisplayName;
  }

  public ApiServiceRef serviceType(String serviceType) {
    this.serviceType = serviceType;
    return this;
  }

  /**
   * The service type. This is available since version 32
   * @return serviceType
  **/
  @ApiModelProperty(value = "The service type. This is available since version 32")


  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiServiceRef apiServiceRef = (ApiServiceRef) o;
    return Objects.equals(this.peerName, apiServiceRef.peerName) &&
        Objects.equals(this.clusterName, apiServiceRef.clusterName) &&
        Objects.equals(this.serviceName, apiServiceRef.serviceName) &&
        Objects.equals(this.serviceDisplayName, apiServiceRef.serviceDisplayName) &&
        Objects.equals(this.serviceType, apiServiceRef.serviceType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(peerName, clusterName, serviceName, serviceDisplayName, serviceType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiServiceRef {\n");
    
    sb.append("    peerName: ").append(toIndentedString(peerName)).append("\n");
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    serviceName: ").append(toIndentedString(serviceName)).append("\n");
    sb.append("    serviceDisplayName: ").append(toIndentedString(serviceDisplayName)).append("\n");
    sb.append("    serviceType: ").append(toIndentedString(serviceType)).append("\n");
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

