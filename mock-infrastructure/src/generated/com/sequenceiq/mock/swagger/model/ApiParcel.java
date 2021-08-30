package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterRef;
import com.sequenceiq.mock.swagger.model.ApiParcelState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A Parcel encapsulate a specific product and version. For example, (CDH 4.1). A parcel is downloaded, distributed to all the machines of a cluster and then allowed to be activated. &lt;p&gt;&gt; The available parcels are determined by which cluster they will be running on. For example, a SLES parcel won&#39;t show up for a RHEL cluster. &lt;/p&gt;
 */
@ApiModel(description = "A Parcel encapsulate a specific product and version. For example, (CDH 4.1). A parcel is downloaded, distributed to all the machines of a cluster and then allowed to be activated. <p>> The available parcels are determined by which cluster they will be running on. For example, a SLES parcel won't show up for a RHEL cluster. </p>")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiParcel   {
  @JsonProperty("product")
  private String product = null;

  @JsonProperty("version")
  private String version = null;

  @JsonProperty("stage")
  private String stage = null;

  @JsonProperty("state")
  private ApiParcelState state = null;

  @JsonProperty("clusterRef")
  private ApiClusterRef clusterRef = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("description")
  private String description = null;

  public ApiParcel product(String product) {
    this.product = product;
    return this;
  }

  /**
   * The name of the product, e.g. CDH, Impala
   * @return product
  **/
  @ApiModelProperty(value = "The name of the product, e.g. CDH, Impala")


  public String getProduct() {
    return product;
  }

  public void setProduct(String product) {
    this.product = product;
  }

  public ApiParcel version(String version) {
    this.version = version;
    return this;
  }

  /**
   * The version of the product, e.g. 1.1.0, 2.3.0.
   * @return version
  **/
  @ApiModelProperty(value = "The version of the product, e.g. 1.1.0, 2.3.0.")


  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public ApiParcel stage(String stage) {
    this.stage = stage;
    return this;
  }

  /**
   * Returns the current stage of the parcel. <p> There are a number of stages a parcel can be in. There are two types of stages - stable and transient. A parcel is in a transient stage when it is transitioning between two stable stages. The stages are listed below with some additional information.  <ul> <li><b>AVAILABLE_REMOTELY</b>: Stable stage - the parcel can be downloaded to the server.</li> <li><b>DOWNLOADING</b>: Transient stage - the parcel is in the process of being downloaded to the server.</li> <li><b>DOWNLOADED</b>: Stable stage - the parcel is downloaded and ready to be distributed or removed from the server.</li> <li><b>DISTRIBUTING</b>: Transient stage - the parcel is being sent to all the hosts in the cluster.</li> <li><b>DISTRIBUTED</b>: Stable stage - the parcel is on all the hosts in the cluster. The parcel can now be activated, or removed from all the hosts.</li> <li><b>UNDISTRIBUTING</b>: Transient stage - the parcel is being removed from all the hosts in the cluster></li> <li><b>ACTIVATING</b>: Transient stage - the parcel is being activated on the hosts in the cluster. <i>New in API v7</i></li> <li><b>ACTIVATED</b>: Steady stage - the parcel is set to active on every host in the cluster. If desired, a parcel can be deactivated from this stage.</li> </ul>
   * @return stage
  **/
  @ApiModelProperty(value = "Returns the current stage of the parcel. <p> There are a number of stages a parcel can be in. There are two types of stages - stable and transient. A parcel is in a transient stage when it is transitioning between two stable stages. The stages are listed below with some additional information.  <ul> <li><b>AVAILABLE_REMOTELY</b>: Stable stage - the parcel can be downloaded to the server.</li> <li><b>DOWNLOADING</b>: Transient stage - the parcel is in the process of being downloaded to the server.</li> <li><b>DOWNLOADED</b>: Stable stage - the parcel is downloaded and ready to be distributed or removed from the server.</li> <li><b>DISTRIBUTING</b>: Transient stage - the parcel is being sent to all the hosts in the cluster.</li> <li><b>DISTRIBUTED</b>: Stable stage - the parcel is on all the hosts in the cluster. The parcel can now be activated, or removed from all the hosts.</li> <li><b>UNDISTRIBUTING</b>: Transient stage - the parcel is being removed from all the hosts in the cluster></li> <li><b>ACTIVATING</b>: Transient stage - the parcel is being activated on the hosts in the cluster. <i>New in API v7</i></li> <li><b>ACTIVATED</b>: Steady stage - the parcel is set to active on every host in the cluster. If desired, a parcel can be deactivated from this stage.</li> </ul>")


  public String getStage() {
    return stage;
  }

  public void setStage(String stage) {
    this.stage = stage;
  }

  public ApiParcel state(ApiParcelState state) {
    this.state = state;
    return this;
  }

  /**
   * The state of the parcel. This shows the progress of state transitions and if there were any errors.
   * @return state
  **/
  @ApiModelProperty(value = "The state of the parcel. This shows the progress of state transitions and if there were any errors.")

  @Valid

  public ApiParcelState getState() {
    return state;
  }

  public void setState(ApiParcelState state) {
    this.state = state;
  }

  public ApiParcel clusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
    return this;
  }

  /**
   * Readonly. A reference to the enclosing cluster.
   * @return clusterRef
  **/
  @ApiModelProperty(value = "Readonly. A reference to the enclosing cluster.")

  @Valid

  public ApiClusterRef getClusterRef() {
    return clusterRef;
  }

  public void setClusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
  }

  public ApiParcel displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Read-only. Display name of the parcel. If set, available since v40.
   * @return displayName
  **/
  @ApiModelProperty(value = "Read-only. Display name of the parcel. If set, available since v40.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiParcel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Read-only. Description of the parcel. If set, available since v40.
   * @return description
  **/
  @ApiModelProperty(value = "Read-only. Description of the parcel. If set, available since v40.")


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
    ApiParcel apiParcel = (ApiParcel) o;
    return Objects.equals(this.product, apiParcel.product) &&
        Objects.equals(this.version, apiParcel.version) &&
        Objects.equals(this.stage, apiParcel.stage) &&
        Objects.equals(this.state, apiParcel.state) &&
        Objects.equals(this.clusterRef, apiParcel.clusterRef) &&
        Objects.equals(this.displayName, apiParcel.displayName) &&
        Objects.equals(this.description, apiParcel.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(product, version, stage, state, clusterRef, displayName, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiParcel {\n");
    
    sb.append("    product: ").append(toIndentedString(product)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    stage: ").append(toIndentedString(stage)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    clusterRef: ").append(toIndentedString(clusterRef)).append("\n");
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

