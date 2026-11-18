
pipeline {
    agent any

    environment {
        IMAGE_NAME = 'springboot-app'
        DEPLOY_HOST = '15.206.147.184'
        DEPLOY_USER = 'ubuntu'
        CONTAINER_NAME = 'springboot-app'
        APP_PORT = '8085'
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/saiabbhiram/springboot-demo.git'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Docker Build and Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        set -e

                        IMAGE="$DOCKER_USER/$IMAGE_NAME:$BUILD_NUMBER"

                        echo "$DOCKER_PASS" | docker login \
                            -u "$DOCKER_USER" --password-stdin

                        docker build -t "$IMAGE" .
                        docker push "$IMAGE"

                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sshagent(credentials: ['deploy-ssh-key']) {
                        sh '''
                            set -e

                            IMAGE="$DOCKER_USER/$IMAGE_NAME:$BUILD_NUMBER"

                            ssh -o StrictHostKeyChecking=accept-new \
                                "$DEPLOY_USER@$DEPLOY_HOST" "
                                set -e

                                echo '$DOCKER_PASS' | docker login \
                                    -u '$DOCKER_USER' --password-stdin

                                docker pull '$IMAGE'

                                docker stop '$CONTAINER_NAME' || true
                                docker rm '$CONTAINER_NAME' || true

                                docker run -d \
                                    --name '$CONTAINER_NAME' \
                                    --restart unless-stopped \
                                    -p '$APP_PORT:8085' \
                                    '$IMAGE'

                                docker logout
                            "
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Application deployed successfully!'
        }

        failure {
            echo 'Deployment failed. Check Jenkins console logs.'
        }
    }
}
