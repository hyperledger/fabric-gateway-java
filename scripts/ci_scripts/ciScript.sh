#!/bin/bash -e
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#

Parse_Arguments() {
  while [ $# -gt 0 ]; do
    case $1 in
      --publishJavaArtifacts)
          publishJavaArtifacts
          ;;
      --publishJavaApiDocs)
          publishJavaApiDocs
          ;;
      *)
          echo "Wrong function called"
          exit 1
          ;;
    esac
    shift
  done
}

# Publish java artifacts after successful merge on amd64
publishJavaArtifacts() {
        echo
        echo -e "\033[32m -----------> Publish npm modules from amd64" "\033[0m"
        ./publishJavaArtifacts.sh
}

# Publish Javadocs after successful merge on amd64
publishJavaApiDocs() {
        echo
        echo -e "\033[32m -----------> Publish Javadocs after successful merge on amd64" "\033[0m"
        ./publishJavaApiDocs.sh
}
Parse_Arguments $@
