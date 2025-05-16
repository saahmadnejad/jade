package ir.donbee.jade.domain.introspection;

import ir.donbee.jade.core.Agent;
import ir.donbee.jade.core.behaviours.CyclicBehaviour;
import ir.donbee.jade.lang.acl.*;
import ir.donbee.jade.domain.FIPANames;
import ir.donbee.jade.content.ContentManager;
import ir.donbee.jade.content.onto.Ontology;
import ir.donbee.jade.content.onto.OntologyException;
import ir.donbee.jade.content.onto.basic.*;
import ir.donbee.jade.content.lang.Codec;
import ir.donbee.jade.content.lang.Codec.CodecException;
import ir.donbee.jade.content.lang.sl.SLCodec;
import ir.donbee.jade.util.leap.List;
import ir.donbee.jade.util.leap.ArrayList;

import java.lang.reflect.*;
import java.io.Serializable;

public class IntrospectionServer extends CyclicBehaviour {
	private Codec codec;
	private Ontology onto;
	private MessageTemplate template;
	
	private static Class serializableClass;
	
	static {
		try {
			serializableClass = Class.forName("java.io.Serializable");
		}
		catch (Exception e) {
		}
	}
	
	public IntrospectionServer(Agent a) {
		super(a);
	}
	
	public void onStart() {
		ContentManager cm = myAgent.getContentManager();
		
		onto = IntrospectionOntology.getInstance();
		cm.registerOntology(onto);
		
		codec = cm.lookupLanguage(FIPANames.ContentLanguage.FIPA_SL);
		if (codec == null) {
			codec = cm.lookupLanguage(FIPANames.ContentLanguage.FIPA_SL2);
			if (codec == null) {
				codec = cm.lookupLanguage(FIPANames.ContentLanguage.FIPA_SL1);
				if (codec == null) {
					codec = cm.lookupLanguage(FIPANames.ContentLanguage.FIPA_SL0);
				}
			}
		}
		if (codec == null) {
			codec = new SLCodec();
			cm.registerLanguage(codec);
		}
		
		template = MessageTemplate.and(
				MessageTemplate.MatchOntology(onto.getName()),
				MessageTemplate.MatchPerformative(ir.donbee.jade.lang.acl.ACLMessage.REQUEST) );
	}
	
	public void action() {
		ir.donbee.jade.lang.acl.ACLMessage request = myAgent.receive(template);
		if (request != null) {
			try {
				ContentManager cm = myAgent.getContentManager();
				Action actionExpr = (Action) cm.extractContent(request);
				Object act = actionExpr.getAction();
				if (act instanceof GetKeys) {
					serveGetKeys(request, actionExpr, (GetKeys) act);
				}
				else if (act instanceof GetValue) {
					serveGetValue(request, actionExpr, (GetValue) act);
				}
				else {
					serveUnknownAction(request, actionExpr, act);
				}
			}
			catch (OntologyException oe) {
				reply(request, ir.donbee.jade.lang.acl.ACLMessage.NOT_UNDERSTOOD);
				oe.printStackTrace();
			}
			catch (CodecException ce) {
				reply(request, ir.donbee.jade.lang.acl.ACLMessage.NOT_UNDERSTOOD);
				ce.printStackTrace();
			}
			catch (ValueEncodingException vee) {
				ir.donbee.jade.lang.acl.ACLMessage msg = request.createReply();
				msg.setPerformative(ir.donbee.jade.lang.acl.ACLMessage.FAILURE);
				msg.setContent("VALUE_NOT_ENCODABLE");
				myAgent.send(msg);
			}
			catch (Throwable t) {
				reply(request, ir.donbee.jade.lang.acl.ACLMessage.FAILURE);
				t.printStackTrace();
			}
		}
		else {
			block();
		}
	}
	
	protected void reply(ir.donbee.jade.lang.acl.ACLMessage request, int performative) {
		ir.donbee.jade.lang.acl.ACLMessage msg = request.createReply();
		msg.setPerformative(performative);
		myAgent.send(msg);
	}
	
	protected void serveGetKeys(ir.donbee.jade.lang.acl.ACLMessage request, Action aExpr, GetKeys action) throws Exception {
		List keys = new ArrayList();
		Method[] mm = myAgent.getClass().getMethods();
		for (int i = 0; i < mm.length; ++i) {
			Method method = mm[i];
			if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
				Class retType = method.getReturnType();
				if (retType.isPrimitive() || (serializableClass != null && serializableClass.isAssignableFrom(retType))) {
					String key = method.getName().substring(3);
					keys.add(key);
				}
			}
		}
		Result r = new Result(aExpr, keys);
		ir.donbee.jade.lang.acl.ACLMessage reply = request.createReply();
		myAgent.getContentManager().fillContent(reply, r);
		reply.setPerformative(ir.donbee.jade.lang.acl.ACLMessage.INFORM);
		myAgent.send(reply);
	}
	
	protected void serveGetValue(ir.donbee.jade.lang.acl.ACLMessage request, Action aExpr, GetValue action) throws Exception {
		Method method = myAgent.getClass().getMethod("get"+action.getKey(), new Class[]{});
		Object value = method.invoke(myAgent, (Object[]) null);	
		if (value == null) {
			value = "null";
		}
		Result r = new Result(aExpr, value);
		ir.donbee.jade.lang.acl.ACLMessage reply = request.createReply();
		try {
			myAgent.getContentManager().fillContent(reply, r);
			reply.setPerformative(ir.donbee.jade.lang.acl.ACLMessage.INFORM);
			myAgent.send(reply);
		}
		catch (OntologyException oe) {
			throw new ValueEncodingException();
		}
		catch (CodecException ce) {
			throw new ValueEncodingException();
		}
	}
	
	protected void serveUnknownAction(ir.donbee.jade.lang.acl.ACLMessage request, Action aExpr, Object action) {
		reply(request, ir.donbee.jade.lang.acl.ACLMessage.REFUSE);
	}
	
	private class ValueEncodingException extends Exception {	
	}
}
