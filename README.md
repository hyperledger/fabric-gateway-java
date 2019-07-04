# Hyperledger Fabric Gateway SDK for Java

The Fabric Gateway SDK allows applications to interact with a Fabric blockchain network.  It provides a simple API to submit transactions to a ledger or query the contents of a ledger with minimal code.

The Gateway SDK implements the Fabric programming model as described in the [Developing Applications](https://hyperledger-fabric.readthedocs.io/en/release-1.4/developapps/developing_applications.html) chapter of the Fabric documentation.

## How to use 

The following shows a complete code sample of how to connect to a fabric network, submit a transaction and query the ledger state using an instantiated smart contract (fabcar sample).

```java
package org.example;

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

public final class Sample {
    public static void main(String[] args) throws IOException {

        // Load an existing wallet holding identities used to access the network.
        Path walletDirectory = Paths.get("wallet");
        Wallet wallet = Wallet.createFileSystemWallet(walletDirectory);

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
            byte[] createCarResult = contract.submitTransaction("createCar", "CAR10", "VW", "Polo", "Grey", "Mary");
            System.out.println(new String(createCarResult, StandardCharsets.UTF_8));

            // Evaluate transactions that query state from the ledger.
            byte[] queryAllCarsResult = contract.evaluateTransaction("queryAllCars");
            System.out.println(new String(queryAllCarsResult, StandardCharsets.UTF_8));

        } catch (ContractException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
```


## Building and testing



```
git clone https://github.com/hyperledger/fabric-gateway-java.git
cd fabric-gateway-java
mvn install
```

The `mvn install` command will download the dependencies and run all the unit tests and scenario tests.  It will also generate all the crypto material required by these tests.


### Unit tests

All classes and methods have a high coverage (~90%) of unit tests.  These are written using the JUnit framework

### Scenario tests

Scenario tests are written using the [Cucumber](https://cucumber.io/) BDD framework

## Documentation

Full Javadoc documentation of the SDK is published [here](https://fabric-gateway-java.github.io/).
