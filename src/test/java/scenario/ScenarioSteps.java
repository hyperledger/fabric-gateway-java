package scenario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.DefaultCommitHandlers;
import org.hyperledger.fabric.gateway.DefaultQueryHandlers;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.gateway.Wallet;

import cucumber.api.java8.En;
import io.cucumber.datatable.DataTable;

public class ScenarioSteps implements En {
	private static Set<String> runningChaincodes = new HashSet<>();
	private static boolean channelsJoined = false;

	private String fabricNetworkType = null;
	private Gateway.Builder gatewayBuilder = null;
	private Gateway gateway = null;
	private String response = null;
	private Transaction transaction = null;

	public ScenarioSteps() {
		Given("^I have deployed a (.+) Fabric network$", (String tlsType) -> {
			// tlsType is either "tls" or "non-tls"
			fabricNetworkType = tlsType;
		});

		Given("^I have created and joined all channels from the (.+) connection profile$", (String tlsType) -> {
			// TODO this only does mychannel
			if (!channelsJoined) {
				final List<String> tlsOptions;
				if (tlsType.equals("tls")) {
					tlsOptions = Arrays.asList("--tls", "true", "--cafile", "/etc/hyperledger/configtx/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem");
				} else {
					tlsOptions = Collections.emptyList();
				}

				List<String> createChannelCommand = new ArrayList<>();
				Collections.addAll(createChannelCommand,
						"docker", "exec", "org1_cli", "peer", "channel", "create",
						"-o", "orderer.example.com:7050",
						"-c", "mychannel",
						"-f", "/etc/hyperledger/configtx/channel.tx",
						"--outputBlock", "/etc/hyperledger/configtx/mychannel.block");
				createChannelCommand.addAll(tlsOptions);
				exec(createChannelCommand);

				List<String> org1JoinChannelCommand = new ArrayList<>();
				Collections.addAll(org1JoinChannelCommand,
						"docker", "exec", "org1_cli", "peer", "channel", "join",
						"-b", "/etc/hyperledger/configtx/mychannel.block"
				);
				org1JoinChannelCommand.addAll(tlsOptions);
				exec(org1JoinChannelCommand);

				List<String> org2JoinChannelCommand = new ArrayList<>();
				Collections.addAll(org2JoinChannelCommand,
						"docker", "exec", "org2_cli", "peer", "channel", "join",
						"-b", "/etc/hyperledger/configtx/mychannel.block"
				);
				org2JoinChannelCommand.addAll(tlsOptions);
				exec(org2JoinChannelCommand);

				List<String> org1AnchorPeersCommand = new ArrayList<>();
				Collections.addAll(org1AnchorPeersCommand,
						"docker", "exec", "org1_cli", "peer", "channel", "update",
						"-o", "orderer.example.com:7050",
						"-c", "mychannel",
						"-f", "/etc/hyperledger/configtx/Org1MSPanchors.tx"
				);
				org1AnchorPeersCommand.addAll(tlsOptions);
				exec(org1AnchorPeersCommand);

				List<String> org2AnchorPeersCommand = new ArrayList<>();
				Collections.addAll(org2AnchorPeersCommand,
						"docker", "exec", "org2_cli", "peer", "channel", "update",
						"-o", "orderer.example.com:7050",
						"-c", "mychannel",
						"-f", "/etc/hyperledger/configtx/Org2MSPanchors.tx"
				);
				org2AnchorPeersCommand.addAll(tlsOptions);
				exec(org2AnchorPeersCommand);

				channelsJoined = true;
			}
		});

		Given("^I have a gateway as user (.+) using the (.+) connection profile$",
				(String userName, String tlsType) -> {
					Wallet wallet = createWallet();
					gatewayBuilder = Gateway.createBuilder();
					gatewayBuilder.identity(wallet, userName);
					gatewayBuilder.networkConfig(getNetworkConfigPath(tlsType));
					gatewayBuilder.commitTimeout(1, TimeUnit.MINUTES);
					if (tlsType.equals("discovery")) {
						gatewayBuilder.discovery(true);
					}
				});

		Given("^I configure the gateway to use the default (.+) commit handler$",
				(String handlerName) -> gatewayBuilder.commitHandler(DefaultCommitHandlers.valueOf(handlerName)));

		Given("^I configure the gateway to use the default (.+) query handler$",
				(String handlerName) -> gatewayBuilder.queryHandler(DefaultQueryHandlers.valueOf(handlerName)));

		Given("^I connect the gateway$", () -> gateway = gatewayBuilder.connect());

		Given("^I deploy (.+) chaincode named (.+) at version (.+) for all organizations on channel (.+) with endorsement policy (.+) and arguments (.+)$",
				(String ccType, String ccName, String version, String channelName,
						String policyType, String argsJson) -> {
					String mangledName = ccName + version + channelName;
					if (runningChaincodes.contains(mangledName)) {
						return;
					}

					JsonArray functionAndArgs = parseJsonArray(argsJson);
					String transactionName = functionAndArgs.getString(0);
					JsonArray args = Json.createArrayBuilder(functionAndArgs).remove(0).build();
					String initArg = Json.createObjectBuilder()
							.add("function", transactionName)
							.add("Args", args)
							.build().toString();

					final List<String> tlsOptions;
					if (fabricNetworkType.equals("tls")) {
						tlsOptions = Arrays.asList("--tls", "true", "--cafile", "/etc/hyperledger/configtx/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem");
					} else {
						tlsOptions = Collections.emptyList();
					}

					String ccPath = Paths.get(FileSystems.getDefault().getSeparator(),
							"opt", "gopath", "src", "github.com", "chaincode", ccType, ccName).toString();

					exec("docker", "exec", "org1_cli", "peer", "chaincode", "install",
							"-l", ccType,
							"-n", ccName,
							"-v", version,
							"-p", ccPath
					);

					exec("docker", "exec", "org2_cli", "peer", "chaincode", "install",
							"-l", ccType,
							"-n", ccName,
							"-v", version,
							"-p", ccPath
					);

					Thread.sleep(3000);

					List<String> instantiateCommand = new ArrayList<>();
					Collections.addAll(instantiateCommand,
							"docker", "exec", "org1_cli", "peer", "chaincode", "instantiate",
							"-o", "orderer.example.com:7050",
							"-l", ccType,
							"-C", channelName,
							"-n", ccName,
							"-v", version,
							"-c", initArg,
							"-P", "AND(\"Org1MSP.member\",\"Org2MSP.member\")"
					);
					instantiateCommand.addAll(tlsOptions);
					exec(instantiateCommand);

					runningChaincodes.add(mangledName);
					Thread.sleep(60000);
				});

		Given("^I update channel with name (.+) with config file (.+) from the (.+) connection profile$",
				(String channelName, String txFileName, String tlsType) -> {
					throw new cucumber.api.PendingException();
				});

		When("^I prepare a transaction named (.+) for contract (.+) on network (.+)$",
				(String txnFn, String ccName, String channelName) -> {
					Network network = gateway.getNetwork(channelName);
					Contract contract = network.getContract(ccName);
					transaction = contract.createTransaction(txnFn);
				});

		When("^I (submit|evaluate) the transaction with arguments (.+)$",
				(String action, String argsJson) -> {

					String[] args = toStringArray(parseJsonArray(argsJson));

					final byte[] result;
					if (action.equals("submit")) {
						result = transaction.submit(args);
					} else {
						result = transaction.evaluate(args);
					}
					response = new String(result, StandardCharsets.UTF_8);
				});

		When("^I set transient data on the transaction to$", (DataTable data) -> {
			Map<String, String> table = data.asMap(String.class, String.class);
			Map<String, byte[]> transientMap = new HashMap<>();
			table.forEach((k, v) -> transientMap.put(k, v.getBytes(StandardCharsets.UTF_8)));
			transaction.setTransient(transientMap);
		});

		Then("^a response should be received$", () -> assertNotNull(response));

		Then("^the response should match (.+)$",	(String expected) -> assertEquals(expected, response));

		Then("^the response should be JSON matching$",
				(String expected) -> {
					assertNotNull(response);
					try (JsonReader expectedReader = createJsonReader(expected);
							JsonReader actualReader = createJsonReader(response)) {
						JsonObject expectedObject = expectedReader.readObject();
						JsonObject actualObject = actualReader.readObject();
						assertEquals(expectedObject, actualObject);
					}
				});
	}

