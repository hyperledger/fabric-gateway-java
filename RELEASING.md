# Releasing

Artifacts are published to the following locations as a result of releasing the Hyperledger Fabric Gateway SDK for Java:

- [Maven Central](https://central.sonatype.com/artifact/org.hyperledger.fabric/fabric-gateway-java).
- [GitHub Packages](https://github.com/hyperledger/fabric-gateway-java/packages/1782386).

## Before releasing

Check that the last [push workflow](https://github.com/hyperledger/fabric-gateway-java/actions/workflows/push.yml) completed all checks successfully, since this is the version that will be published.

## Create release

Creating a GitHub release on the [releases page](https://github.com/hyperledger/fabric-gateway-java/releases) will trigger the build to publish the new release.

When drafting the release, create a new tag for the new version (with a `v` prefix), e.g. `vX.Y.Z`

See previous releases for examples of the title and description.

## After releasing

The following tasks are required after releasing:

- Update version number in `pom.xml` to the next patch level.
