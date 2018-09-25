properties = null
version = null

def loadProperties() {
    node {
        checkout scm
        properties = readProperties file: "${WORKSPACE}/../../cloudbreak-build-master/workspace/version"
        version = properties.VERSION
    }
}

pipeline {
    agent any

    stages {
        stage('Build master') {
            steps {
                build job: 'cloudbreak-build-master-pipeline', propagate: true, wait: true
            }
        }
        stage('Docker containers') {
            parallel {
                stage('Dockerhub tag Cloudbreak') {
                    steps {
                        build job: 'dockerhub-tag-cloudbreak-pipeline',
                                parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                                propagate: true, wait: true
                    }
                }
                stage('Dockerhub tag autoscale') {
                    steps {
                        build job: 'dockerhub-tag-autoscale-pipeline',
                                parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                                propagate: true, wait: true
                    }
                }
                stage('Dockerhub tag hortonworks-cloud') {
                    steps {
                        build job: 'dockerhub-tag-hortonworks-cloud-pipeline',
                                parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                                propagate: true, wait: true
                    }
                }
            }
        }
        stage('Integration test') {
            steps {
                build job: 'cloudbreak-integration-test-pipeline',
                        parameters: [
                                [$class: 'BooleanParameterValue', name: 'UPLOAD_SWAGGER_JSON_TO_S3', value: true],
                                [$class: 'BooleanParameterValue', name: 'DOWNLOAD_JAR_FROM_S3', value: true],
                                [$class: 'StringParameterValue', name: 'TARGET_CBD_VERSION', value: version]
                        ],
                        propagate: true, wait: true
            }
        }
        stage('Add CB version to image catalog') {
            steps {
                build job: 'imagecatalog-v2-cb-append-pipeline',
                        parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                        propagate: true, wait: true
            }
        }
        stage('CB CLI artifacts') {
            steps {
                build job: 'cb-cli-release-pipeline',
                        parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                        propagate: true, wait: true
            }
        }
    }
}