environments {
  jenkins {
    sonar_host_url = "http://$System.env.SONAR_HOST:9000"
    metadata = "https://s3-eu-west-1.amazonaws.com/maven.sequenceiq.com/releases/com/sequenceiq/cloudbreak/maven-metadata.xml"
    sonar_host_user = "$System.env.SONAR_USER"
    sonar_host_password = "$System.env.SONAR_PASSWORD"
  }

  local {
    sonar_host_url = "http://localhost:9000"
    metadata = "https://s3-eu-west-1.amazonaws.com/maven.sequenceiq.com/releases/com/sequenceiq/cloudbreak/maven-metadata.xml"
    sonar_host_user = "$System.env.SONAR_USER"
    sonar_host_password = "$System.env.SONAR_PASSWORD"
  }
}