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

# Publish java artifacts after successful merge
publishJavaArtifacts() {
        echo
        echo  "-----------> Publish gateway jar artifacts after successful merge"
        ./publishJavaArtifacts.sh
}

# Publish javadocs after successful merge
publishJavaApiDocs() {
        echo
        echo  "-----------> Publish Javadocs after successful merge"
        ./publishJavaApiDocs.sh
}
Parse_Arguments $@
