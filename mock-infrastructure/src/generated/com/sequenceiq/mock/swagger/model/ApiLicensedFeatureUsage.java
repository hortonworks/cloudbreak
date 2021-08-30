package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Information about the number of nodes using which product features. &lt;p&gt; Usage information is provided for individual clusters, as well as totals across all clusters.
 */
@ApiModel(description = "Information about the number of nodes using which product features. <p> Usage information is provided for individual clusters, as well as totals across all clusters.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiLicensedFeatureUsage   {
  @JsonProperty("totals")
  @Valid
  private Map<String, Integer> totals = null;

  @JsonProperty("clusters")
  @Valid
  private Map<String, Object> clusters = null;

  public ApiLicensedFeatureUsage totals(Map<String, Integer> totals) {
    this.totals = totals;
    return this;
  }

  public ApiLicensedFeatureUsage putTotalsItem(String key, Integer totalsItem) {
    if (this.totals == null) {
      this.totals = new HashMap<>();
    }
    this.totals.put(key, totalsItem);
    return this;
  }

  /**
   * Map from named features to the total number of nodes using those features.
   * @return totals
  **/
  @ApiModelProperty(value = "Map from named features to the total number of nodes using those features.")


  public Map<String, Integer> getTotals() {
    return totals;
  }

  public void setTotals(Map<String, Integer> totals) {
    this.totals = totals;
  }

  public ApiLicensedFeatureUsage clusters(Map<String, Object> clusters) {
    this.clusters = clusters;
    return this;
  }

  public ApiLicensedFeatureUsage putClustersItem(String key, Object clustersItem) {
    if (this.clusters == null) {
      this.clusters = new HashMap<>();
    }
    this.clusters.put(key, clustersItem);
    return this;
  }

  /**
   * Map from clusters to maps of named features to the number of nodes in the cluster using those features.
   * @return clusters
  **/
  @ApiModelProperty(value = "Map from clusters to maps of named features to the number of nodes in the cluster using those features.")


  public Map<String, Object> getClusters() {
    return clusters;
  }

  public void setClusters(Map<String, Object> clusters) {
    this.clusters = clusters;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiLicensedFeatureUsage apiLicensedFeatureUsage = (ApiLicensedFeatureUsage) o;
    return Objects.equals(this.totals, apiLicensedFeatureUsage.totals) &&
        Objects.equals(this.clusters, apiLicensedFeatureUsage.clusters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totals, clusters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiLicensedFeatureUsage {\n");
    
    sb.append("    totals: ").append(toIndentedString(totals)).append("\n");
    sb.append("    clusters: ").append(toIndentedString(clusters)).append("\n");
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

