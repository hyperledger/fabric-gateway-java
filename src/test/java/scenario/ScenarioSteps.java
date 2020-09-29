package scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import io.cucumber.datatable.DataTable;
import io.cucumber.docstring.DocString;
import io.cucumber.java8.En;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractEvent;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.DefaultCheckpointers;
import org.hyperledger.fabric.gateway.DefaultCommitHandlers;
import org.hyperledger.fabric.gateway.DefaultQueryHandlers;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.sample.SampleCommitHandlerFactory;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.helper.Config;

import static org.assertj.core.api.Assertions.assertThat;

public class ScenarioSteps implements En {
    private static final long EVENT_TIMEOUT_SECONDS = 30;
    private static final Set<String> runningChaincodes = new HashSet<>();
    private static boolean channelsJoined = false;
    private static final String COUCHDB_SERVER_URL = "http://admin:adminpw@localhost:5984";
    private static final String DOCKER_COMPOSE_TLS_FILE = "docker-compose-tls.yaml";
    private static final String DOCKER_COMPOSE_FILE = "docker-compose.yaml";
    private static final Path DOCKER_COMPOSE_DIR = Paths.get("src", "test", "fixtures", "docker-compose").toAbsolutePath();


    private String fabricNetworkType;
    private Gateway.Builder gatewayBuilder;
    private Gateway gateway;
    private Network network;
    private Contract contract;
    private TransactionInvocation transactionInvocation;
    private Consumer<BlockEvent> blockListener;
    private final BlockingQueue<BlockEvent> blockEvents = new LinkedBlockingQueue<>();
    private Consumer<ContractEvent> contractListener;
    private final BlockingQueue<ContractEvent> contractEvents = new LinkedBlockingQueue<>();
    private final Path checkpointFile;
    private Checkpointer checkpointer;
    private Wallet wallet = Wallets.newInMemoryWallet();

    static {
        System.setProperty(Config.SERVICE_DISCOVER_AS_LOCALHOST, "true");
    }

