package ir.donbee.jade.core;

//#MIDP_EXCLUDE_FILE

import ir.donbee.jade.core.event.JADEEvent;

/**
 * Interface to be implemented by classes that need to be notified about 
 * FrontEnd relevant events such as BORN_AGENT and DEAD_AGENT.
 * @see MicroRuntime#addListener(FEListener)
 */
public interface FEListener {
	void handleEvent(JADEEvent ev);
}
