version = null

def loadProperties() {
    node {
        checkout scm
        propFile = readProperties file: "${WORKSPACE}/../../cloudbreak-build-master/workspace/version"
        version = propFile.version
        echo propFile
        echo propFile.version
    }
}

pipeline {
    agent any

    stages {
        stage('Build master') {
            steps {
                // TODO: replace with pipeline job
                build job: 'cloudbreak-build-master', propagate: true, wait: true
            }
        }
        stage('Docker containers') {
            parallel {
                stage('Dockerhub tag Cloudbreak') {
                    // TODO: replace with pipeline job
                    steps {
                        build job: 'dockerhub-tag-cloudbreak',
                                parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                                propagate: true, wait: true
                    }
                }
                stage('Dockerhub tag autoscale') {
                    // TODO: replace with pipeline job
                    steps {
                        build job: 'dockerhub-tag-autoscale',
                                parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                                propagate: true, wait: true
                    }
                }
            }
        }
        stage('Run integration test and build Containers') {
            parallel {
                stage('Integration test') {
                    // TODO: replace with pipeline job
                    steps {
                        build job: 'integration-test',
                                parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                                propagate: true, wait: true
                    }
                }
                stage('Dockerhub tag hortonworks-cloud') {
                    // TODO: replace with pipeline job
                    steps {
                        build job: 'dockerhub-tag-hortonworks-cloud',
                                parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                                propagate: true, wait: true
                    }
                }
            }
        }
        stage('Add CB version to image catalog') {
            steps {
                // TODO: replace with pipeline job
                build job: 'imagecatalog-v2-cb-append',
                        parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                        propagate: true, wait: true
            }
        }
        stage('CB CLI artifacts') {
            steps {
                // TODO: replace with pipeline job
                build job: 'cb-cli-release',
                        parameters: [[$class: 'StringParameterValue', name: 'VERSION', value: version]],
                        propagate: true, wait: true
            }
        }
    }
}