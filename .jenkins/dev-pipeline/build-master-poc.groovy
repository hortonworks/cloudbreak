pipeline {
    agent any

    environment {
        AWS_S3_ACCESS_KEY_ID = credentials('aws-s3-access-key')
        AWS_S3_SECRET_ACCESS_KEY = credentials('aws-s3-secret-key')
    }

    stages {
        stage('Build jenkinsfile branch') {
            steps {
                sh """
                    export AWS_ACCESS_KEY_ID=$AWS_S3_ACCESS_KEY_ID
                    export AWS_ACCESS_KEY=$AWS_S3_ACCESS_KEY_ID
                    export AWS_SECRET_ACCESS_KEY=$AWS_S3_SECRET_ACCESS_KEY
                    export JAVA_HOME=/usr/lib/jvm/jdk-10.0.1
                    
                    git fetch --all
                    git checkout jenkinsfile
                    make build-dev-pipeline-poc
                """
            }
        }
    }
}