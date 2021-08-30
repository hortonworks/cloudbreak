package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used when enabling HDFS automatic failover.
 */
@ApiModel(description = "Arguments used when enabling HDFS automatic failover.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsFailoverArguments   {
  @JsonProperty("nameservice")
  private String nameservice = null;

  @JsonProperty("zooKeeperService")
  private ApiServiceRef zooKeeperService = null;

  @JsonProperty("activeFCName")
  private String activeFCName = null;

  @JsonProperty("standByFCName")
  private String standByFCName = null;

  public ApiHdfsFailoverArguments nameservice(String nameservice) {
    this.nameservice = nameservice;
    return this;
  }

  /**
   * Nameservice for which to enable automatic failover.
   * @return nameservice
  **/
  @ApiModelProperty(value = "Nameservice for which to enable automatic failover.")


  public String getNameservice() {
    return nameservice;
  }

  public void setNameservice(String nameservice) {
    this.nameservice = nameservice;
  }

  public ApiHdfsFailoverArguments zooKeeperService(ApiServiceRef zooKeeperService) {
    this.zooKeeperService = zooKeeperService;
    return this;
  }

  /**
   * The ZooKeeper service to use.
   * @return zooKeeperService
  **/
  @ApiModelProperty(value = "The ZooKeeper service to use.")

  @Valid

  public ApiServiceRef getZooKeeperService() {
    return zooKeeperService;
  }

  public void setZooKeeperService(ApiServiceRef zooKeeperService) {
    this.zooKeeperService = zooKeeperService;
  }

  public ApiHdfsFailoverArguments activeFCName(String activeFCName) {
    this.activeFCName = activeFCName;
    return this;
  }

  /**
   * Name of the failover controller to create for the active NameNode.
   * @return activeFCName
  **/
  @ApiModelProperty(value = "Name of the failover controller to create for the active NameNode.")


  public String getActiveFCName() {
    return activeFCName;
  }

  public void setActiveFCName(String activeFCName) {
    this.activeFCName = activeFCName;
  }

  public ApiHdfsFailoverArguments standByFCName(String standByFCName) {
    this.standByFCName = standByFCName;
    return this;
  }

  /**
   * Name of the failover controller to create for the stand-by NameNode.
   * @return standByFCName
  **/
  @ApiModelProperty(value = "Name of the failover controller to create for the stand-by NameNode.")


  public String getStandByFCName() {
    return standByFCName;
  }

  public void setStandByFCName(String standByFCName) {
    this.standByFCName = standByFCName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsFailoverArguments apiHdfsFailoverArguments = (ApiHdfsFailoverArguments) o;
    return Objects.equals(this.nameservice, apiHdfsFailoverArguments.nameservice) &&
        Objects.equals(this.zooKeeperService, apiHdfsFailoverArguments.zooKeeperService) &&
        Objects.equals(this.activeFCName, apiHdfsFailoverArguments.activeFCName) &&
        Objects.equals(this.standByFCName, apiHdfsFailoverArguments.standByFCName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nameservice, zooKeeperService, activeFCName, standByFCName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsFailoverArguments {\n");
    
    sb.append("    nameservice: ").append(toIndentedString(nameservice)).append("\n");
    sb.append("    zooKeeperService: ").append(toIndentedString(zooKeeperService)).append("\n");
    sb.append("    activeFCName: ").append(toIndentedString(activeFCName)).append("\n");
    sb.append("    standByFCName: ").append(toIndentedString(standByFCName)).append("\n");
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

