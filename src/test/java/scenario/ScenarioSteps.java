package scenario;

import cucumber.api.java8.En;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;

import static org.junit.Assert.assertEquals;

public class ScenarioSteps implements En {
	Gateway gateway = null;
	String evaluateResponse = null;
	String submitResponse = null;
	boolean fabricRunning = false;

	public ScenarioSteps() {
		Given("I have deployed a (.+?) Fabric network", (String tlsType) -> {
			// tlsType is either "tls" or "non-tls"
		    if (!fabricRunning) {
		    	fabricRunning = true;
		    }
		});

		Given("I have created and joined all channels from the (.+?) common connection profile", (String tlsType) -> {
		    // TODO this only does mychannel
			String tlsOption;
			if (tlsType.equals("tls")) {
				tlsOption = "--tls true --cafile /etc/hyperledger/configtx/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem";
			} else {
				tlsOption = "";
			}
	    	exec(String.format("docker exec org1_cli peer channel create -o orderer.example.com:7050 -c mychannel -f /etc/hyperledger/configtx/channel.tx --outputBlock /etc/hyperledger/configtx/mychannel.block %s", tlsOption));
	    	exec(String.format("docker exec org1_cli peer channel join -b /etc/hyperledger/configtx/mychannel.block %s", tlsOption));
	    	exec(String.format("docker exec org2_cli peer channel join -b /etc/hyperledger/configtx/mychannel.block %s", tlsOption));
		});

		Given("^I have created a gateway named (.+?) as user (.+?) within (.+?) using the (.+?) common connection profile$", (String gatewayName, String userName, String orgName, String tlsType) -> {
	    	String ccp = tlsType.equals("tls") ? "connection-tls.json" : "connection.json";
			Path networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", ccp);
			Wallet wallet = createWallet();
			Gateway.Builder builder = Gateway. createBuilder();
			builder.identity(wallet, userName);
			builder.networkConfig(networkConfigPath);
			gateway = builder.connect();
		});

		Given("^I install\\/instantiate (.+?) chaincode named (.+?) at version (.+?) as (.+?) to the (.+?) Fabric network for all organizations on channel (.+?) with endorsement policy (.+?) and args (.+?)$", (String ccType, String ccName, String version, String ccId, String tlsType, String channelName, String policyType, String args) -> {
		    // Write code here that turns the phrase above into concrete actions
		    String[] params = args.substring(1, args.length()-1).split(",");
			String tlsOption;
			if (tlsType.equals("tls")) {
				tlsOption = "--tls true --cafile /etc/hyperledger/configtx/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem";
			} else {
				tlsOption = "";
			}

		    String ccPath = String.format("/opt/gopath/src/github.com/chaincode/%s/%s", ccType, ccName);
	    	exec(String.format("docker exec org1_cli peer chaincode install -l %s -n %s -v %s -p %s", ccType, ccName, version, ccPath));
	    	exec(String.format("docker exec org2_cli peer chaincode install -l %s -n %s -v %s -p %s", ccType, ccName, version, ccPath));
	    	Thread.sleep(3000);
	    	exec(String.format("docker exec org1_cli peer chaincode instantiate -o orderer.example.com:7050 -l %s -C %s -n %s -v %s -c {\"function\":\"initLedger\",\"Args\":[\"\"]} -P OR(\"Org1MSP.member\",\"Org2MSP.member\") %s", ccType, channelName, ccName, version, tlsOption));
	    	Thread.sleep(3000);
		});

		When("^I use the gateway named (.+?) to submit a transaction with args (.+?) for chaincode (.+?) instantiated on channel (.+?)$", (String gatewayName, String args, String ccName, String channelName) -> {
		    // Write code here that turns the phrase above into concrete actions
		    String[] params = args.substring(1, args.length()-1).split(",");
		    Network network = gateway.getNetwork(channelName);
		    Contract contract = network.getContract(ccName);
		    String transactionName = params[0];
		    String[] arguments = new String[params.length - 1];
		    System.arraycopy(params, 1, arguments, 0, params.length - 1);
		    byte[] retval = contract.submitTransaction(transactionName, arguments);
		    submitResponse = new String(retval);
		});

		Then("The gateway named test_gateway has a submit type response", () -> {
		    // Write code here that turns the phrase above into concrete actions
		    //throw new cucumber.api.PendingException();
		});

		When("^I use the gateway named (.+?) to evaluate transaction with args (.+?) for chaincode (.+?) instantiated on channel (.+?)$", (String gatewayName, String args, String ccName, String channelName) -> {
		    String[] params = args.substring(1, args.length()-1).split(",");
		    Network network = gateway.getNetwork(channelName);
		    Contract contract = network.getContract(ccName);
		    String transactionName = params[0];
		    String[] arguments = new String[params.length - 1];
		    System.arraycopy(params, 1, arguments, 0, params.length - 1);
		    byte[] retval = contract.evaluateTransaction(transactionName, arguments);
		    evaluateResponse = new String(retval);
		});

		Then("^The gateway named (.+?) has a (.+?) type response matching (.+?)$", (String gatewayName, String type, String expected) -> {
			switch (type) {
				case "evaluate":
					assertEquals(expected, evaluateResponse);
					break;
				case "submit":
					assertEquals(expected, submitResponse);
					break;
			}
		});
	}

