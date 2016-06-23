package com.sequenceiq.periscope.doc

object ApiDescription {

    val JSON = "application/json"

    val ALERT_DESCRIPTION = "Operations on alerts"
    val CONFIGURATION_DESCRIPTION = "Operations on configurations"
    val HISTORY_DESCRIPTION = "Operations on hirtories"
    val CLUSTERS_DESCRIPTION = "Operations on clusters"
    val POLICIES_DESCRIPTION = "Operations on policies"

    object AlertOpDescription {
        val METRIC_BASED_POST = "create alert which metric based"
        val METRIC_BASED_PUT = "modify alert which metric based"
        val METRIC_BASED_GET = "retrieve alert which metric based"
        val METRIC_BASED_DELETE = "delete alert which metric based"
        val METRIC_BASED_DEFINITIONS = "retrieve alert definitions"

        val TIME_BASED_POST = "create alert which time based"
        val TIME_BASED_PUT = "modify alert which time based"
        val TIME_BASED_GET = "retrieve alert which time based"
        val TIME_BASED_DELETE = "delete alert which time based"
    }

    object AlertNotes {
        val TIME_BASED_NOTES = "Auto-scaling supports two Alert types: metric and time based. " + "Time based alerts are based on cron expressions and allow alerts to be triggered based on time."
        val METRIC_BASED_NOTES = "Auto-scaling supports two Alert types: metric and time based. "
        + "Metric based alerts are using the default (or custom) Ambari metrics. These metrics have a default Threshold value configured in Ambari - "
        + "nevertheless these thresholds can be configured, changed or altered in Ambari. In order to change the default threshold for a metric "
        + "please go to Ambari UI and select the Alerts tab and the metric. The values can be changed in the Threshold section. "
    }


    object ConfigurationOpDescription {
        val CONFIGURATION_POST = "create configuration"
        val CONFIGURATION_GET = "retrieve configuration"
    }

    object ConfigurationNotes {
        val NOTES = "An SLA scaling policy can contain multiple alerts. When an alert is triggered a scaling adjustment is applied,"
        + " however to keep the cluster size within boundaries a cluster size min. and cluster size max. is attached to the cluster - "
        + "thus a scaling policy can never over or undersize a cluster. Also in order to avoid stressing the cluster we have introduced a "
        + "cooldown time period (minutes) - though an alert is raised and there is an associated scaling policy, the system will not apply "
        + "the policy within the configured timeframe. In an SLA scaling policy the triggered rules are applied in order."
    }

    object HistoryOpDescription {
        val HISTORY_GET_ALL = "retrieve full history"
        val HISTORY_GET = "retrieve a specific history"
    }

    object HistoryNotes {
        val NOTES = "Get Auto-scaling history on a specific cluster"
    }

    object PolicyOpDescription {
        val POLICY_POST = "create policy"
        val POLICY_PUT = "modify policy"
        val POLICY_GET = "retrieve policy"
        val POLICY_DELETE = "delete policy"
    }

    object PolicyNotes {
        val NOTES = "Scaling is the ability to increase or decrease the capacity of the Hadoop cluster or application "
        + "based on an alert. When scaling policies are used, the capacity is automatically increased or decreased according to the "
        + "conditions defined. Cloudbreak will do the heavy lifting and based on the alerts and the scaling policy linked to them it "
        + "executes the associated policy. We scaling granularity is at the hostgroup level - thus you have the option to scale services "
        + "or components only, not the whole cluster."
    }

    object ClusterOpDescription {
        val CLUSTER_POST = "create cluster"
        val CLUSTER_PUT = "modify cluster"
        val CLUSTER_GET = "retrieve cluster"
        val CLUSTER_GET_ALL = "retrieve all cluster"
        val CLUSTER_DELETE = "delete cluster"
        val CLUSTER_SET_STATE = "set cluster state"
    }

    object ClusterNotes {
        val NOTES = "Ambari cluster."
    }

    object AmbariJsonProperties {
        val HOST = "Ambari server host address"
        val PORT = "Ambari server port"
        val USERNAME = "Ambari server username"
        val PASSWORD = "Ambari server password"
    }

    object ScalingConfigurationJsonProperties {
        val MINSIZE = "The minimum size of the cluster after scaling"
        val MAXSIZE = "The maximum size of the cluster after scaling"
        val COOLDOWN = "The time between two scaling activities"
    }

    object ScalingPolicyJsonProperties {
        val ID = "Id of the policy"
        val NAME = "Name of the policy"
        val ADJUSTMENTTYPE = "Type of the scaling count"
        val SCALINGADJUSTMENT = "Count of the scaling"
        val ALERTID = "Id of the alert which trigger the scaling"
        val HOSTGROUP = "Name of hostgroup which affected by the scaling"
    }

    object StateJsonProperties {
        val STATE = "State of the cluster"
    }

    object TimeAlertJsonProperties {
        val TIMEZONE = "Timezone of the time alert"
        val CRON = "Cron expression of the time alert"
    }

    object MetricAlertJsonProperties {
        val ALERTDEFINITION = "Definition of the alert"
        val PERIOD = "Period of the alert"
        val ALERTSTATE = "State of the alert"
    }

    object BaseAlertJsonProperties {
        val ID = "Id of the alert"
        val ALERTNAME = "Name of the alert"
        val DESCRIPTION = "Description of the alert"
        val SCALINGPOLICYID = "Id of the scaling ploicy"
    }

    object ClusterJsonProperties {
        val ID = "Id of the cluster"
        val HOST = "Host address of the ambari server"
        val PORT = "Port of the Ambari server"
        val STATE = "State of the cluster"
        val STACKID = "Id of the stack in cloudbreak"
    }

    object HistoryJsonProperties {
        val ID = "Id of the history object"
        val CLUSTERID = "If of the cluster"
        val CBSTACKID = "Id of the cloudbreak stack"
        val ORIGINALNODECOUNT = "The node count before of the scaling"
        val ADJUSTMENT = "Count of scaling"
        val ADJUSTMENTTYPE = "Type of adjustment"
        val SCALINGSTATUS = "Status of scaling activity"
        val STATUSREASON = "Reason of the status"
        val TIMESTAMP = "Time of the creation"
        val HOSTGROUP = "The affected hostgroup name"
        val ALERTTYPE = "Type of the alert event"
        val PROPERTIES = "Additional properties"
    }
}
