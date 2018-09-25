pipeline {
    agent any

    parameters {
        string(name: 'VERSION', defaultValue: '', description: 'Cloudbreak version to build the Docker containers from. Example: 1.5.0-dev.87.')
        string(name: 'BRANCH', defaultValue: 'jenkinsfile', description: '')
        booleanParam(name: 'UPLOAD_SWAGGER_JSON_TO_S3', defaultValue: true, description: 'Beautiful boolean value')
    }

    environment {
        AWS_S3_ACCESS_KEY_ID = credentials('aws-s3-access-key')
        AWS_S3_SECRET_ACCESS_KEY = credentials('aws-s3-secret-key')
    }

    stages {
        stage('Using parameter to do something') {
            steps {
                sh "echo 'The received version is: '"
                sh "echo '${params.VERSION}'"

                sh """
                    git fetch --all
                    git checkout ${params.BRANCH}
                    
                    git branch                    

                    echo 'running complex things.'
                    echo 'value of params.VERSION: ${params.VERSION}'
                    echo 'value of VERSION: ${VERSION}'
                    echo 'value of params.UPLOAD_SWAGGER_JSON_TO_S3: ${params.UPLOAD_SWAGGER_JSON_TO_S3}'
                    echo 'value of UPLOAD_SWAGGER_JSON_TO_S3: ${UPLOAD_SWAGGER_JSON_TO_S3}'
                    echo 'value of AWS_S3_ACCESS_KEY_ID: $AWS_S3_ACCESS_KEY_ID'

                    if [ "$UPLOAD_SWAGGER_JSON_TO_S3" = true ]; then
                        echo 'if-branch is true'
                    else
                        echo 'if-branch is false'
                    fi
                """
            }
        }
    }
}