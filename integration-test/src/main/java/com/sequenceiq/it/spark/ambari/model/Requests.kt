package com.sequenceiq.it.spark.ambari.model

import com.fasterxml.jackson.annotation.JsonProperty

class Requests(var id: Int?,

               @JsonProperty("request_status")
               var requestStatus: String?,

               @JsonProperty("progress_percent")
               var progressPercent: Int?)
