{
  "AlertDefinition": {
    "cluster_name": "${clusterName}",
    "component_name": "RESOURCEMANAGER",
    "description": "This alarm triggers if the pending applications exceed a certain threshold.",
    "enabled": true,
    "ignore_host": false,
    "interval": 5,
    "label": "Pending applications",
    "name": "pending_applications",
    "scope": "HOST",
    "service_name": "YARN",
    "source": {
      "jmx": {
        "property_list": [
          "Hadoop:service=ResourceManager,name=QueueMetrics,q0=root/AppsPending",
          "Hadoop:service=ResourceManager,name=QueueMetrics,q0=root/AppsRunning"
        ],
        "value": "{0}"
      },
      "reporting": {
        "ok": {
          "text": "Running apps: {1}, pending: {0}"
        },
        "warning": {
          "text": "Running apps: {1}, pending: {0}",
          "value": 5
        },
        "critical": {
          "text": "Running apps: {1}, pending: {0}",
          "value": 10
        },
        "units": "d"
      },
      "type": "METRIC",
      "uri": {
        "http": "{{yarn-site/yarn.resourcemanager.webapp.address}}",
        "https": "{{yarn-site/yarn.resourcemanager.webapp.https.address}}",
        "https_property": "{{yarn-site/yarn.http.policy}}",
        "https_property_value": "HTTPS_ONLY",
        "default_port": 0,
        "high_availability": {
          "alias_key": "{{yarn-site/yarn.resourcemanager.ha.rm-ids}}",
          "http_pattern": "{{yarn-site/yarn.resourcemanager.webapp.address.{{alias}}}}",
          "https_pattern": "{{yarn-site/yarn.resourcemanager.webapp.https.address.{{alias}}}}"
        }
      }
    }
  }
}