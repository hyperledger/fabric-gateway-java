#!/bin/bash
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#

set -e -o pipefail

# Input environment variables:
: "${GITHUB_USER:?}" # The GitHub user name for publishing
: "${GITHUB_EMAIL:?}" # Email address of the GitHub user
: "${SOURCE_DIR:?}" # Directory where the project source is located
: "${PUBLISH_DIR:?}" # Directory used to store content to publish to GitHub Pages
: "${SOURCE_BRANCH:?}" # Source code branch name
: "${JAVADOC_DIR:?}" # Directory where built JavaDoc is located

COMMIT_HASH=$(git rev-parse HEAD)
readonly COMMIT_HASH
readonly PUBLISH_RELEASE_DIR="${PUBLISH_DIR}/${SOURCE_BRANCH}"

cleanPublishDir() {
    echo "Removing ${PUBLISH_RELEASE_DIR}"
    rm -rf "${PUBLISH_RELEASE_DIR}"

    if [[ ${SOURCE_BRANCH} = main ]]; then
        removePublishRootFiles
    fi
}

removePublishRootFiles() {
    find "${PUBLISH_DIR}" -type f -maxdepth 1 -mindepth 1 \
        -exec echo Removing {} \; \
        -exec rm -f {} \;
}

copyToPublish() {
    echo "Copying built documentation from ${JAVADOC_DIR} to ${PUBLISH_RELEASE_DIR}"
    rsync -r "${JAVADOC_DIR}/" "${PUBLISH_RELEASE_DIR}"

    if [[ ${SOURCE_BRANCH} = main ]]; then
        echo 'Copying root directory markup files'
        cp -p "${SOURCE_DIR}"/*.md "${PUBLISH_DIR}"
    fi
}

publishDocs() {
    echo 'Publishing documentation'
    (cd "${PUBLISH_DIR}" && _stagingPushDocs)
}

_stagingPushDocs() {
    if [ -z "$(git status --porcelain=v1 2>/dev/null)" ]; then
        echo 'No changes to publish'
        return
    fi

    git config --local user.name "${GITHUB_USER}"
    git config --local user.email "${GITHUB_EMAIL}"
    git add .
    git commit -m "Commit ${COMMIT_HASH}"
    git push
}

cleanPublishDir
copyToPublish
publishDocs
