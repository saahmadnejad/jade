package io.donbee.jade.examples.devteam;

import io.donbee.jade.core.Agent;
import io.donbee.jade.core.AID;
import io.donbee.jade.domain.DFService;
import io.donbee.jade.domain.FIPAException;
import io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription;
import io.donbee.jade.domain.FIPAAgentManagement.Property;
import io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription;
import io.donbee.jade.domain.FIPANames;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;
import io.donbee.llm.Brain;
import io.donbee.llm.CliBrain;
import io.donbee.llm.FallbackBrain;
import io.donbee.llm.HttpBrain;
import io.donbee.llm.LlmConfig;

import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * Base class for LLM-powered team members. Receives tasks as ACL REQUEST,
 * delegates to {@link Brain}, and replies with INFORM or FAILURE.
 *
 * <p>Supports decentralized peer-to-peer communication: agents can discover
 * each other via DF and send direct messages, bypassing the Manager hub.</p>
 *
 * <p>Brain args (passed by {@link DevTeamScenario}, identical order for all
 * roles): brainType, baseUrl, model, fallbackModel, proxyEnabled, proxyHost,
 * proxyPort, callTimeoutSec, keyEnvVar, cliCommand, workingDir.</p>
 */
public abstract class RoleAgent extends Agent {

    protected Brain brain;
    private String githubToken;
    private String teamId;

    /** Role suffix used in agent local names, e.g. {@code architect}. */
    protected abstract String role();

    /** Persona instructions sent as the system prompt. */
    protected abstract String systemPrompt();

    /** DF service types for peer discovery. */
    protected static final String DF_ROLE_SERVICE_TYPE = "devteam-role";

