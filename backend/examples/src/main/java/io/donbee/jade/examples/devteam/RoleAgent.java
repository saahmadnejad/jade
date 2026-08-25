package io.donbee.jade.examples.devteam;

import java.nio.file.Path;

import io.donbee.jade.core.Agent;
import io.donbee.jade.domain.DFService;
import io.donbee.jade.domain.FIPAException;
import io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription;
import io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;
import io.donbee.llm.Brain;
import io.donbee.llm.HttpBrain;
import io.donbee.llm.LlmConfig;

/**
 * Base class for LLM-powered team members. Receives a task as an ACL
 * REQUEST, delegates it to its {@link Brain} and answers with INFORM +
 * generated text (or FAILURE on brain/provider errors).
 *
 * <p>Brain args (passed by {@link DevTeamScenario}, identical order for all
 * roles): baseUrl, model, proxyEnabled, proxyHost, proxyPort, callTimeoutSec,
 * keyEnvVar.</p>
 */
public abstract class RoleAgent extends Agent {

    protected Brain brain;

    /** Role suffix used in agent local names, e.g. {@code architect}. */
    protected abstract String role();

    /** Persona instructions sent as the system prompt. */
    protected abstract String systemPrompt();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        String baseUrl = str(args, 0, "https://openrouter.ai/api/v1");
        String model = str(args, 1, "thinkingmachines/inkling-small:free");
        boolean proxyEnabled = Boolean.parseBoolean(str(args, 2, "false"));
        String proxyHost = str(args, 3, null);
        int proxyPort = intArg(args, 4, 1080);
        int timeoutSec = intArg(args, 5, 120);
        String keyEnvVar = str(args, 6, "OPENROUTER_API_KEY");

        try {
            String apiKey = SecretsResolver.resolveApiKey(System::getenv, keyEnvVar, Path.of(""));
            LlmConfig.Builder builder = LlmConfig.builder(baseUrl, model)
                .apiKey(() -> apiKey)
                .timeoutMs(timeoutSec * 1000);
            if (proxyEnabled && proxyHost != null && !proxyHost.isBlank()) {
                builder.socksProxy(proxyHost, proxyPort);
            }
            brain = new HttpBrain(builder.build());
        } catch (IllegalStateException e) {
            System.err.println("[" + roleName() + "] " + e.getMessage());
            doDelete();
            return;
        } catch (Exception e) {
            System.err.println("[" + roleName() + "] brain init failed: " + e.getMessage());
            doDelete();
            return;
        }

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("devteam-role");
            sd.setName(role());
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println("[" + roleName() + "] DF registration failed: " + e.getMessage());
        }

        System.out.println("[" + roleName() + "] ready (model: " + brain.model() + ")");

        addBehaviour(new io.donbee.jade.core.behaviours.CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage task = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (task == null) {
                    block();
                    return;
                }
                System.out.println("[" + roleName() + "] thinking about: "
                    + firstLine(task.getContent()));
                ACLMessage reply = task.createReply();
                try {
                    String result = brain.respond(systemPrompt(), task.getContent());
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(result);
                } catch (Exception e) {
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("(" + roleName() + "-failed " + sanitize(e.getMessage()) + ")");
                    System.err.println("[" + roleName() + "] brain call failed: " + e.getMessage());
                }
                myAgent.send(reply);
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {
            // Already gone
        }
    }

    String roleName() {
        return getLocalName();
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
