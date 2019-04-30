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
import org.hyperledger.fabric.gateway.DefaultCommitHandlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ScenarioSteps implements En {
	Gateway gateway = null;
	String evaluateResponse = null;
	String submitResponse = null;
    boolean fabricRunning = false;
    static boolean channelsJoined = false;

	public ScenarioSteps() {
		Given("I have deployed a (.+?) Fabric network", (String tlsType) -> {
			// tlsType is either "tls" or "non-tls"
		    if (!fabricRunning) {
		    	fabricRunning = true;
		    }
		});

		Given("I have created and joined all channels from the (.+?) common connection profile", (String tlsType) -> {
		    // TODO this only does mychannel
			if(!channelsJoined) {
				String tlsOption;
				if (tlsType.equals("tls")) {
					tlsOption = "--tls true --cafile /etc/hyperledger/configtx/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem";
				} else {
					tlsOption = "";
				}
		    	exec(String.format("docker exec org1_cli peer channel create -o orderer.example.com:7050 -c mychannel -f /etc/hyperledger/configtx/channel.tx --outputBlock /etc/hyperledger/configtx/mychannel.block %s", tlsOption));
		    	exec(String.format("docker exec org1_cli peer channel join -b /etc/hyperledger/configtx/mychannel.block %s", tlsOption));
		    	exec(String.format("docker exec org2_cli peer channel join -b /etc/hyperledger/configtx/mychannel.block %s", tlsOption));
		    	exec(String.format("docker exec org1_cli peer channel update -o orderer.example.com:7050 -c mychannel -f /etc/hyperledger/configtx/Org1MSPanchors.tx %s", tlsOption));
		    	exec(String.format("docker exec org2_cli peer channel update -o orderer.example.com:7050 -c mychannel -f /etc/hyperledger/configtx/Org2MSPanchors.tx %s", tlsOption));
		    	channelsJoined = true;
			}
		});

		Given("^I have created a gateway named (.+?) as user (.+?) within (.+?) using the (.+?) common connection profile$", (String gatewayName, String userName, String orgName, String tlsType) -> {
	    	String ccp;
			switch (tlsType) {
			case "tls":
				ccp = "connection-tls.json";
				break;
			case "discovery":
				ccp = "connection-discovery.json";
				break;
			default:
				ccp = "connection.json";
			}
			Path networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway", ccp);
			Wallet wallet = createWallet();
			Gateway.Builder builder = Gateway. createBuilder();
			builder.identity(wallet, userName);
			builder.networkConfig(networkConfigPath);
			builder.commitTimeout(1, TimeUnit.MINUTES);
			builder.commitHandler(DefaultCommitHandlers.NETWORK_SCOPE_ANYFORTX);
			if(tlsType.equals("discovery")) {
				builder.discovery(true);
			}
			gateway = builder.connect();
		});

		Given("^I install\\/instantiate (.+?) chaincode named (.+?) at version (.+?) as (.+?) to the (.+?) Fabric network for all organizations on channel (.+?) with endorsement policy (.+?) and args (.+?)$", (String ccType, String ccName, String version, String ccId, String tlsType, String channelName, String policyType, String args) -> {
		    // Write code here that turns the phrase above into concrete actions
		    String[] params = args.substring(1, args.length()-1).split(",");
		    String transactionName = params[0];
		    String[] params2_N = new String[params.length - 1];
		    System.arraycopy(params, 1, params2_N, 0, params.length - 1);
		    String arguments = "[\"" + String.join("\",\"", params2_N) + "\"]";
			String tlsOption;
			String initArg = String.format("{\"function\":\"%s\",\"Args\":%s}", transactionName, arguments);
			if (tlsType.equals("tls")) {
				tlsOption = "--tls true --cafile /etc/hyperledger/configtx/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem";
			} else {
				tlsOption = "";
			}

		    String ccPath = String.format("/opt/gopath/src/github.com/chaincode/%s/%s", ccType, ccName);
	    	exec(String.format("docker exec org1_cli peer chaincode install -l %s -n %s -v %s -p %s", ccType, ccName, version, ccPath));
	    	exec(String.format("docker exec org2_cli peer chaincode install -l %s -n %s -v %s -p %s", ccType, ccName, version, ccPath));
	    	Thread.sleep(3000);
	    	exec(String.format("docker exec org1_cli peer chaincode instantiate -o orderer.example.com:7050 -l %s -C %s -n %s -v %s -c %s -P AND(\"Org1MSP.member\",\"Org2MSP.member\") %s", ccType, channelName, ccName, version, initArg, tlsOption));
	    	Thread.sleep(10000);
		});

		Given("I update channel with name (.+?) with config file (.+?) from the (.+?) common connection profile", (String channelName, String txFileName, String tlsType) -> {
		    // Write code here that turns the phrase above into concrete actions
		    throw new cucumber.api.PendingException();
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

		Then("^The gateway named (.+?) has a (.+?) type response$", (String gatewayName, String type) -> {
			switch (type) {
			case "evaluate":
				assertNotNull(evaluateResponse);
				break;
			case "submit":
				assertNotNull(submitResponse);
				break;
			}
		});

	}

	private static void exec(String command) throws Exception {
		System.err.println(command);
		Process process = Runtime.getRuntime().exec(command);
		int exitCode = process.waitFor();

		// get STDERR for the process and print it
		InputStream is = process.getErrorStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		String line;
		while ((line = br.readLine()) != null) {
			System.err.println(line);
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
		// get STDERR for the process and print it
		InputStream is = process.getErrorStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		String line;
		while ((line = br.readLine()) != null) {
			System.err.println(line);
		}

		assertEquals(exitCode, 0);
	}

	private static Wallet createWallet() throws IOException, GatewayException {
		Path credentialPath = Paths.get("src", "test", "fixtures", "crypto-material", "crypto-config",
				"peerOrganizations", "org1.example.com", "users", "User1@org1.example.com", "msp");
		String certificatePem = readFile(
				credentialPath.resolve(Paths.get("signcerts", "User1@org1.example.com-cert.pem")));
		PrivateKey privateKey = readPrivateKey(credentialPath.resolve(Paths.get("keystore", "key.pem")));
		Wallet wallet = Wallet.createInMemoryWallet();
		wallet.put("User1", Wallet.Identity.createIdentity("Org1MSP", certificatePem, privateKey));
		return wallet;
	}

	private static String readFile(Path file) throws IOException {
		StringBuilder contents = new StringBuilder();
		if (Files.exists(file)) {
			try (BufferedReader fr = Files.newBufferedReader(file)) {
				String line;
				while ((line = fr.readLine()) != null) {
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
