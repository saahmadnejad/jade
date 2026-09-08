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
import io.donbee.llm.BashTool;
import io.donbee.llm.Brain;
import io.donbee.llm.FallbackBrain;
import io.donbee.llm.LangChain4jBrain;

import java.nio.file.Path;
import java.time.Duration;
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
 * roles): baseUrl, model, fallbackModel, proxyEnabled, proxyHost,
 * proxyPort, callTimeoutSec, workingDir.</p>
 */
public abstract class RoleAgent extends Agent {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(RoleAgent.class.getName());

    protected Brain brain;
    private String githubToken;
    private String llmApiKey;
    private String teamId;
    private int callCount = 0;
    protected String workDir;
    protected int timeoutSec;

    /** Role suffix used in agent local names, e.g. {@code architect}. */
    protected abstract String role();

    /** Persona instructions sent as the system prompt. */
    protected abstract String systemPrompt();

    /** DF service types for peer discovery. */
    protected static final String DF_ROLE_SERVICE_TYPE = "devteam-role";

    /**
     * Protocol for peer-to-peer INFORM messages. The value must be the exact
     * reserved token defined by the FIPA Interaction Protocol Library
     * ({@code fipa-request}); hand-written variants such as
     * {@code "FIPA_REQUEST"} do not match the reserved value and are
     * non-conformant on the wire.
     */
    static final String PEER_PROTOCOL =
        FIPANames.InteractionProtocol.FIPA_REQUEST;

    /** Conversation-id prefix for peer-to-peer messages. */
    static final String PEER_CONVERSATION_PREFIX = "dt-peer-";

    @Override
    protected void setup() {
        Object[] args = getArguments();
        teamId = extractTeamId(getLocalName());

        String baseUrl = str(args, 0, "http://9router:20128/v1");
        String model = str(args, 1, "oc/laguna-s-2.1-free");
        String fallbackModel = str(args, 2, null);
        boolean proxyEnabled = Boolean.parseBoolean(str(args, 3, "false"));
        String proxyHost = str(args, 4, null);
        int proxyPort = intArg(args, 5, 1080);
        timeoutSec = intArg(args, 6, 300);
        workDir = str(args, 7, null);

        githubToken = resolveGithubTokenQuietly();
        llmApiKey = resolveApiKeyQuietly();

        try {
            Brain primary = buildBrain(baseUrl, model, proxyEnabled, proxyHost,
                proxyPort, timeoutSec);
            if (fallbackModel != null && !fallbackModel.isBlank()
                && !fallbackModel.equalsIgnoreCase(model)) {
                Brain fallback = buildBrain(baseUrl, fallbackModel, false,
                    null, 0, timeoutSec);
                brain = new FallbackBrain(List.of(primary, fallback));
                LOG.info("role=" + roleName() + ": brain: " + model
                    + " (fallback: " + fallbackModel + ")");
            } else {
                brain = primary;
                LOG.info("role=" + roleName() + ": brain: " + model);
            }
        } catch (IllegalStateException e) {
            LOG.warning("role=" + roleName() + ": " + e.getMessage());
            doDelete();
            return;
        } catch (Exception e) {
            LOG.warning("role=" + roleName() + ": brain init failed: " + e.getMessage());
            doDelete();
            return;
        }

        registerInDF();

        LOG.info("role=" + roleName() + ": ready (model: " + brain.model() + ")");

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
        LOG.info("role=" + roleName() + ": thinking about: "
            + firstLine(request.getContent()));
        ACLMessage reply = request.createReply();
        try {
            String result = callBrain(systemPrompt(), request.getContent(), request.getConversationId());
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(result);
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("(" + roleName() + "-failed " + sanitize(e.getMessage()) + ")");
            LOG.warning("role=" + roleName() + ": brain call failed: " + e.getMessage());
        }
        send(reply);
    }

    /** Called when a peer sends us an INFORM message (direct P2P comms). */
    protected void onPeerMessage(ACLMessage msg) {
        // Default: ignore peer messages. Override in subclasses if needed.
    }

    /**
     * Invoke the brain and log the exact model response to stdout (bracketed
     * for parseability) and to the team workspace file.
     */
    protected String callBrain(String systemPrompt, String userPrompt, String conversationId) {
        List<Brain.Tool> tools = createTools();
        // Bash tool context: models otherwise invent absolute paths (/workspace, ...)
        String bashCtx = "You have a bash tool. All commands run in the team "
            + "workspace directory '" + workDir + "' (relative paths resolve "
            + "there; never use or create absolute paths like /workspace). "
            + "Write files, run tests and git inside it via the bash tool.";
        String sys = (systemPrompt == null || systemPrompt.isBlank())
            ? bashCtx : systemPrompt + "\n\n" + bashCtx;
        String result = brain.respond(sys, userPrompt, tools);
        callCount++;
        String conv = conversationId != null && !conversationId.isBlank() ? conversationId : "turn-" + callCount;
        LOG.info("role=" + roleName() + ": [model-response-start conv=" + conv + " turn=" + callCount + "]");
        LOG.info(result);
        LOG.info("role=" + roleName() + ": [model-response-end conv=" + conv + "]");
        Workspace ws = WorkspaceStore.get(teamId);
        if (ws != null) {
            ws.save("logs/" + role() + "-" + conv + ".md", result);
        }
        return result;
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
            LOG.warning("role=" + roleName() + ": DF registration failed: " + e.getMessage());
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
            LOG.warning("role=" + roleName() + ": DF lookup failed for " + roleSuffix + ": " + e.getMessage());
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
            msg.setProtocol(PEER_PROTOCOL);
            msg.setConversationId(PEER_CONVERSATION_PREFIX + getLocalName());
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

    private Brain buildBrain(String baseUrl, String model,
                             boolean proxyEnabled, String proxyHost, int proxyPort,
                             int timeoutSec) {
        Duration timeout = Duration.ofSeconds(timeoutSec);
        String proxyH = proxyEnabled ? proxyHost : null;
        int proxyP = proxyEnabled ? proxyPort : 0;
        Path brainWorkDir = workDir != null && !workDir.isBlank()
            ? Path.of(workDir.trim())
            : null;
        return LangChain4jBrain.build(baseUrl, model,
            () -> llmApiKey, timeout, proxyH, proxyP, brainWorkDir);
    }

    private List<Brain.Tool> createTools() {
        Path toolWorkDir = workDir != null && !workDir.isBlank()
            ? Path.of(workDir.trim())
            : Path.of(System.getProperty("java.io.tmpdir"), "jade-devteam-" + teamId);
        // Cap each bash invocation below the whole-call LLM timeout so one
        // runaway command cannot eat the entire agent-loop budget.
        Duration bashTimeout = Duration.ofSeconds(Math.min(timeoutSec, 120));
        return List.of(new BashTool(toolWorkDir, bashTimeout));
    }

    private String resolveGithubTokenQuietly() {
        try {
            return SecretsResolver.resolveGithubToken(System::getenv, Path.of(""));
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private String resolveApiKeyQuietly() {
        try {
            return SecretsResolver.resolveApiKey(System::getenv, Path.of(""));
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