def rvm_sh(cmd) {
    env.ruby_version = "2.6.1"
    sh '''#!/bin/bash -l
    source ~/.rvm/scripts/rvm
    rvm $ruby_version gemset create ${APPLICATION_NAME}
    rvm $ruby_version@${APPLICATION_NAME} do ''' + cmd
}

pipeline {
    agent any
    environment {
        // Application name, should match git repo
        APPLICATION_NAME = "intellij-example"
        // ENV to deploy
        KSONNIFY_ENV = "production"
        // Branch to deploy (should be master)
        GIT_REPO_URL = "git@github.com:company/${APPLICATION_NAME}.git"
        GIT_CREDENTIALS_NAME = "jenkins-ci-deploy-key"
        ECR_REPO_URL = "1234.dkr.ecr.us-east-1.amazonaws.com"
        ECR_REPO_NAME = "${APPLICATION_NAME}"
        KUBECTL_CONTEXT = "company-prod"
        KUBECONFIG = "${HOME}/kube-prod-config"
        KUBECONFIG_NAME = "kube-prod-config"
        KSONNIFY_GEM_VERSION = "~> 0.18.0"
        PIP_EXTRA_INDEX_URL = credentials('PIP_EXTRA_INDEX_URL')
        KUBE2IAM_ROLE = "kube2iam-production-intellij-example"
    }

    options {
        disableConcurrentBuilds()
    }

    parameters {
        string(defaultValue: 'master', description: 'SHA or branch name to deploy', name: 'GIT_BRANCH')
    }

    stages {
        stage('Check kubectl context') {
            steps {
                sh "${KUBECTL_PATH} config current-context | grep ${KUBECTL_CONTEXT}"
            }
        }
        stage('Git checkout app') {
            steps {
                dir("${APPLICATION_NAME}") {
                    checkout([$class: 'GitSCM', branches: [[name: "${params.GIT_BRANCH}" ]],
                        userRemoteConfigs: [[credentialsId: "${GIT_CREDENTIALS_NAME}", url: "${GIT_REPO_URL}"]]])
                    script {
                        env.gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                        env.gitSHA = gitCommit.take(6)
                        env.imageName = "${ECR_REPO_URL}/${ECR_REPO_NAME}:${gitSHA}"
                        env.configJson = "config-${gitSHA}.json"
                        env.imageNameLogstash = "${ECR_REPO_URL}/${LOGSTASH_ECR_REPO_NAME}:${gitSHA}"
                        env.deployJson = "deploy-${gitSHA}.json"
                        env.migrationJson = "migration-${gitSHA}.json"
                        env.secretsFile = "secrets-${gitSHA}.json"
                    }
                }
            }
        }
        stage('Push to ECR') {
            steps {
                script {
                    docker.withRegistry("https://${ECR_REPO_URL}", "ecr:us-east-1:jenkins-ecr-user") {
                        docker.image("${imageName}").push()
                    }
                }
            }
        }
        stage('Get App Secrets') {
            steps {
                sh """#!/bin/bash
                      export AWS_DEFAULT_REGION=us-east-1
                      virtualenv .venv
                      . .venv/bin/activate
                      pip install pip==9.0.3
                      cd kubernetes/tools/
                      pip install requirements -r requirements.txt
                      python k8s-secrets.py get -a ${APPLICATION_NAME} -n all --json > ${WORKSPACE}/${APPLICATION_NAME}/${secretsFile}
                   """
            }
        }
        stage('Create Config Map') {
            steps {
                dir("${APPLICATION_NAME}") {
                    script {
                        rvm_sh "ksonnify build all deployment/kubernetes/deploy.jsonnet -i ${imageName} -b ${BUILD_TAG} -g ${gitSHA} -e ${KSONNIFY_ENV} --with-secret-json-file ${secretsFile} --only ConfigMap Secret > ${configJson}"
                        sh "cat ${configJson} | ${KUBECTL_PATH} apply -f -"
                    }
                }
            }
        }
        stage('Database Migrations') {
            steps {
                dir("${APPLICATION_NAME}") {
                    script {
                        rvm_sh "ksonnify build release deployment/kubernetes/migrate.jsonnet -i ${imageName} -b ${BUILD_TAG} -g ${gitSHA} -e ${KSONNIFY_ENV} --with-secret-json-file ${secretsFile} > ${migrationJson}"
                        sh "cat ${migrationJson} | ${KUBECTL_PATH} apply -f -"
                        sh "../tools/kubernetes_monitor_job.sh -b ${BUILD_TAG} -k ${KUBECTL_PATH} -r ${APPLICATION_NAME} -s build=${BUILD_TAG}"
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                dir("${APPLICATION_NAME}") {
                    script {
                    }
                }
            }
        }
        stage('Cleanup Workspace') {
            steps {
                cleanWs()
            }
        }
    }
    post {
        always {
            build job: 'deployment-verification', parameters: [string(name: 'APPLICATION_NAME', value: "${APPLICATION_NAME}"),
                                                                   string(name: 'KUBECTL_CONTEXT', value: "${KUBECTL_CONTEXT}"),
                                                                   string(name: 'KUBECONFIG', value: "${KUBECONFIG}")], wait: true
        }
    }
}
