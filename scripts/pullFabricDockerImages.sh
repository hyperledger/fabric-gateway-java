#!/bin/bash

set -euo pipefail

# Pull images from Hyperledger Artifactory and re-tag to latest
for IMAGE in peer orderer ca baseos ccenv javaenv nodeenv tools; do
    docker pull "hyperledger-fabric.jfrog.io/fabric-${IMAGE}:amd64-2.1-stable"
    docker tag "hyperledger-fabric.jfrog.io/fabric-${IMAGE}:amd64-2.1-stable" "hyperledger/fabric-${IMAGE}:amd64-latest"
    docker tag "hyperledger-fabric.jfrog.io/fabric-${IMAGE}:amd64-2.1-stable" "hyperledger/fabric-${IMAGE}"
done
