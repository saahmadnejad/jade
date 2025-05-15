package ir.donbee.jade.domain;

import ir.donbee.jade.core.Agent;
import ir.donbee.jade.domain.DFService;
import ir.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription;
import ir.donbee.jade.lang.acl.ACLMessage;
import ir.donbee.jade.proto.SubscriptionInitiator;
import ir.donbee.jade.util.Logger;
import ir.donbee.jade.util.leap.Iterator;

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