	private Path getNetworkConfigPath(String configType) {
		Path networkConfigDir = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway");
		switch (configType) {
			case "tls":
				return networkConfigDir.resolve("connection-tls.json");
			case "discovery":
				return networkConfigDir.resolve("connection-discovery.json");
			case "basic":
				return networkConfigDir.resolve("connection.json");
			default:
				throw new IllegalArgumentException("Unknown network configuration type: " + configType);
		}
	}

	private JsonArray parseJsonArray(String jsonString) {
		try (JsonReader reader = createJsonReader(jsonString)) {
			return reader.readArray();
		}
	}

	private static JsonReader createJsonReader(String jsonString) {
		return Json.createReader(new StringReader(jsonString));
	}

	private String[] toStringArray(JsonArray jsonArray) {
		return jsonArray.getValuesAs(JsonString.class).stream()
				.map(JsonString::getString)
				.toArray(String[]::new);
	}

	private static void exec(String command, String... args) throws IOException, InterruptedException {
		List<String> argList = new ArrayList<>(1 + args.length);
		argList.add(command);
		Collections.addAll(argList, args);
		exec(argList);
	}

	private static void exec(List<String> commandArgs) throws IOException, InterruptedException{
		System.err.println(commandArgs);
		Process process = Runtime.getRuntime().exec(commandArgs.toArray(new String[0]));
		int exitCode = process.waitFor();

		// get STDERR for the process and print it
		InputStream errorStream = process.getErrorStream();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
			for (String line; (line = reader.readLine()) != null; ) {
				System.err.println(line);
			}
		}

		assertEquals("Failed to execute command: " + commandArgs, exitCode, 0);

	}

	static void startFabric(boolean tls) throws Exception {
		createCryptoMaterial();
		String yaml = tls ? "docker-compose-tls.yaml" : "docker-compose.yaml";
		String dockerComposeFile = Paths.get("src", "test", "fixtures", "docker-compose", yaml).toString();
		exec("docker-compose", "-f", dockerComposeFile, "-p", "node", "up", "-d");
		Thread.sleep(10000);
	}

	static void stopFabric(boolean tls) throws Exception {
		String yaml = tls ? "docker-compose-tls.yaml" : "docker-compose.yaml";
		Path dockerCompose = Paths.get("src", "test", "fixtures", "docker-compose", yaml);
		exec("docker-compose", "-f", dockerCompose.toAbsolutePath().toString(), "-p", "node", "down");
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
		Path certificatePem = credentialPath.resolve(Paths.get("signcerts", "User1@org1.example.com-cert.pem"));
		Path privateKey = credentialPath.resolve(Paths.get("keystore", "key.pem"));
		Wallet wallet = Wallet.createInMemoryWallet();
		wallet.put("User1", Wallet.Identity.createIdentity("Org1MSP",
				Files.newBufferedReader(certificatePem), Files.newBufferedReader(privateKey)));
		return wallet;
	}

}
