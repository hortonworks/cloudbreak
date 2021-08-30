package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiImpalaQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * The response contains a list of queries and warnings.
 */
@ApiModel(description = "The response contains a list of queries and warnings.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiImpalaQueryResponse   {
  @JsonProperty("queries")
  @Valid
  private List<ApiImpalaQuery> queries = null;

  @JsonProperty("warnings")
  @Valid
  private List<String> warnings = null;

  public ApiImpalaQueryResponse queries(List<ApiImpalaQuery> queries) {
    this.queries = queries;
    return this;
  }

  public ApiImpalaQueryResponse addQueriesItem(ApiImpalaQuery queriesItem) {
    if (this.queries == null) {
      this.queries = new ArrayList<>();
    }
    this.queries.add(queriesItem);
    return this;
  }

  /**
   * The list of queries for this response.
   * @return queries
  **/
  @ApiModelProperty(value = "The list of queries for this response.")

  @Valid

  public List<ApiImpalaQuery> getQueries() {
    return queries;
  }

  public void setQueries(List<ApiImpalaQuery> queries) {
    this.queries = queries;
  }

  public ApiImpalaQueryResponse warnings(List<String> warnings) {
    this.warnings = warnings;
    return this;
  }

  public ApiImpalaQueryResponse addWarningsItem(String warningsItem) {
    if (this.warnings == null) {
      this.warnings = new ArrayList<>();
    }
    this.warnings.add(warningsItem);
    return this;
  }

  /**
   * This list of warnings for this response.
   * @return warnings
  **/
  @ApiModelProperty(value = "This list of warnings for this response.")


  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiImpalaQueryResponse apiImpalaQueryResponse = (ApiImpalaQueryResponse) o;
    return Objects.equals(this.queries, apiImpalaQueryResponse.queries) &&
        Objects.equals(this.warnings, apiImpalaQueryResponse.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(queries, warnings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaQueryResponse {\n");
    
    sb.append("    queries: ").append(toIndentedString(queries)).append("\n");
    sb.append("    warnings: ").append(toIndentedString(warnings)).append("\n");
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

