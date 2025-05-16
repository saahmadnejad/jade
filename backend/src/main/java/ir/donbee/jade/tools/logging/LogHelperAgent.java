/*****************************************************************
 JADE - Java Agent DEvelopment Framework is a framework to develop 
 multi-agent systems in compliance with the FIPA specifications.
 Copyright (C) 2000 CSELT S.p.A. 
 
 GNU Lesser General Public License
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation, 
 version 2.1 of the License. 
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA.
 *****************************************************************/
package ir.donbee.jade.tools.logging;

import java.util.Map;

import ir.donbee.jade.content.AgentAction;
import ir.donbee.jade.content.lang.Codec;
import ir.donbee.jade.content.lang.sl.SLCodec;
import ir.donbee.jade.content.onto.basic.Action;
import ir.donbee.jade.content.onto.basic.Result;
import ir.donbee.jade.core.AID;
import ir.donbee.jade.core.Agent;
import ir.donbee.jade.domain.FIPANames;
import ir.donbee.jade.domain.FIPAAgentManagement.FailureException;
import ir.donbee.jade.domain.FIPAAgentManagement.NotUnderstoodException;
import ir.donbee.jade.domain.FIPAAgentManagement.RefuseException;
import ir.donbee.jade.domain.introspection.AMSSubscriber;
import ir.donbee.jade.domain.introspection.Event;
import ir.donbee.jade.domain.introspection.IntrospectionVocabulary;
import ir.donbee.jade.domain.introspection.DeadAgent;
import ir.donbee.jade.domain.introspection.AMSSubscriber.EventHandler;
import ir.donbee.jade.lang.acl.ACLMessage;
import ir.donbee.jade.lang.acl.MessageTemplate;
import ir.donbee.jade.proto.SimpleAchieveREResponder;
import ir.donbee.jade.tools.logging.ontology.LogManagementOntology;
import ir.donbee.jade.tools.logging.ontology.GetAllLoggers;
import ir.donbee.jade.tools.logging.ontology.SetLevel;
import ir.donbee.jade.tools.logging.ontology.SetFile;
import ir.donbee.jade.util.Logger;
import ir.donbee.jade.util.leap.List;

/**
 * @author 00917820
 * @version $Date:  $ $Revision: $
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LogHelperAgent extends Agent {
	
	private String logManagerClass = JavaLoggingLogManagerImpl.JAVA_LOGGING_LOG_MANAGER_CLASS; //default logManagerClass if not specified.
	private Logger logger;
	private Codec codec = new SLCodec();
	private LogManager logManager = null;
	private AMSSubscriber myAMSSubscriber = null;
	
	protected void setup(){
		
		logger = Logger.getMyLogger(getLocalName());
		
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			// Get the Log Manager Agent AID as argument
			if (args[0] instanceof AID) {
				amsSubscribe((AID) args[0]);
			}
		}
		// Register languages and ontologies
		getContentManager().registerLanguage(codec);	
		getContentManager().registerOntology(LogManagementOntology.getInstance());
		
		MessageTemplate mt = MessageTemplate.MatchOntology(LogManagementOntology.NAME);
		addBehaviour(new LogHelperAgentBehaviour(this, mt));
		logger.log(Logger.INFO, getName() + " started using " + logManagerClass );
	}
	
	protected void takeDown() {
		if (myAMSSubscriber != null) {
			send(myAMSSubscriber.getCancel());
		}
	}
	
	
	/**
	 * Inner class LogHelperAgentBehaviour
	 */
	private class LogHelperAgentBehaviour extends SimpleAchieveREResponder{
		
		private Action actExpr;
		private AgentAction action;
		
		public LogHelperAgentBehaviour(Agent a, MessageTemplate mt) {
			super(a, mt);
		}
		
		protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
			try {
				actExpr =(Action)getContentManager().extractContent(request);
				action = (AgentAction) actExpr.getAction();
				return null;
			}
			catch(Exception e){
				String errorMsg = "Error decoding request. "+e;
				logger.log(Logger.WARNING, errorMsg);
				throw new NotUnderstoodException(errorMsg);
			}
		}
		
	    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
	    	ACLMessage reply = request.createReply();
			if (action instanceof GetAllLoggers){
				handleGetAllLoggers((GetAllLoggers)action, actExpr, reply);
			}
			else if(action instanceof SetFile){
				handleSetFile((SetFile)action, actExpr, reply);
			}
			else if (action instanceof SetLevel){
				handleSetLevel((SetLevel)action, actExpr, reply);
			}
			else {
				throw new FailureException("Action "+action.getClass().getName()+" not supported.");		
			}
			return reply;
	    }
	    
	    
		private void handleGetAllLoggers(GetAllLoggers action, Action actExpr, ACLMessage reply) throws FailureException{
			try {
				String className = action.getType();
				if (className != null)
					logManagerClass = className;
				
				logger.log(Logger.CONFIG, "Log manager class defined: " + logManagerClass);
				logManager = (LogManager) Class.forName(logManagerClass).newInstance();
				
				List logInfo = logManager.getAllLogInfo();
				Result r = new Result(actExpr, logInfo);
				getContentManager().fillContent(reply, r);
				reply.setPerformative(ACLMessage.INFORM);
			} catch (Exception any) {
				String errorMsg = "Error initializing LogManager. "+any;
				logger.log(Logger.WARNING, errorMsg);
				any.printStackTrace();
				throw new FailureException(errorMsg);
			}
		}
		
		//FIXME: risolvere il problema del root logger quello senza nome !!!!!
		private void handleSetFile(SetFile action, Action actExpr, ACLMessage reply) throws FailureException{
			if(logManager != null){
				logManager.setFile(action.getFile(), action.getLogger());
			}else{
				throw new FailureException("LogManager not initialized");
			}
			reply.setPerformative(ACLMessage.INFORM);
		}
		
		private void handleSetLevel(SetLevel action, Action actExpr, ACLMessage reply)throws FailureException{
			if(logManager != null){
				logManager.setLogLevel(action.getLogger(), action.getLevel());
			}else{
				throw new FailureException("LogManager not initialized");
			}
			reply.setPerformative(ACLMessage.INFORM);
		}	
	} // END of inner class LogHelperAgentBehaviour
	
	
	private void amsSubscribe(final AID owner) {
		myAMSSubscriber = new AMSSubscriber() {
			protected void installHandlers(Map handlersTable) {
				handlersTable.put(IntrospectionVocabulary.DEADAGENT, new EventHandler() {
					public void handle(Event ev) {
						DeadAgent da = (DeadAgent) ev;
						if (da.getAgent().equals(owner)) {
							// My Owner is dead --> suicide
							doDelete();
						}
					}
				});
			}
		};
		
		addBehaviour(myAMSSubscriber);		
	}
}
