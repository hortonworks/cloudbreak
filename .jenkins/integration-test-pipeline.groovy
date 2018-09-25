pipeline {
    agent any

    parameters {
        string(name: 'TARGET_CBD_VERSION', defaultValue: '', description: 'Cloudbreak version to run integration tests against. e.g. 1.5.0-dev.87.')

        string(name: 'TYPE', defaultValue: 'none', description: '')

        booleanParam(name: 'UPLOAD_SWAGGER_JSON_TO_S3', defaultValue: false, description: '')

        booleanParam(name: 'DOWNLOAD_JAR_FROM_S3', defaultValue: false, description: '')
    }

    environment {
        AWS_ACCESS_KEY_ID = credentials('aws-s3-access-key')
        AWS_SECRET_ACCESS_KEY = credentials('aws-s3-secret-key')
    }

    stages {
        stage('Integration test') {
            steps {
                script {
                    currentBuild.displayName = "#${BUILD_NUMBER}-${params.TARGET_CBD_VERSION}"
                }

                sh """
                    export VERSION=$TARGET_CBD_VERSION
                    git checkout refs/tags/${TARGET_CBD_VERSION}

                    if [ -d "/var/lib/boot2docker/" ]; then
                        sudo chown -R \$(whoami) /var/lib/boot2docker/
                    fi
                    cd integration-test
                    make download-cbd
                    scripts/fill_public_ip.sh
                    make cbd-delete
                    if [ "$DOWNLOAD_JAR_FROM_S3" = true ]; then
                        make download-jar-from-s3
                    else
                        make docker-build
                    fi
                    make runtest
                    if [ "$UPLOAD_SWAGGER_JSON_TO_S3" = true ]; then
                        make upload-s3
                    fi
                """
            }
        }
    }
}