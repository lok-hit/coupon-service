// =============================================================================
// Coupon Service — Jenkins Pipeline
// Target: Oracle Cloud Free Tier VM (Jenkins running in Docker)
//
// Required Jenkins credentials:
//   ghcr-token        — Secret text: GitHub PAT with packages:write
//   oracle-vm-ssh     — SSH Username with private key (user: ubuntu or opc)
//
// Required Jenkins environment variable (configure in Manage Jenkins → System):
//   ORACLE_VM_HOST    — Public IP or hostname of the Oracle Cloud VM
//   GITHUB_REPO       — e.g. your-org/coupon-service (lowercase)
// =============================================================================

pipeline {
    agent any

    environment {
        REGISTRY   = 'ghcr.io'
        IMAGE_BASE = "${REGISTRY}/${env.GITHUB_REPO ?: 'your-org/coupon-service'}"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        // Prevent concurrent runs from stepping on each other on the VM
        disableConcurrentBuilds(abortPrevious: false)
    }

    stages {
        // ─────────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "sha-${env.GIT_SHORT}"
                }
                echo "Building image tag: ${env.IMAGE_TAG}"
            }
        }

        // ─────────────────────────────────────────────────────────
        stage('Build JAR') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew bootJar --no-daemon -x test'
            }
        }

        // ─────────────────────────────────────────────────────────
        stage('Test') {
            steps {
                // Docker socket is mounted into the Jenkins container (see jenkins/docker-compose.yml)
                // so Testcontainers can spin up PostgreSQL and Redis
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'build/test-results/test/*.xml'

                    publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'build/reports/tests/test',
                        reportFiles          : 'index.html',
                        reportName           : 'Test Report'
                    ])
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        stage('Docker Build & Push') {
            when {
                branch 'main'
            }
            steps {
                script {
                    withCredentials([string(credentialsId: 'ghcr-token', variable: 'GHCR_TOKEN')]) {
                        sh """
                            echo "\$GHCR_TOKEN" | docker login ${REGISTRY} -u jenkins --password-stdin

                            # Build multi-platform if buildx is available, otherwise single-platform
                            if docker buildx version > /dev/null 2>&1; then
                                docker buildx build \
                                    --platform linux/amd64,linux/arm64 \
                                    --push \
                                    -t ${IMAGE_BASE}:${env.IMAGE_TAG} \
                                    -t ${IMAGE_BASE}:latest \
                                    .
                            else
                                docker build \
                                    -t ${IMAGE_BASE}:${env.IMAGE_TAG} \
                                    -t ${IMAGE_BASE}:latest \
                                    .
                                docker push ${IMAGE_BASE}:${env.IMAGE_TAG}
                                docker push ${IMAGE_BASE}:latest
                            fi
                        """
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId  : 'oracle-vm-ssh',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    ),
                    string(credentialsId: 'ghcr-token', variable: 'GHCR_TOKEN')
                ]) {
                    sh """
                        ssh -i "\$SSH_KEY" \
                            -o StrictHostKeyChecking=no \
                            -o ConnectTimeout=15 \
                            "\$SSH_USER@${env.ORACLE_VM_HOST}" << 'REMOTE'
                                set -euo pipefail
                                cd /opt/coupon-service

                                echo "${env.GHCR_TOKEN}" | docker login ghcr.io -u jenkins --password-stdin

                                IMAGE_TAG=${env.IMAGE_TAG} \
                                GITHUB_REPOSITORY=${env.GITHUB_REPO} \
                                  docker compose -f docker-compose.prod.yml pull app

                                IMAGE_TAG=${env.IMAGE_TAG} \
                                GITHUB_REPOSITORY=${env.GITHUB_REPO} \
                                  docker compose -f docker-compose.prod.yml up -d --no-build

                                docker image prune -f
REMOTE
                    """
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        stage('Health Check') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId  : 'oracle-vm-ssh',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    sh """
                        ssh -i "\$SSH_KEY" \
                            -o StrictHostKeyChecking=no \
                            "\$SSH_USER@${env.ORACLE_VM_HOST}" << 'REMOTE'
                                for i in \$(seq 1 12); do
                                    STATUS=\$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || true)
                                    if [ "\$STATUS" = "200" ]; then
                                        echo "Health check passed (attempt \$i)"
                                        exit 0
                                    fi
                                    echo "Attempt \$i: HTTP \$STATUS — waiting 5s..."
                                    sleep 5
                                done
                                echo "Health check failed after 60s" && exit 1
REMOTE
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline succeeded — deployed ${env.IMAGE_TAG ?: 'N/A'}"
        }
        failure {
            echo "Pipeline FAILED — check stage logs above"
        }
        cleanup {
            // Remove dangling images from the Jenkins host
            sh 'docker image prune -f || true'
        }
    }
}
