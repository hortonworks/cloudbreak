package com.sequenceiq.it.spark.ambari.model

import com.fasterxml.jackson.annotation.JsonProperty

class Clusters(

        @JsonProperty("cluster_name")
        var clusterName: String?)
