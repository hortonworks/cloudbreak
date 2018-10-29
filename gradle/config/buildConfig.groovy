environments {
  jenkins {
    metadata = "$System.env.NEXUS_URL/com/sequenceiq/cloudbreak/maven-metadata.xml"
    sonar_host_url = "http://$System.env.SONAR_HOST:9000"
    sonar_host_user = "$System.env.SONAR_USER"
    sonar_host_password = "$System.env.SONAR_PASSWORD"
  }

  local {
    metadata = "$System.env.NEXUS_URL/com/sequenceiq/cloudbreak/maven-metadata.xml"
    sonar_host_url = "http://localhost:9000"
    sonar_host_user = "$System.env.SONAR_USER"
    sonar_host_password = "$System.env.SONAR_PASSWORD"
  }
}