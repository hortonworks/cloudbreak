/*
 * CDP Service Discovery
 * CDP Service Discovery is a web service that provides information to a workload service
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.cdp.servicediscovery.model;

import java.util.Objects;
import java.util.Arrays;
import com.cloudera.cdp.servicediscovery.model.WarehouseService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Information on a virtual warehouse.
 */
@JsonPropertyOrder({
  VirtualWarehouse.JSON_PROPERTY_CRN,
  VirtualWarehouse.JSON_PROPERTY_ID,
  VirtualWarehouse.JSON_PROPERTY_NAME,
  VirtualWarehouse.JSON_PROPERTY_TYPE,
  VirtualWarehouse.JSON_PROPERTY_CLUSTER_ID,
  VirtualWarehouse.JSON_PROPERTY_DBC_ID,
  VirtualWarehouse.JSON_PROPERTY_STATUS,
  VirtualWarehouse.JSON_PROPERTY_SERVICES
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class VirtualWarehouse {
  public static final String JSON_PROPERTY_CRN = "crn";
  private String crn;

  public static final String JSON_PROPERTY_ID = "id";
  private String id;

  public static final String JSON_PROPERTY_NAME = "name";
  private String name;

  public static final String JSON_PROPERTY_TYPE = "type";
  private String type;

  public static final String JSON_PROPERTY_CLUSTER_ID = "clusterId";
  private String clusterId;

  public static final String JSON_PROPERTY_DBC_ID = "dbcId";
  private String dbcId;

  public static final String JSON_PROPERTY_STATUS = "status";
  private String status;

  public static final String JSON_PROPERTY_SERVICES = "services";
  private Map<String, WarehouseService> services = new HashMap<>();

  public VirtualWarehouse() {
  }

  public VirtualWarehouse crn(String crn) {
    
    this.crn = crn;
    return this;
  }

   /**
   * crn
   * @return crn
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CRN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getCrn() {
    return crn;
  }


  @JsonProperty(JSON_PROPERTY_CRN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCrn(String crn) {
    this.crn = crn;
  }


  public VirtualWarehouse id(String id) {
    
    this.id = id;
    return this;
  }

   /**
   * id
   * @return id
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getId() {
    return id;
  }


  @JsonProperty(JSON_PROPERTY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setId(String id) {
    this.id = id;
  }


  public VirtualWarehouse name(String name) {
    
    this.name = name;
    return this;
  }

   /**
   * name
   * @return name
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getName() {
    return name;
  }


  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setName(String name) {
    this.name = name;
  }


  public VirtualWarehouse type(String type) {
    
    this.type = type;
    return this;
  }

   /**
   * type
   * @return type
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getType() {
    return type;
  }


  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setType(String type) {
    this.type = type;
  }


  public VirtualWarehouse clusterId(String clusterId) {
    
    this.clusterId = clusterId;
    return this;
  }

   /**
   * clusterId
   * @return clusterId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CLUSTER_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getClusterId() {
    return clusterId;
  }


  @JsonProperty(JSON_PROPERTY_CLUSTER_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setClusterId(String clusterId) {
    this.clusterId = clusterId;
  }


  public VirtualWarehouse dbcId(String dbcId) {
    
    this.dbcId = dbcId;
    return this;
  }

   /**
   * dbcId
   * @return dbcId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DBC_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getDbcId() {
    return dbcId;
  }


  @JsonProperty(JSON_PROPERTY_DBC_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDbcId(String dbcId) {
    this.dbcId = dbcId;
  }


  public VirtualWarehouse status(String status) {
    
    this.status = status;
    return this;
  }

   /**
   * status
   * @return status
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getStatus() {
    return status;
  }


  @JsonProperty(JSON_PROPERTY_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStatus(String status) {
    this.status = status;
  }


  public VirtualWarehouse services(Map<String, WarehouseService> services) {
    
    this.services = services;
    return this;
  }

  public VirtualWarehouse putServicesItem(String key, WarehouseService servicesItem) {
    if (this.services == null) {
      this.services = new HashMap<>();
    }
    this.services.put(key, servicesItem);
    return this;
  }

   /**
   * The services that make up the warehouse
   * @return services
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SERVICES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Map<String, WarehouseService> getServices() {
    return services;
  }


  @JsonProperty(JSON_PROPERTY_SERVICES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setServices(Map<String, WarehouseService> services) {
    this.services = services;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VirtualWarehouse virtualWarehouse = (VirtualWarehouse) o;
    return Objects.equals(this.crn, virtualWarehouse.crn) &&
        Objects.equals(this.id, virtualWarehouse.id) &&
        Objects.equals(this.name, virtualWarehouse.name) &&
        Objects.equals(this.type, virtualWarehouse.type) &&
        Objects.equals(this.clusterId, virtualWarehouse.clusterId) &&
        Objects.equals(this.dbcId, virtualWarehouse.dbcId) &&
        Objects.equals(this.status, virtualWarehouse.status) &&
        Objects.equals(this.services, virtualWarehouse.services);
  }

  @Override
  public int hashCode() {
    return Objects.hash(crn, id, name, type, clusterId, dbcId, status, services);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VirtualWarehouse {\n");
    sb.append("    crn: ").append(toIndentedString(crn)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    clusterId: ").append(toIndentedString(clusterId)).append("\n");
    sb.append("    dbcId: ").append(toIndentedString(dbcId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    services: ").append(toIndentedString(services)).append("\n");
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

}