	private static void exec(String command) throws Exception {
    	System.out.println(command);
    	Process process = Runtime.getRuntime().exec(command);
    	int exitCode = process.waitFor();

    	// get STDERR for the process and print it
        InputStream is = process.getErrorStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

    	String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }

    	assertEquals(exitCode, 0);

	}

	static void startFabric(boolean tls) throws Exception {
    	createCryptoMaterial();
    	String yaml = tls ? "docker-compose-tls.yaml" : "docker-compose.yaml";
    	Path dockerCompose = Paths.get("src", "test", "fixtures", "docker-compose", yaml);
    	exec(String.format("docker-compose -f %s -p node up -d", dockerCompose.toAbsolutePath().toString()));
    	Thread.sleep(10000);
	}

	static void stopFabric(boolean tls) throws Exception {
    	String yaml = tls ? "docker-compose-tls.yaml" : "docker-compose.yaml";
    	Path dockerCompose = Paths.get("src", "test", "fixtures", "docker-compose", yaml);
    	exec(String.format("docker-compose -f %s -p node down", dockerCompose.toAbsolutePath().toString()));
	}

	private static void createCryptoMaterial() throws Exception {
		File fixtures = Paths.get("src", "test", "fixtures").toFile();
		Process process = Runtime.getRuntime().exec("sh generate.sh", null, fixtures);
    	int exitCode = process.waitFor();
    	assertEquals(exitCode, 0);
	}

	private static Wallet createWallet() throws IOException, GatewayException {
		Path credentialPath = Paths.get("src", "test", "fixtures", "crypto-material", "crypto-config", "peerOrganizations", "org1.example.com", "users", "User1@org1.example.com", "msp");
		String certificatePem = readFile(credentialPath.resolve(Paths.get("signcerts", "User1@org1.example.com-cert.pem")));
		PrivateKey privateKey = readPrivateKey(credentialPath.resolve(Paths.get("keystore", "key.pem")));
		Wallet wallet = Wallet.createInMemoryWallet();
		wallet.put("user1", Wallet.Identity.createIdentity("Org1MSP", certificatePem, privateKey));
		return wallet;
	}

	private static String readFile(Path file) throws IOException {
		StringBuilder contents = new StringBuilder();
		if (Files.exists(file)) {
			try (BufferedReader fr = Files.newBufferedReader(file)) {
				String line;
				while((line = fr.readLine()) != null) {
					contents.append(line);
					contents.append('\n');
				}
			}
			return contents.toString();
		}
		return null;
	}

	private static PrivateKey readPrivateKey(Path pemFile) throws IOException {
		if (Files.exists(pemFile)) {
			try (PEMParser parser = new PEMParser(Files.newBufferedReader(pemFile))) {
				Object key = parser.readObject();
				JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
				if (key instanceof PrivateKeyInfo) {
					return converter.getPrivateKey((PrivateKeyInfo) key);
				} else {
					return converter.getPrivateKey(((PEMKeyPair) key).getPrivateKeyInfo());
				}
			}
		}
		return null;
	}

}
