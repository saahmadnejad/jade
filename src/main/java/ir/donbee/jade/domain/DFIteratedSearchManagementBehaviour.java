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

package ir.donbee.jade.domain;

//#MIDP_EXCLUDE_FILE

import ir.donbee.jade.core.behaviours.CyclicBehaviour;
import ir.donbee.jade.content.Concept;
import ir.donbee.jade.content.Predicate;
import ir.donbee.jade.content.onto.basic.Action;
import ir.donbee.jade.content.onto.basic.Done;
import ir.donbee.jade.content.onto.basic.Result;
import ir.donbee.jade.content.onto.OntologyException;
import ir.donbee.jade.content.lang.Codec.CodecException;
import ir.donbee.jade.lang.acl.ACLMessage;
import ir.donbee.jade.lang.acl.MessageTemplate;
import ir.donbee.jade.lang.acl.ConversationList;
import ir.donbee.jade.domain.FIPAAgentManagement.*;
import ir.donbee.jade.domain.KBManagement.KBIterator;
import ir.donbee.jade.proto.SSIteratedAchieveREResponder;
import ir.donbee.jade.util.leap.List;
import ir.donbee.jade.util.leap.ArrayList;
import ir.donbee.jade.util.Logger;

/**
 This behaviour serves search requests according to the iterated-fipa-request
 protocol. 
 @author Giovanni Caire - TILAB
 */
class DFIteratedSearchManagementBehaviour extends CyclicBehaviour {
	
	private df theDF;
	private MessageTemplate template;
	private ConversationList conversations;
	
	private Logger logger; 
	
	public DFIteratedSearchManagementBehaviour(df theDF, MessageTemplate mt) {
		super(theDF);
		this.theDF = theDF;
		conversations = new ConversationList(theDF);
		template = MessageTemplate.and(mt, conversations.getMessageTemplate());
		logger = Logger.getMyLogger(theDF.getLocalName());
	}
	
	public void action() {
		ACLMessage msg = theDF.receive(template);
		if (msg != null) {
			if (logger.isLoggable(Logger.FINE)) {
				logger.log(Logger.FINE, "DF "+myAgent.getName()+": Iterated-search request received from "+msg.getSender().getName()+". Conv-ID = "+msg.getConversationId());
			}
			theDF.addBehaviour(new SSIteratedAchieveREResponder(theDF, msg) {
				private KBIterator iterator = null;
				private String myConversationId = null;
				private int cnt = 0; // Only for debugging purposes
				
				protected ACLMessage handleRequest(ACLMessage request) throws RefuseException, FailureException, NotUnderstoodException {
					ACLMessage reply = null;
					
					if (logger.isLoggable(Logger.FINE)) {
						logger.log(Logger.FINE, "DF "+myAgent.getName()+": Iterated-search "+request.getConversationId()+". Serving request # "+cnt);
					}
					cnt++;
					try {
						// Parse the request content
						Action aExpr = (Action) theDF.getContentManager().extractContent(request);
						Search search = (Search) aExpr.getAction();
						SearchConstraints constraints = search.getConstraints();
						int maxResult = theDF.getActualMaxResults(constraints);
						
						// If this is the first REQUEST, create the Iterator
						if (iterator == null) {
							if (logger.isLoggable(Logger.FINER)) {
								logger.log(Logger.FINER, "DF "+myAgent.getName()+": Iterated-search "+request.getConversationId()+". Initializing KBIterator");
							}
							iterator = theDF.iteratedSearchAction(search, request.getSender());
							if (logger.isLoggable(Logger.FINEST)) {
								logger.log(Logger.FINEST, "DF "+myAgent.getName()+": Iterated-search "+request.getConversationId()+". KBIterator correctly initialized");
							}
						}
						
						// Get the requested number of results 
						List ll = new ArrayList();
						for (int i = 0; i < maxResult; ++i) {
							if (iterator.hasNext()) {
								ll.add(iterator.next());
							}
							else {
								if (logger.isLoggable(Logger.FINER)) {
									logger.log(Logger.FINER, "DF "+myAgent.getName()+": Iterated-search "+request.getConversationId()+". Closing KBIterator");
								}
								iterator.close();
								// FIXME: properly handle recursive search
								closeSessionOnNextReply();
								break;
							}
						}
						if (logger.isLoggable(Logger.FINE)) {
							logger.log(Logger.FINE, "DF "+myAgent.getName()+": Iterated-search "+request.getConversationId()+". Sending back "+ll.size()+" results");
						}
						
						// Fill the reply
						reply = request.createReply();
						try {
							Result result = new Result(aExpr, ll);
							theDF.getContentManager().fillContent(reply, result);
							reply.setPerformative(ACLMessage.INFORM);
						}
						catch (Exception e) {
							// Unexpected error encoding the reply
							e.printStackTrace();
							throw new FailureException(ExceptionVocabulary.INTERNALERROR+" \""+e.getMessage()+"\"");
						}
					}
					catch (OntologyException oe) {
						throw new NotUnderstoodException(ExceptionVocabulary.UNRECOGNISEDVALUE+" content");
					}	
					catch (CodecException ce) {
						throw new NotUnderstoodException(ExceptionVocabulary.UNRECOGNISEDVALUE+" content");
					}	
					catch (FailureException fe) {
						throw fe;
					}
					catch (Throwable t) {
						// Unexpected error
						t.printStackTrace();
						throw new FailureException(ExceptionVocabulary.INTERNALERROR+" \""+t.getMessage()+"\"");
					}
					
					return reply;
				}
				
				protected void handleCancel(ACLMessage cancel) {
					if (logger.isLoggable(Logger.FINE)) {
						logger.log(Logger.FINE, "DF "+myAgent.getName()+": Iterated-search "+cancel.getConversationId()+". Serving cancel");
					}
					if (iterator != null) {
						if (logger.isLoggable(Logger.FINER)) {
							logger.log(Logger.FINER, "DF "+myAgent.getName()+": Iterated-search "+cancel.getConversationId()+". Closing KBIterator");
						}
						iterator.close();
					}
				}
				
				/**
				 Avoid conflicts between the main MessageTemplate and that used 
				 internally by this IteratedFipaRequestResponder.
				 */
				protected void afterReply(ACLMessage reply) {
					if (reply != null) {
						myConversationId = reply.getConversationId();
						conversations.registerConversation(myConversationId);
					}
				}
				
				/**
				 When the protocol terminates, deregister the conversation
				 */
				public int onEnd() {
					conversations.deregisterConversation(myConversationId);
					return super.onEnd();
				}
			} );
		}
		else {
			block();
		}
	}
}

