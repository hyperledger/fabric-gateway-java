# Hyperledger Fabric Gateway SDK for Java

The Fabric Gateway SDK allows applications to interact with a Fabric blockchain network.  It provides a simple API to submit transactions to a ledger or query the contents of a ledger with minimal code.

The Gateway SDK implements the Fabric programming model as described in the [Developing Applications](https://hyperledger-fabric.readthedocs.io/en/release-1.4/developapps/developing_applications.html) chapter of the Fabric documentation.

## How to use 

The following shows a complete code sample of how to connect to a fabric network, submit a transaction and query the ledger state using an instantiated smart contract (fabcar sample).

```
package org.example;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;

public interface Sample {
  static void main(String[] args) throws Exception {

    // Create a new file system based wallet for managing identities.
    Path walletPath = Paths.get("wallet");
    Wallet wallet = Wallet.createFileSystemWallet(walletPath);

    // load a CCP
    Path networkConfigPath = Paths.get("..", "..", "basic-network", "connection.json");

    Gateway.Builder builder = Gateway.createBuilder();
    builder.identity(wallet, "user1").networkConfig(networkConfigPath);

    // create a gateway connection
    try (Gateway gateway = builder.connect()) {
      // get the network and contract
      Network network = gateway.getNetwork("mychannel");
      Contract contract = network.getContract("fabcar");

      byte[] result = contract.submitTransaction("createCar", "CAR10", "VW", "Polo", "Grey", "Mary");
      System.out.println(result);

      result = contract.evaluateTransaction("queryAllCars");
      System.out.println(new String(result));

    } catch (Exception ex) {
      ex.printStackTrace();
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
