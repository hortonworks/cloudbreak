package com.sequenceiq.cloudbreak.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class AccessToken(@JsonProperty("access_token") val token: String, @JsonProperty("token_type") val tokenType: String, @JsonProperty("expires_in") val expiresIn: Int)