    @Override
    protected void setup() {
        Object[] args = getArguments();
        teamId = extractTeamId(getLocalName());

        String brainType = str(args, 0, "http");
        String baseUrl = str(args, 1, "https://openrouter.ai/api/v1");
        String model = str(args, 2, "thinkingmachines/inkling-small:free");
        String fallbackModel = str(args, 3, null);
        boolean proxyEnabled = Boolean.parseBoolean(str(args, 4, "false"));
        String proxyHost = str(args, 5, null);
        int proxyPort = intArg(args, 6, 1080);
        int timeoutSec = intArg(args, 7, 300);
        String keyEnvVar = str(args, 8, "OPENROUTER_API_KEY");
        String cliCommand = str(args, 9, "opencode run");
        String workingDir = str(args, 10, null);

        try {
            Brain primary = buildBrain(brainType, baseUrl, model, proxyEnabled, proxyHost,
                proxyPort, timeoutSec, keyEnvVar, cliCommand, workingDir);
            if (fallbackModel != null && !fallbackModel.isBlank()
                && !fallbackModel.equalsIgnoreCase(model)) {
                Brain fallback = buildBrain(brainType, baseUrl, fallbackModel, false,
                    null, 0, timeoutSec, keyEnvVar, cliCommand, workingDir);
                brain = new FallbackBrain(List.of(primary, fallback));
                System.out.println("[" + roleName() + "] brain: " + model
                    + " (fallback: " + fallbackModel + ")");
            } else {
                brain = primary;
                System.out.println("[" + roleName() + "] brain: " + model);
            }
        } catch (IllegalStateException e) {
            System.err.println("[" + roleName() + "] " + e.getMessage());
            doDelete();
            return;
        } catch (Exception e) {
            System.err.println("[" + roleName() + "] brain init failed: " + e.getMessage());
            doDelete();
            return;
        }
        githubToken = resolveGithubTokenQuietly();

        registerInDF();

        System.out.println("[" + roleName() + "] ready (model: " + brain.model() + ")");

        addBehaviour(new io.donbee.jade.core.behaviours.CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(
                    MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
                if (msg == null) {
                    block();
                    return;
                }
                handleIncomingMessage(msg);
            }
        });
    }

    /**
     * Handle incoming REQUEST or INFORM messages. Subclasses can override to
     * customize behavior for direct peer communication.
     */
    protected void handleIncomingMessage(ACLMessage msg) {
        if (msg.getPerformative() == ACLMessage.REQUEST) {
            handleTask(msg);
        } else {
            // INFORM - peer-to-peer communication
            onPeerMessage(msg);
        }
    }

    /** Subclasses implement task handling (LLM delegation + response). */
    protected void handleTask(ACLMessage request) {
        System.out.println("[" + roleName() + "] thinking about: "
            + firstLine(request.getContent()));
        ACLMessage reply = request.createReply();
        try {
            String result = brain.respond(systemPrompt(), request.getContent());
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(result);
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("(" + roleName() + "-failed " + sanitize(e.getMessage()) + ")");
            System.err.println("[" + roleName() + "] brain call failed: " + e.getMessage());
        }
        send(reply);
    }

    /** Called when a peer sends us an INFORM message (direct P2P comms). */
    protected void onPeerMessage(ACLMessage msg) {
        // Default: ignore peer messages. Override in subclasses if needed.
    }

    private void registerInDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(DF_ROLE_SERVICE_TYPE);
            sd.setName(role());
            sd.addProperties(new Property("team", teamId));
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println("[" + roleName() + "] DF registration failed: " + e.getMessage());
        }
    }

    protected AID findPeer(String roleSuffix) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(DF_ROLE_SERVICE_TYPE);
            sd.setName(roleSuffix);
            sd.addProperties(new Property("team", teamId));
            template.addServices(sd);
            DFAgentDescription[] results = DFService.search(this, null, template, null);
            if (results.length > 0) {
                return results[0].getName();
            }
        } catch (FIPAException e) {
            System.err.println("[" + roleName() + "] DF lookup failed for " + roleSuffix + ": " + e.getMessage());
        }
        return null;
    }

    /** Find all roles on this team (including self). */
    protected Set<String> findTeamRoles(String peerRole) {
        Set<String> roles = new java.util.HashSet<>();
        roles.add("architect");
        roles.add("implementer");
        roles.add("tester");
        roles.add("reviewer");
        roles.remove(peerRole);
        return roles;
    }

    /** Notify a peer agent directly with an INFORM message. */
    protected void notifyPeer(String roleSuffix, String content) {
        AID peer = findPeer(roleSuffix);
        if (peer != null) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(peer);
            msg.setProtocol("FIPA_REQUEST");
            msg.setConversationId("dt-peer-" + getLocalName());
            msg.setContent(content);
            send(msg);
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {}
    }

    private Brain buildBrain(String brainType, String baseUrl, String model,
                             boolean proxyEnabled, String proxyHost, int proxyPort,
                             int timeoutSec, String keyEnvVar, String cliCommand,
                             String workingDir) {
        if ("cli".equalsIgnoreCase(brainType)) {
            Path dir = workingDir != null && !workingDir.isBlank()
                ? Path.of(workingDir.trim()) : null;
            return new CliBrain(List.of(cliCommand.trim().split("\\s+")),
                model, role(), dir, timeoutSec * 1000,
                githubToken != null ? Map.of("GH_TOKEN", githubToken) : null);
        }
        String apiKey = SecretsResolver.resolveApiKey(System::getenv, keyEnvVar, Path.of(""));
        LlmConfig.Builder builder = LlmConfig.builder(baseUrl, model)
            .apiKey(() -> apiKey)
            .timeoutMs(timeoutSec * 1000);
        if (proxyEnabled && proxyHost != null && !proxyHost.isBlank()) {
            builder.socksProxy(proxyHost, proxyPort);
        }
        return new HttpBrain(builder.build());
    }

    private String resolveGithubTokenQuietly() {
        try {
            return SecretsResolver.resolveGithubToken(System::getenv, Path.of(""));
        } catch (IllegalStateException e) {
            return null;
        }
    }

    String roleName() {
        return getLocalName();
    }

    private String extractTeamId(String localName) {
        String r = role();
        String suffix = "-" + r;
        return localName.endsWith(suffix) ? localName.substring(0, localName.length() - suffix.length()) : localName;
    }

    static String firstLine(String s) {
        if (s == null) return "";
        int nl = s.indexOf('\n');
        String line = nl > 0 ? s.substring(0, nl) : s;
        return line.length() > 80 ? line.substring(0, 80) + "..." : line;
    }

    static String sanitize(String s) {
        return s == null ? "error" : s.replaceAll("[\\s()]+", " ").trim();
    }

    static String str(Object[] args, int i, String def) {
        return args != null && args.length > i && args[i] != null ? args[i].toString() : def;
    }

    static int intArg(Object[] args, int i, int def) {
        try {
            return Integer.parseInt(str(args, i, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}