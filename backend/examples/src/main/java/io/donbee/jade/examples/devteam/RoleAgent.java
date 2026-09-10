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
import io.donbee.llm.RetryingBrain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Base class for LLM-powered team members. Receives tasks as ACL REQUEST,
 * delegates to {@link Brain}, and replies with INFORM or FAILURE.
 *
 * <p>Supports decentralized peer-to-peer communication: agents can discover
 * each other via DF and send direct messages, bypassing the Manager hub.</p>
 *
 * <p>Brain args (passed by {@link DevTeamScenario}, identical order for all
 * roles): model, fallbackModel, callTimeoutSec, workingDir. The brain is an
 * {@link CliBrain} shelling the opencode CLI with the role's persona
 * ({@code --agent <role>}); each brain is wrapped in
 * {@link RetryingBrain} and pairs are chained via {@link FallbackBrain}
 * (see ADR-0003).</p>
 */
public abstract class RoleAgent extends Agent {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(RoleAgent.class.getName());

    /** Default opencode model id (provider/model) when the arg is missing. */
    static final String DEFAULT_MODEL = "tokenrouter/z-ai/glm-5.3-free";

    protected Brain brain;
    private String githubToken;
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
        try {
            doSetup();
        } catch (Throwable t) {
            // Any setup failure must be visible in logs before the agent dies;
            // JADE's own "died without being properly terminated" hides the cause.
            LOG.log(Level.SEVERE, "role=" + roleName()
                + ": setup failed: " + t, t);
            doDelete();
        }
    }

    private void doSetup() {
        Object[] args = getArguments();
        teamId = extractTeamId(getLocalName());

        String model = str(args, 0, DEFAULT_MODEL);
        String fallbackModel = str(args, 1, null);
        timeoutSec = intArg(args, 2, 600);
        workDir = str(args, 3, null);

        githubToken = resolveGithubTokenQuietly();

        try {
            Brain primary = buildBrain(model);
            if (fallbackModel != null && !fallbackModel.isBlank()
                && !fallbackModel.equalsIgnoreCase(model)) {
                Brain fallback = buildBrain(fallbackModel);
                brain = new FallbackBrain(List.of(primary, fallback));
                LOG.info("role=" + roleName() + ": brain: " + model
                    + " (fallback: " + fallbackModel + ")");
            } else {
                brain = primary;
                LOG.info("role=" + roleName() + ": brain: " + model);
            }
        } catch (IllegalStateException e) {
            LOG.severe("role=" + roleName() + ": " + e.getMessage());
            doDelete();
            return;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "role=" + roleName() + ": brain init failed", e);
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
            onTaskCompleted(result);
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("(" + roleName() + "-failed " + sanitize(e.getMessage()) + ")");
            LOG.log(Level.WARNING,
                "role=" + roleName() + ": brain call failed", e);
        }
        send(reply);
    }

    /**
     * Hook after a successful brain call: subclasses forward results to peers
     * (P2P INFORM). Default: nothing.
     */
    protected void onTaskCompleted(String result) {
        // no-op
    }

    /** Called when a peer sends us an INFORM message (direct P2P comms). */
    protected void onPeerMessage(ACLMessage msg) {
        // Default: ignore peer messages. Override in subclasses if needed.
    }

    /**
     * Invoke the brain and log the exact model response (bracketed for
     * parseability) and save it to the team workspace file.
     */
    protected String callBrain(String systemPrompt, String userPrompt, String conversationId) {
        // Workspace context: the CLI agent runs inside the team workspace
        // directory; all paths resolve there. Respond in English.
        String bashCtx = "You are an agent working inside the team workspace "
            + "directory '" + workDir + "' (relative paths resolve there; "
            + "never use or create absolute paths like /workspace). "
            + "Always respond in English; all code, comments, docs and file "
            + "contents must be in English.";
        String sys = (systemPrompt == null || systemPrompt.isBlank())
            ? bashCtx : systemPrompt + "\n\n" + bashCtx;
        String result = brain.respond(sys, userPrompt);
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
            LOG.log(Level.SEVERE, "role=" + roleName() + ": DF registration failed", e);
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

    private Brain buildBrain(String model) {
        Duration timeout = Duration.ofSeconds(timeoutSec);
        Path brainWorkDir = workDir != null && !workDir.isBlank()
            ? Path.of(workDir.trim())
            : Path.of(System.getProperty("java.io.tmpdir"), "jade-devteam-" + teamId);
        return new RetryingBrain(
            new CliBrain(model, role(), brainWorkDir, timeout, null));
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