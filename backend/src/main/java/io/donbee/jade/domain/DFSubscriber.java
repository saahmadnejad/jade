package io.donbee.jade.domain;

import io.donbee.jade.core.Agent;
import io.donbee.jade.domain.DFService;
import io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.proto.SubscriptionInitiator;
import io.donbee.jade.util.Logger;
import io.donbee.jade.util.leap.Iterator;

//#PJAVA_EXCLUDE_FILE
//#MIDP_EXCLUDE_FILE

public abstract class DFSubscriber extends SubscriptionInitiator {
	private static final long serialVersionUID = -5741304962740821073L;

	private static final Logger logger = Logger.getJADELogger(DFSubscriber.class.getName());

	private boolean firstNotificationReceived = false;

	public DFSubscriber(Agent a, DFAgentDescription template) {
		super(a, DFService.createSubscriptionMessage(a, a.getDefaultDF(), template, null));
	}

	public abstract void onRegister(DFAgentDescription dfad);
	public abstract void onDeregister(DFAgentDescription dfad);

	public void afterFirstNotification(DFAgentDescription[] dfds) {
		// default: nothing to do
	}

	@Override
	protected void handleInform(ACLMessage inform) {
		try {
			DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());

			for (int i = 0; i < dfds.length; ++i) {
				DFAgentDescription dfad = dfds[i];
				Iterator services = dfad.getAllServices();		  				
				if (services.hasNext()) { 
					onRegister(dfad);
				} else {
					onDeregister(dfad);
				}
			}
			if (!firstNotificationReceived) {
				firstNotificationReceived = true;
				afterFirstNotification(dfds);
			}
		}
		catch (Exception e) {
			logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": Error decoding DF notification", e);            
		}
	}
}