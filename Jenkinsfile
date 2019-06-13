// Jenkinsfile is triggered when a patchset a submitted
@Library("fabric-ci-lib") _ // global shared library from ci-management repository
// global shared library from ci-management repository
// https://github.com/hyperledger/ci-management/tree/master/vars (Global Shared scripts)
timestamps { // set the timestamps on the jenkins console
  timeout(40) { // Build timeout set to 40 mins
    node ('hyp-x') {
      // trigger jobs on x86_64 builds nodes
      try {
        def JAVA_HOME = "/usr/lib/jvm/java-1.8.0-openjdk-amd64"
        def mvnHome = tool name: 'mvn35'
        def ROOTDIR = pwd() // workspace dir (/w/workspace/<job_name>)
        // Set JAVA_HOME, and special PATH variables
        List javaEnv = [
          "PATH+MVN=${mvnHome}/bin:${JAVA_HOME}/bin",
          "M2_HOME=${mvnHome}"
        ]
        // clean environment and get env data
        stage('Clean Environment') {
          // Delete working directory
          deleteDir()
          // Clean build environment before starting the build
          fabBuildLibrary.cleanupEnv()
          // Display jenkins environment details
          fabBuildLibrary.envOutput()
        }
        stage('Checkout SCM') {
          // Get changes from gerrit
          fabBuildLibrary.cloneRefSpec('fabric-gateway-java')
        }
        withEnv(javaEnv) {
        stage ('Initialize') {
          sh '''
            echo "PATH = ${PATH}"
            echo "M2_HOME = ${M2_HOME}"
            echo "JAVA_HOME = ${JAVA_HOME}"
          '''
        }
        stage ('Maven Tests') {
          try {
            dir("$ROOTDIR/$BASE_DIR") {
              sh 'mvn install'
            }
          } catch (err) {
              failure_stage = "Maven Tests"
              throw err
            }
        }

    // Publish jar files from merge job
    if (env.JOB_TYPE == "merge") {
      publishJar()
      apiDocs()
   } else {
      echo "------> Don't publish jar files from VERIFY job"
   }

        stage ('PostBuild') {
          if (currentBuild.result == 'FAILURE' || currentBuild.result == 'SUCCESS') {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/**/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.log'
          }// Don't fail the build if there is no log file
        }
        } // withEnv
      } finally { // post build actions
          // Send notifications only for failures
          if (env.JOB_TYPE == "merge") {
            if (currentBuild.result == 'FAILURE') {
              // Send notification to rocketChat channel
              // Send merge build failure email notifications to the submitter
              sendNotifications(currentBuild.result, props["CHANNEL_NAME"])
            }
          }
          // Delete containers
          fabBuildLibrary.deleteContainers()
          // Delete unused docker images
          fabBuildLibrary.deleteUnusedImages()
          // Delete workspace when build is done
          cleanWs notFailBuild: true
        } // end finally block
    } // end node block
  } // end timeout block
} // end timestamps

def publishJar() {
// Publish jar artifacts after successful merge
  stage("Publish Jar") {
    sh 'echo "-------> Publish Jar files"'
    def ROOTDIR = pwd()
    configFileProvider([
      configFile(fileId: 'fabric-gateway-java-settings', variable: 'SETTINGS_FILE'),
      configFile(fileId: 'global-settings', variable: 'GLOBAL_SETTINGS_FILE')]) {

    try {
      dir("${ROOTDIR}/$PROJECT_DIR/scripts/ci_scripts") {
        sh './ciScript.sh --publishJavaArtifacts'
      }
    }
    catch (err) {
      failure_stage = "Publish Jar Nexus"
      currentBuild.result = 'FAILURE'
      throw err
    }
    }
  }
}

def apiDocs() {
// Publish Java API docs after successful merge
  stage("Publish JavaApiDocs") {
    sh 'echo "--------> Publish JavaApiDocs"'
    def ROOTDIR = pwd()
    withCredentials([[$class: 'UsernamePasswordMultiBinding',
      credentialsId: 'fabric-gateway-java-gh',
      usernameVariable: 'GATEWAY_JAVA_GH_USERNAME',
      passwordVariable: 'GATEWAY_JAVA_GH_PASSWORD']]) {
    try {
      dir("${ROOTDIR}/$PROJECT_DIR/scripts/ci_scripts") {
        sh './ciScript.sh --publishJavaApiDocs'
      }
    }
    catch (err) {
      failure_stage = "Publish JavaApiDocs"
      currentBuild.result = 'FAILURE'
      throw err
    }
    }
  }
}
