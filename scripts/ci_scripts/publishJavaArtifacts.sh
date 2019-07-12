#!/bin/bash -e
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#
# Get the version from pom.xml
cd "${WORKSPACE}/${BASE_DIR}"
version=$(mvn -q \
    -Dexec.executable=echo \
    -Dexec.args='${project.version}' \
    --non-recursive \
    exec:exec)

if [[ ${version} =~ SNAPSHOT ]]; then
    echo "Pushing fabric-gateway-java-${version}.jar to Nexus snapshots"
    mvn org.apache.maven.plugins:maven-deploy-plugin:deploy-file \
      -DupdateReleaseInfo=true \
      -Dfile="${WORKSPACE}/${BASE_DIR}/target/fabric-gateway-java-${version}.jar" \
      -Djavadoc="${WORKSPACE}/${BASE_DIR}/target/fabric-gateway-java-${version}-javadoc.jar" \
      -DpomFile="${WORKSPACE}/${BASE_DIR}/pom.xml" \
      -DrepositoryId=hyperledger-snapshots \
      -Durl=https://nexus.hyperledger.org/content/repositories/snapshots/ \
      -DuniqueVersion=false \
      -gs "${GLOBAL_SETTINGS_FILE}" -s "${SETTINGS_FILE}"
      echo '========> DONE <======='
else
    echo "Pushing fabric-gateway-java-${version}.jar to Nexus releases"
    mvn org.apache.maven.plugins:maven-deploy-plugin:deploy-file \
      -DupdateReleaseInfo=true \
      -Dfile="${WORKSPACE}/${BASE_DIR}/target/fabric-gateway-java-${version}.jar" \
      -Djavadoc="${WORKSPACE}/${BASE_DIR}/target/fabric-gateway-java-${version}-javadoc.jar" \
      -DpomFile="${WORKSPACE}/${BASE_DIR}/pom.xml" \
      -DrepositoryId=hyperledger-releases \
      -Durl=https://nexus.hyperledger.org/content/repositories/releases/ \
      -DuniqueVersion=false \
      -gs "${GLOBAL_SETTINGS_FILE}" -s "${SETTINGS_FILE}"
      echo '========> DONE <======='
fi