    public ScenarioSteps() throws IOException {
        checkpointFile = TestUtils.getInstance().getUnusedFilePath();

        After(() -> {
            if (gateway != null) {
                gateway.close();
            }

            emptyWallet();
        });

        Given("I have deployed a {word} Fabric network", (String tlsType) -> {
            // tlsType is either "tls" or "non-tls"
            fabricNetworkType = tlsType;
        });

        Given("I have created and joined all channels from the {word} connection profile", (String tlsType) -> {
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

        Given("I use a CouchDB wallet", () -> {
            URL serverUrl = new URL(COUCHDB_SERVER_URL);
            wallet = Wallets.newCouchDBWallet(serverUrl, "wallet");
        });

        Given("I have a gateway as user {word} using the {word} connection profile",
                (String userName, String tlsType) -> {
                    prepareGateway(tlsType);
                    populateWallet();
                    gatewayBuilder.identity(wallet, userName);
                });

        Given("I have a gateway with identity User1 using the {word} connection profile",
                (String tlsType) -> {
                    prepareGateway(tlsType);
                    Identity identity = newOrg1UserIdentity();
                    gatewayBuilder.identity(identity);
                });

        Given("I configure the gateway to use the default {word} commit handler",
                (String handlerName) -> gatewayBuilder.commitHandler(DefaultCommitHandlers.valueOf(handlerName)));

        Given("I configure the gateway to use the sample commit handler",
                () -> gatewayBuilder.commitHandler(SampleCommitHandlerFactory.INSTANCE));

        Given("I configure the gateway to use the default {word} query handler",
                (String handlerName) -> gatewayBuilder.queryHandler(DefaultQueryHandlers.valueOf(handlerName)));

        Given("I connect the gateway", () -> gateway = gatewayBuilder.connect());

        Given("I deploy {word} chaincode named {word} at version {word} for all organizations on channel {word} with endorsement policy {} and arguments {}",
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

                    String ccLabel = ccName + "v" + version;
                    String ccPackage = ccName + ".tar.gz";

                    // Org1
                    exec("docker", "exec", "org1_cli", "peer", "lifecycle", "chaincode", "package", ccPackage, "--lang",
                            ccType, "--label", ccLabel, "--path", ccPath);

                    exec("docker", "exec", "org1_cli", "peer", "lifecycle", "chaincode", "install", ccPackage);

                    String installed = exec("docker", "exec", "org1_cli", "peer", "lifecycle", "chaincode",
                            "queryinstalled");
                    Pattern regex = Pattern.compile(".*Package ID: (.*), Label: " + ccLabel + ".*");
                    Matcher matcher = regex.matcher(installed);
                    if (!matcher.matches()) {
                        System.out.println(installed);
                        throw new IllegalStateException("Cannot find installed chaincode for Org1: " + ccLabel);
                    }
                    String packageId = matcher.group(1);

                    List<String> approveCommand = new ArrayList<>();
                    Collections.addAll(approveCommand, "docker", "exec", "org1_cli", "peer", "lifecycle", "chaincode",
                            "approveformyorg", "--package-id", packageId, "--channelID", channelName, "--name", ccName,
                            "--version", version, "--signature-policy", "AND(\"Org1MSP.member\",\"Org2MSP.member\")",
                            "--sequence", "1", "--waitForEvent"
                    );
                    approveCommand.addAll(tlsOptions);
                    exec(approveCommand);

                    // Org2
                    exec("docker", "exec", "org2_cli", "peer", "lifecycle", "chaincode", "package", ccPackage, "--lang",
                            ccType, "--label", ccLabel,
                            "--path", ccPath);

                    exec("docker", "exec", "org2_cli", "peer", "lifecycle", "chaincode", "install", ccPackage);

                    installed = exec("docker", "exec", "org2_cli", "peer", "lifecycle", "chaincode",
                            "queryinstalled");
                    matcher = regex.matcher(installed);
                    if (!matcher.matches()) {
                        System.err.println(installed);
                        throw new IllegalStateException("Cannot find installed chaincode for Org2: " + ccLabel);
                    }
                    packageId = matcher.group(1);

                    approveCommand = new ArrayList<>();
                    Collections.addAll(approveCommand, "docker", "exec", "org2_cli", "peer", "lifecycle", "chaincode",
                            "approveformyorg", "--package-id", packageId, "--channelID", channelName, "--name", ccName,
                            "--version", version, "--signature-policy", "AND(\"Org1MSP.member\",\"Org2MSP.member\")",
                            "--sequence", "1", "--waitForEvent");
                    approveCommand.addAll(tlsOptions);
                    exec(approveCommand);

                    // commit
                    List<String> commitCommand = new ArrayList<>();
                    Collections.addAll(commitCommand, "docker", "exec", "org1_cli", "peer", "lifecycle", "chaincode",
                            "commit", "--channelID", channelName, "--name", ccName, "--version", version,
                            "--signature-policy", "AND(\"Org1MSP.member\",\"Org2MSP.member\")", "--sequence", "1",
                            "--waitForEvent", "--peerAddresses", "peer0.org1.example.com:7051", "--peerAddresses",
                            "peer0.org2.example.com:8051",
                            "--tlsRootCertFiles",
                            "/etc/hyperledger/configtx/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt",
                            "--tlsRootCertFiles",
                            "/etc/hyperledger/configtx/crypto-config/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt");
                    commitCommand.addAll(tlsOptions);
                    exec(commitCommand);


                    runningChaincodes.add(mangledName);
                    Thread.sleep(60000);
                });

        Given("I use the {word} network", (String networkName) -> network = gateway.getNetwork(networkName));

        Given("I use the {word} contract", (String contractName) -> contract = network.getContract(contractName));

        When("I prepare a(n) {word} transaction", (String transactionName) -> {
            Transaction transaction = contract.createTransaction(transactionName);
            transactionInvocation = TransactionInvocation.expectSuccess(transaction);
        });

        When("I prepare a(n) {word} transaction that I expect to fail", (String transactionName) -> {
            Transaction transaction = contract.createTransaction(transactionName);
            transactionInvocation = TransactionInvocation.expectFail(transaction);
        });

        When("^I (submit|evaluate) the transaction with arguments (.+)$",
                (String action, String argsJson) -> {
                    String[] args = newStringArray(parseJsonArray(argsJson));
                    if (action.equals("submit")) {
                        transactionInvocation.submit(args);
                    } else {
                        transactionInvocation.evaluate(args);
                    }
                });

        When("I set transient data on the transaction to", (DataTable data) -> {
            Map<String, String> table = data.asMap(String.class, String.class);
            Map<String, byte[]> transientMap = new HashMap<>();
            table.forEach((k, v) -> transientMap.put(k, v.getBytes(StandardCharsets.UTF_8)));
            transactionInvocation.setTransient(transientMap);
        });

        When("I set endorsing peers on the transaction to {}", (String peersJson) -> {
            Set<String> peerNames = Arrays.stream(newStringArray(parseJsonArray(peersJson)))
                    .collect(Collectors.toSet());
            Collection<Peer> peers = network.getChannel().getPeers().stream()
                    .filter(peer -> peerNames.contains(peer.getName()))
                    .collect(Collectors.toList());
            transactionInvocation.setEndorsingPeers(peers);
        });

        When("I add a block listener", () -> {
            clearBlockListener();
            blockListener = network.addBlockListener(blockEvents::add);
        });

        When("I add a block listener with a file checkpointer", () -> {
            clearBlockListener();
            blockListener = network.addBlockListener(fileCheckpointer(), blockEvents::add);
        });

        When("I add a block listener with replay from block {int}", (Integer startBlock) -> {
            clearBlockListener();
            blockListener = network.addBlockListener(startBlock, blockEvents::add);
        });

        When("I wait for a block event to be received", this::getBlockEvent);

        When("I remove the block listener", this::clearBlockListener);

        When("I add a contract listener for events matching {string}", (String eventName) -> {
            clearContractListener();
            Pattern eventNamePattern = Pattern.compile(eventName);
            contractListener = contract.addContractListener(contractEvents::add, eventNamePattern);
        });

        When("I add a contract listener for events matching {string} with a file checkpointer",
                (String eventName) -> {
                    clearContractListener();
                    Pattern eventNamePattern = Pattern.compile(eventName);
                    contractListener = contract.addContractListener(fileCheckpointer(), contractEvents::add, eventNamePattern);
                });

        When("I add a contract listener for events matching {string} with replay from block {int}",
                (String eventName, Integer startBlock) -> {
                    clearContractListener();
                    Pattern eventNamePattern = Pattern.compile(eventName);
                    contractListener = contract.addContractListener(startBlock, contractEvents::add, eventNamePattern);
                });

        When("I wait for a contract event with payload {string} to be received", this::getContractEvent);

        When("I remove the contract listener", this::clearContractListener);

        When("I put an identity named {string} into the wallet", (String label) -> {
            Identity identity = newOrg1UserIdentity();
            wallet.put(label, identity);
        });

        When("I remove an identity named {string} from the wallet", (String label) -> wallet.remove(label));

        Then("a response should be received", () -> transactionInvocation.getResponse());

        Then("the response should match {}",
                (String expected) -> assertThat(transactionInvocation.getResponse()).isEqualTo(expected));

        Then("the response should be JSON matching", (DocString expected) -> {
            try (JsonReader expectedReader = createJsonReader(expected.getContent());
                 JsonReader actualReader = createJsonReader(transactionInvocation.getResponse())) {
                JsonObject expectedObject = expectedReader.readObject();
                JsonObject actualObject = actualReader.readObject();
                assertThat(actualObject).isEqualTo(expectedObject);
            }
        });

        Then("the error message should contain {string}",
                (String expected) -> assertThat(transactionInvocation.getError().getMessage()).contains(expected));

        Then("the error should include proposal responses", () -> {
            Throwable t = transactionInvocation.getError();
            assertThat(t).isInstanceOf(ContractException.class);

            Collection<ProposalResponse> proposalResponses = ((ContractException) t).getProposalResponses();
            assertThat(proposalResponses).isNotEmpty();
        });

        Then("a block event should be received", this::getBlockEvent);

        Then("a contract event with payload {string} should be received", this::getContractEvent);

        Then("the wallet should contain {int} identities", (Integer number) -> {
            Set<String> labels = wallet.list();
            assertThat(labels).hasSize(number);
        });

        Then("the wallet should contain an identity named {string}", (String label) -> {
            Set<String> labels = wallet.list();
            assertThat(labels).contains(label);
        });

        Then("I should be able to get an identity named {string} from the wallet", (String label) -> {
            Identity identity = wallet.get(label);
            Identity expected = newOrg1UserIdentity();
            assertThat(identity).isEqualTo(expected);
        });

        Then("I should not be able to get an identity named {string} from the wallet", (String label) -> {
            Identity identity = wallet.get(label);
            assertThat(identity).isNull();
        });
    }

    private void emptyWallet() throws IOException {
        for (String label : wallet.list()) {
            wallet.remove(label);
        }
    }

    private Checkpointer fileCheckpointer() throws IOException {
        closeCheckpointer();
        checkpointer = DefaultCheckpointers.file(checkpointFile);
        return checkpointer;
    }

    private void closeCheckpointer() throws IOException {
        if (checkpointer != null) {
            checkpointer.close();
        }
    }

    private static Path getNetworkConfigPath(String configType) {
        Path networkConfigDir = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway");
        switch (configType) {
            case "tls":
                return networkConfigDir.resolve("connection-tls.json");
            case "discovery":
                return networkConfigDir.resolve("connection-discovery.json");
            case "basic":
                return networkConfigDir.resolve("connection.json");
            case "client-only":
                return networkConfigDir.resolve("connection-client-only.json");
            default:
                throw new IllegalArgumentException("Unknown network configuration type: " + configType);
        }
    }

    private static JsonArray parseJsonArray(String jsonString) {
        try (JsonReader reader = createJsonReader(jsonString)) {
            return reader.readArray();
        }
    }

    private static JsonReader createJsonReader(String jsonString) {
        return Json.createReader(new StringReader(jsonString));
    }

    private static String[] newStringArray(JsonArray jsonArray) {
        return jsonArray.getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .toArray(String[]::new);
    }

    public static String newString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private ContractEvent getContractEvent(String expectedPayload) throws InterruptedException {
        List<String> payloads = new ArrayList<>();
        ContractEvent matchingEvent = removeFirstMatch(contractEvents, event -> {
            String eventPayload = event.getPayload().map(ScenarioSteps::newString).orElse("");
            payloads.add(eventPayload);
            return expectedPayload.equals(eventPayload);
        });
        assertThat(matchingEvent)
                .withFailMessage("No contract events with payload \"%s\": %s, network=%s, contract=%s, checkpointer=%s",
                        expectedPayload, payloads, network, contract, checkpointer)
                .isNotNull();
        return matchingEvent;
    }

    /**
     * Remove and return the first element matching the given predicate. All other elements remain on the queue.
     * @param queue A queue.
     * @param match Filter used to match queue elements.
     * @return The first matching element or null if no matches are found.
     * @throws InterruptedException If waiting for queue elements is interrupted.
     */
    private <T> T removeFirstMatch(BlockingQueue<T> queue, Predicate<? super T> match) throws InterruptedException {
        List<T> unmatchedElements = new ArrayList<>();
        T element;

        while ((element = queue.poll(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) != null) {
            if (match.test(element)) {
                break;
            }
            unmatchedElements.add(element);
        }

        queue.addAll(unmatchedElements); // Re-queue elements that didn't match
        return element;
    }

    private static String exec(List<String> commandArgs) throws IOException, InterruptedException {
        return exec(null, commandArgs);
    }

    private static String exec(Path dir, List<String> commandArgs) throws IOException, InterruptedException {
        return exec(dir, commandArgs.toArray(new String[0]));
    }

    private static String exec(String... commandArgs) throws IOException, InterruptedException {
        return exec(null, commandArgs);
    }

    private static String exec(Path dir, String... commandArgs) throws IOException, InterruptedException {
        String commandString = String.join(" ", commandArgs);
        System.err.println(commandString);
        StringBuilder sb = new StringBuilder();

        File dirFile = dir != null ? dir.toFile() : null;
        Process process = Runtime.getRuntime().exec(commandArgs, null, dirFile);
        int exitCode = process.waitFor();

        // get STDERR for the process and print it
        try (InputStream errorStream = process.getErrorStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
            for (String line; (line = reader.readLine()) != null; ) {
                System.err.println(line);
                sb.append(line);
            }
        }

        // get STDOUT for the process and print it
        try (InputStream inputStream = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            for (String line; (line = reader.readLine()) != null;) {
                System.out.println(line);
                sb.append(line);
            }
        }


        assertThat(exitCode)
                .withFailMessage("Failed to execute command: %s",commandString)
                .isEqualTo(0);
        return sb.toString();
    }

    static void startFabric(boolean tls) throws Exception {
        createCryptoMaterial();
        String dockerComposeFile = tls ? DOCKER_COMPOSE_TLS_FILE : DOCKER_COMPOSE_FILE;
        exec(DOCKER_COMPOSE_DIR, "docker-compose", "-f", dockerComposeFile, "-p", "node", "up", "-d");
        Thread.sleep(10000);
    }

    static void stopFabric(boolean tls) throws Exception {
        String dockerComposeFile = tls ? DOCKER_COMPOSE_TLS_FILE : DOCKER_COMPOSE_FILE;
        exec(DOCKER_COMPOSE_DIR, "docker-compose", "-f", dockerComposeFile, "-p", "node", "down");
    }

    private static void createCryptoMaterial() throws Exception {
        Path fixtures = Paths.get("src", "test", "fixtures");
        exec(fixtures, "./generate.sh");
    }

    private void populateWallet() throws IOException, CertificateException, InvalidKeyException {
        Identity identity = newOrg1UserIdentity();
        wallet.put("User1", identity);
    }

    private void prepareGateway(String tlsType) throws IOException {
        gatewayBuilder = Gateway.createBuilder();
        gatewayBuilder.networkConfig(getNetworkConfigPath(tlsType));
        gatewayBuilder.commitTimeout(1, TimeUnit.MINUTES);
        if (tlsType.equals("discovery")) {
            gatewayBuilder.discovery(true);
        }
    }

    private static Identity newOrg1UserIdentity() throws IOException, CertificateException, InvalidKeyException {
        Path credentialPath = Paths.get("src", "test", "fixtures", "crypto-material", "crypto-config",
                "peerOrganizations", "org1.example.com", "users", "User1@org1.example.com", "msp");

        Path certificatePath = credentialPath.resolve(Paths.get("signcerts", "User1@org1.example.com-cert.pem"));
        X509Certificate certificate = readX509Certificate(certificatePath);

        Path privateKeyPath = credentialPath.resolve(Paths.get("keystore", "key.pem"));
        PrivateKey privateKey = getPrivateKey(privateKeyPath);

        return Identities.newX509Identity("Org1MSP", certificate, privateKey);
    }

    private static X509Certificate readX509Certificate(final Path certificatePath) throws IOException, CertificateException {
        try (Reader certificateReader = Files.newBufferedReader(certificatePath, StandardCharsets.UTF_8)) {
            return Identities.readX509Certificate(certificateReader);
        }
    }

    private static PrivateKey getPrivateKey(final Path privateKeyPath) throws IOException, InvalidKeyException {
        try (Reader privateKeyReader = Files.newBufferedReader(privateKeyPath, StandardCharsets.UTF_8)) {
            return Identities.readPrivateKey(privateKeyReader);
        }
    }

    private BlockEvent getBlockEvent() throws InterruptedException {
        BlockEvent event = blockEvents.poll(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(event)
                .withFailMessage("No block events received: network=%s, contract=%s, checkpointer=%s",
                        network, contract, checkpointer)
                .isNotNull();
        return event;
    }

    private void clearBlockListener() {
        if (blockListener != null) {
            network.removeBlockListener(blockListener);
        }
        blockEvents.clear();
    }

    private void clearContractListener() {
        if (contractListener != null) {
            contract.removeContractListener(contractListener);
        }
        contractEvents.clear();
    }
}
