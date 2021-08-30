package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHealthCheck;
import com.sequenceiq.mock.swagger.model.ApiHealthSummary;
import com.sequenceiq.mock.swagger.model.ApiRoleRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Provides information about an HDFS nameservice. &lt;p&gt; Nameservices can be either a stand-alone NameNode, a NameNode paired with a SecondaryNameNode, or a high-availability pair formed by an active and a stand-by NameNode. &lt;p&gt; The following fields are only available in the object&#39;s full view: &lt;ul&gt; &lt;li&gt;healthSummary&lt;/li&gt; &lt;li&gt;healthChecks&lt;/li&gt; &lt;/ul&gt;
 */
@ApiModel(description = "Provides information about an HDFS nameservice. <p> Nameservices can be either a stand-alone NameNode, a NameNode paired with a SecondaryNameNode, or a high-availability pair formed by an active and a stand-by NameNode. <p> The following fields are only available in the object's full view: <ul> <li>healthSummary</li> <li>healthChecks</li> </ul>")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiNameservice   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("active")
  private ApiRoleRef active = null;

  @JsonProperty("activeFailoverController")
  private ApiRoleRef activeFailoverController = null;

  @JsonProperty("standBy")
  private ApiRoleRef standBy = null;

  @JsonProperty("standByFailoverController")
  private ApiRoleRef standByFailoverController = null;

  @JsonProperty("secondary")
  private ApiRoleRef secondary = null;

  @JsonProperty("mountPoints")
  @Valid
  private List<String> mountPoints = null;

  @JsonProperty("healthSummary")
  private ApiHealthSummary healthSummary = null;

  @JsonProperty("healthChecks")
  @Valid
  private List<ApiHealthCheck> healthChecks = null;

  public ApiNameservice name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Name of the nameservice.
   * @return name
  **/
  @ApiModelProperty(value = "Name of the nameservice.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiNameservice active(ApiRoleRef active) {
    this.active = active;
    return this;
  }

  /**
   * Reference to the active NameNode.
   * @return active
  **/
  @ApiModelProperty(value = "Reference to the active NameNode.")

  @Valid

  public ApiRoleRef getActive() {
    return active;
  }

  public void setActive(ApiRoleRef active) {
    this.active = active;
  }

  public ApiNameservice activeFailoverController(ApiRoleRef activeFailoverController) {
    this.activeFailoverController = activeFailoverController;
    return this;
  }

  /**
   * Reference to the active NameNode's failover controller, if configured.
   * @return activeFailoverController
  **/
  @ApiModelProperty(value = "Reference to the active NameNode's failover controller, if configured.")

  @Valid

  public ApiRoleRef getActiveFailoverController() {
    return activeFailoverController;
  }

  public void setActiveFailoverController(ApiRoleRef activeFailoverController) {
    this.activeFailoverController = activeFailoverController;
  }

  public ApiNameservice standBy(ApiRoleRef standBy) {
    this.standBy = standBy;
    return this;
  }

  /**
   * Reference to the stand-by NameNode.
   * @return standBy
  **/
  @ApiModelProperty(value = "Reference to the stand-by NameNode.")

  @Valid

  public ApiRoleRef getStandBy() {
    return standBy;
  }

  public void setStandBy(ApiRoleRef standBy) {
    this.standBy = standBy;
  }

  public ApiNameservice standByFailoverController(ApiRoleRef standByFailoverController) {
    this.standByFailoverController = standByFailoverController;
    return this;
  }

  /**
   * Reference to the stand-by NameNode's failover controller, if configured.
   * @return standByFailoverController
  **/
  @ApiModelProperty(value = "Reference to the stand-by NameNode's failover controller, if configured.")

  @Valid

  public ApiRoleRef getStandByFailoverController() {
    return standByFailoverController;
  }

  public void setStandByFailoverController(ApiRoleRef standByFailoverController) {
    this.standByFailoverController = standByFailoverController;
  }

  public ApiNameservice secondary(ApiRoleRef secondary) {
    this.secondary = secondary;
    return this;
  }

  /**
   * Reference to the SecondaryNameNode.
   * @return secondary
  **/
  @ApiModelProperty(value = "Reference to the SecondaryNameNode.")

  @Valid

  public ApiRoleRef getSecondary() {
    return secondary;
  }

  public void setSecondary(ApiRoleRef secondary) {
    this.secondary = secondary;
  }

  public ApiNameservice mountPoints(List<String> mountPoints) {
    this.mountPoints = mountPoints;
    return this;
  }

  public ApiNameservice addMountPointsItem(String mountPointsItem) {
    if (this.mountPoints == null) {
      this.mountPoints = new ArrayList<>();
    }
    this.mountPoints.add(mountPointsItem);
    return this;
  }

  /**
   * Mount points assigned to this nameservice in a federation.
   * @return mountPoints
  **/
  @ApiModelProperty(value = "Mount points assigned to this nameservice in a federation.")


  public List<String> getMountPoints() {
    return mountPoints;
  }

  public void setMountPoints(List<String> mountPoints) {
    this.mountPoints = mountPoints;
  }

  public ApiNameservice healthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
    return this;
  }

  /**
   * Requires \"full\" view. The high-level health status of this nameservice.
   * @return healthSummary
  **/
  @ApiModelProperty(value = "Requires \"full\" view. The high-level health status of this nameservice.")

  @Valid

  public ApiHealthSummary getHealthSummary() {
    return healthSummary;
  }

  public void setHealthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
  }

  public ApiNameservice healthChecks(List<ApiHealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
    return this;
  }

  public ApiNameservice addHealthChecksItem(ApiHealthCheck healthChecksItem) {
    if (this.healthChecks == null) {
      this.healthChecks = new ArrayList<>();
    }
    this.healthChecks.add(healthChecksItem);
    return this;
  }

  /**
   * Requires \"full\" view. List of health checks performed on the nameservice.
   * @return healthChecks
  **/
  @ApiModelProperty(value = "Requires \"full\" view. List of health checks performed on the nameservice.")

  @Valid

  public List<ApiHealthCheck> getHealthChecks() {
    return healthChecks;
  }

  public void setHealthChecks(List<ApiHealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiNameservice apiNameservice = (ApiNameservice) o;
    return Objects.equals(this.name, apiNameservice.name) &&
        Objects.equals(this.active, apiNameservice.active) &&
        Objects.equals(this.activeFailoverController, apiNameservice.activeFailoverController) &&
        Objects.equals(this.standBy, apiNameservice.standBy) &&
        Objects.equals(this.standByFailoverController, apiNameservice.standByFailoverController) &&
        Objects.equals(this.secondary, apiNameservice.secondary) &&
        Objects.equals(this.mountPoints, apiNameservice.mountPoints) &&
        Objects.equals(this.healthSummary, apiNameservice.healthSummary) &&
        Objects.equals(this.healthChecks, apiNameservice.healthChecks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, active, activeFailoverController, standBy, standByFailoverController, secondary, mountPoints, healthSummary, healthChecks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiNameservice {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    activeFailoverController: ").append(toIndentedString(activeFailoverController)).append("\n");
    sb.append("    standBy: ").append(toIndentedString(standBy)).append("\n");
    sb.append("    standByFailoverController: ").append(toIndentedString(standByFailoverController)).append("\n");
    sb.append("    secondary: ").append(toIndentedString(secondary)).append("\n");
    sb.append("    mountPoints: ").append(toIndentedString(mountPoints)).append("\n");
    sb.append("    healthSummary: ").append(toIndentedString(healthSummary)).append("\n");
    sb.append("    healthChecks: ").append(toIndentedString(healthChecks)).append("\n");
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

