# Hyperledger Fabric Gateway SDK for Java

<div style="float: right">
<table align="right">
  <tr><th>Branch</th><th>Build status</th></tr>
  <tr><td>main</td><td><a href="https://dev.azure.com/Hyperledger/Fabric-Gateway-Java/_build/latest?definitionId=38&branchName=main"><img src="https://dev.azure.com/Hyperledger/Fabric-Gateway-Java/_apis/build/status/Fabric-Gateway-Java?branchName=main"></a></td></tr>
  <tr><td>release-2.2</td><td><a href="https://dev.azure.com/Hyperledger/Fabric-Gateway-Java/_build/latest?definitionId=38&branchName=release-2.2"><img src="https://dev.azure.com/Hyperledger/Fabric-Gateway-Java/_apis/build/status/Fabric-Gateway-Java?branchName=release-2.2"></a></td></tr>
  <tr><td>release-1.4</td><td><a href="https://dev.azure.com/Hyperledger/Fabric-Gateway-Java/_build/latest?definitionId=38&branchName=release-1.4"><img src="https://dev.azure.com/Hyperledger/Fabric-Gateway-Java/_apis/build/status/Fabric-Gateway-Java?branchName=release-1.4"></a></td></tr>
</table>
</div>

The Fabric Gateway SDK allows applications to interact with a Fabric blockchain network.  It provides a simple API to submit transactions to a ledger or query the contents of a ledger with minimal code.

The Gateway SDK implements the Fabric programming model as described in the [Developing Applications](https://hyperledger-fabric.readthedocs.io/en/latest/developapps/developing_applications.html) chapter of the Fabric documentation.

> **Note:** When developing applications for Hyperledger Fabric v2.4 and later, you are strongly encouraged to use the new [Fabric Gateway client API](https://hyperledger.github.io/fabric-gateway/).

## How to use 

The following shows a complete code sample of how to connect to a fabric network, submit a transaction and query the ledger state using an instantiated smart contract (fabcar sample).

```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;

class Sample {
    public static void main(String[] args) throws IOException {
        // Load an existing wallet holding identities used to access the network.
        Path walletDirectory = Paths.get("wallet");
        Wallet wallet = Wallets.newFileSystemWallet(walletDirectory);

        // Path to a common connection profile describing the network.
        Path networkConfigFile = Paths.get("connection.json");

        // Configure the gateway connection used to access the network.
        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, "user1")
                .networkConfig(networkConfigFile);

        // Create a gateway connection
        try (Gateway gateway = builder.connect()) {

            // Obtain a smart contract deployed on the network.
            Network network = gateway.getNetwork("mychannel");
            Contract contract = network.getContract("fabcar");

            // Submit transactions that store state to the ledger.
            byte[] createCarResult = contract.createTransaction("createCar")
                    .submit("CAR10", "VW", "Polo", "Grey", "Mary");
            System.out.println(new String(createCarResult, StandardCharsets.UTF_8));

            // Evaluate transactions that query state from the ledger.
            byte[] queryAllCarsResult = contract.evaluateTransaction("queryAllCars");
            System.out.println(new String(queryAllCarsResult, StandardCharsets.UTF_8));

        } catch (ContractException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

### API documentation

Full Javadoc documentation is published for each of the following versions:
- [2.2](https://hyperledger.github.io/fabric-gateway-java/release-2.2/)
- [1.4](https://hyperledger.github.io/fabric-gateway-java/release-1.4/)

### Maven

Add the following dependency to your project's `pom.xml` file:

```xml
<dependency>
  <groupId>org.hyperledger.fabric</groupId>
  <artifactId>fabric-gateway-java</artifactId>
  <version>2.2.0</version>
</dependency>
```

### Gradle

Add the following dependency to your project's `build.gradle` file:

```groovy
implementation 'org.hyperledger.fabric:fabric-gateway-java:2.2.0'
```

### Compatibility

The following table shows versions of Fabric, Java and other dependencies that are explicitly tested and that are supported for use with version 2.2 of the Fabric Gateway SDK. Refer to the appropriate GitHub branch for compatibility of other release versions.

|     | Tested | Supported |
| --- | ------ | --------- |
| **Fabric** | 2.2 | 2.2 |
| **Java** | 8, 11 | 8+ |
| **Platform** | Ubuntu 20.04 | |

#### Client applications running on POWER architecture
To run Java SDK clients on IBM POWER systems (e.g. zLinux), use Java 11 and set the SSL provider to ‘JDK’ by either supplying the -D command line option:

```-Dorg.hyperledger.fabric.sdk.connections.ssl.sslProvider=JDK```

or with the environment variable set as follows:

```ORG_HYPERLEDGER_FABRIC_SDK_CONNECTIONS_SSL_SSLPROVIDER=JDK```

## Building and testing

```commandline
git clone https://github.com/hyperledger/fabric-gateway-java.git
cd fabric-gateway-java
mvn install
```

The `mvn install` command will download the dependencies and run all the unit tests and scenario tests. It will also generate all the crypto material required by these tests.

Docker is required to run the scenario tests.

### Unit tests

All classes and methods have a high coverage (~90%) of unit tests. These are written using the [JUnit](https://junit.org/junit5/),
[AssertJ](https://joel-costigliola.github.io/assertj/) and [Mockito](https://site.mockito.org/) frameworks.

### Scenario tests

Scenario tests are written using the [Cucumber](https://cucumber.io/) BDD framework.
