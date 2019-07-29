@Library('jenkins-pipeline') _
import com.figure.Common

def common
pipeline {
    agent any

    stages {
        stage('Stage Checkout') {
            steps {
                script {
                    common = new Common(this)
                }
                gitCheckout()
            }
        }
        stage('Gradle Build') {
            steps {
               gradleCleanBuild("${common.fixedBranchName()}-${env.BUILD_NUMBER}")
            }
        }
        stage('Gradle Publish') {
          steps {
             script {
                gradlePublish("${common.fixedBranchName()}-${env.BUILD_NUMBER}")
             }
          }
        }
        stage('Git Tag') {
            steps {
                script {
                    if (env.BRANCH_NAME == "master") {
                        gitTag(this, env.BUILD_NUMBER, env.GIT_COMMIT, env.GIT_URL)
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                rmrf('build')
            }
        }
    }
}
