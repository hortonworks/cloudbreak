properties = null
version = null

def loadProperties() {
    node {
        checkout scm
        properties = readProperties file: "${WORKSPACE}/../../cloudbreak-temp-pipeline-file-job-1/workspace/version"
        version = properties.VERSION
    }
}


pipeline {
    agent any

    environment {
        BRANCH = 'jenkinsfile'
    }

    stages {
        stage('create test file') {
            steps {
                sh '''
                    pwd
                    VERSION='verzio-17.8.1-pipeline-test'
                    echo VERSION=$VERSION > $WORKSPACE/version
                    ls -l
                    cat version
                '''
            }
        }
        stage('read version from file into properties') {
            steps {
                script {
                    loadProperties()
                    print properties
                    print properties.VERSION
                    print version
                }
            }
        }
        stage('invoke other job with parameter') {
            steps {
                build job: 'cloudbreak-temp-pipeline-file-job-2', parameters: [
                        [$class: 'StringParameterValue', name: 'VERSION', value: version],
                        [$class: 'StringParameterValue', name: 'BRANCH', value: 'jenkinsfile']
                ]
            }
        }
    }
}