#/bin/bash

set -e -o pipefail

# Pull images from Hyperledger Nexus and re-tag to latest
for IMAGE in peer orderer ca baseos ccenv javaenv nodeenv tools; do
    docker pull "nexus3.hyperledger.org:10001/hyperledger/fabric-${IMAGE}:amd64-2.0.0-stable"
    docker tag "nexus3.hyperledger.org:10001/hyperledger/fabric-${IMAGE}:amd64-2.0.0-stable" "hyperledger/fabric-${IMAGE}:amd64-latest"
    docker tag "nexus3.hyperledger.org:10001/hyperledger/fabric-${IMAGE}:amd64-2.0.0-stable" "hyperledger/fabric-${IMAGE}"
done
