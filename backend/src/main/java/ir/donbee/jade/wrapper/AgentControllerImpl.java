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

package ir.donbee.jade.wrapper;

import ir.donbee.jade.core.AID;
import ir.donbee.jade.core.Location;
import ir.donbee.jade.core.NotFoundException;

/**
   This class is a Proxy class, allowing access to a JADE agent.
   Invoking methods on instances of this class, it is possible to
   trigger state transition of the agent life cycle.  This class must
   not be instantiated by applications. Instead, use the
   <code>createAgent()</code> method in class
   <code>AgentContainer</code>.
   <br>
   <b>NOT available in MIDP</b>
   <br>
   @see ir.donbee.jade.wrapper.AgentContainer#createNewAgent(String, String, Object[])
   @author Giovanni Rimassa - Universita' di Parma
 */
class AgentControllerImpl implements AgentController {

	private AID agentID;
	private ContainerProxy myProxy;
	private ir.donbee.jade.core.AgentContainer myContainer;

	/**
     This constructor should not be called by applications.
     The method <code>AgentContainer.createAgent()</code> should
     be used instead.
	 */
	public AgentControllerImpl(AID id, ContainerProxy cp, ir.donbee.jade.core.AgentContainer ac) {
		agentID = id;
		myProxy = cp;
		myContainer = ac;
	}


	/**
	 * @see ir.donbee.jade.wrapper.AgentController#getName()
	 */
	public String getName() throws StaleProxyException {
		// Just to check that the agent is still there
		ir.donbee.jade.core.Agent a = myContainer.acquireLocalAgent(agentID);
		if (a == null) {
			throw new StaleProxyException("Controlled agent not found");
		}
		myContainer.releaseLocalAgent(agentID);
		return agentID.getName();
	}       

	/**
	 * @see ir.donbee.jade.wrapper.AgentController#start()
	 */
	public void start() throws StaleProxyException {
		try {
			myContainer.powerUpLocalAgent(agentID);
		}
		catch (NotFoundException nfe) {
			throw new StaleProxyException("Controlled agent not found");
		}  		
	}

	/**
	 * @see ir.donbee.jade.wrapper.AgentController#suspend()
	 */
	public void suspend() throws StaleProxyException {
		try {
			myProxy.suspendAgent(agentID);
		}
		catch (Throwable t) {
			throw new StaleProxyException(t);
		}
	}

	/**
	 * @see ir.donbee.jade.wrapper.AgentController#activate()
	 */
	public void activate() throws StaleProxyException {
		try {
			myProxy.activateAgent(agentID);
		}
		catch (Throwable t) {
			throw new StaleProxyException(t);
		}
	}

	/**
	 * @see ir.donbee.jade.wrapper.AgentController#kill()
	 */
	public void kill() throws StaleProxyException {
		try {
			myProxy.killAgent(agentID);
		}
		catch (Throwable t) {
			throw new StaleProxyException(t);
		}
	}

	/**
	 * @see ir.donbee.jade.wrapper.AgentController#move(ir.donbee.jade.core.Location)
	 */
	public void move(Location where) throws StaleProxyException {
		try {
			myProxy.moveAgent(agentID, where);
		}
		catch (Throwable t) {
			throw new StaleProxyException(t);
		}
	}


	/**
	 * @see ir.donbee.jade.wrapper.AgentController#clone(ir.donbee.jade.core.Location, java.lang.String)
	 */
	public void clone(Location where, String newName) throws StaleProxyException {
		try {
			myProxy.cloneAgent(agentID, where, newName);
		}
		catch (Throwable t) {
			throw new StaleProxyException(t);
		}
	}

	/**
	 * @see ir.donbee.jade.wrapper.AgentController#putO2AObject(java.lang.Object, boolean)
	 */
	public void putO2AObject(Object o, boolean blocking) throws StaleProxyException {
		ir.donbee.jade.core.Agent adaptee = myContainer.acquireLocalAgent(agentID);
		if (adaptee == null) {
			throw new StaleProxyException("Controlled agent does not exist");
		}
		try {
			adaptee.putO2AObject(o, blocking);
		} catch (InterruptedException ace) {
			throw new StaleProxyException(ace);
		}
		finally {
			myContainer.releaseLocalAgent(agentID);
		}
	}

	//#J2ME_EXCLUDE_BEGIN
	@SuppressWarnings("unchecked")
	public <T> T getO2AInterface(Class<T> theInterface) throws StaleProxyException {
		ir.donbee.jade.core.Agent adaptee = myContainer.acquireLocalAgent(agentID);
		if (adaptee == null) {
			throw new StaleProxyException("Controlled agent does not exist");
		}
		try {
			T o2aInterfaceImpl = adaptee.getO2AInterface(theInterface);
	
			if(o2aInterfaceImpl == null)
				return null;
	
			ClassLoader classLoader = o2aInterfaceImpl.getClass().getClassLoader();
	
			return (T) java.lang.reflect.Proxy.newProxyInstance(classLoader, new Class[] { theInterface }, new O2AProxy(o2aInterfaceImpl) {
				protected void checkAgent() throws O2AException {
					if (!myContainer.isLocalAgent(agentID)) {
						throw new O2AException("Controlled agent does not exist");
					}
				}
			});
		}
		finally {
			myContainer.releaseLocalAgent(agentID);
		}
	}
	//#J2ME_EXCLUDE_END
	
	/**
	 * @see ir.donbee.jade.wrapper.AgentController#getState()
	 */
	public State getState() throws StaleProxyException {
		ir.donbee.jade.core.Agent adaptee = myContainer.acquireLocalAgent(agentID);
		if (adaptee == null) {
			throw new StaleProxyException("Controlled agent does not exist");
		}
		int jadeState = adaptee.getState();
		State ret = null;
		switch (jadeState) {
		case ir.donbee.jade.core.Agent.AP_INITIATED:
			ret =  AgentState.AGENT_STATE_INITIATED;
			break;
		case ir.donbee.jade.core.Agent.AP_ACTIVE:
			ret =  AgentState.AGENT_STATE_ACTIVE;
			break;
		case ir.donbee.jade.core.Agent.AP_IDLE:
			ret =  AgentState.AGENT_STATE_IDLE;
			break;
		case ir.donbee.jade.core.Agent.AP_SUSPENDED:
			ret =  AgentState.AGENT_STATE_SUSPENDED;
			break;
		case ir.donbee.jade.core.Agent.AP_WAITING:
			ret =  AgentState.AGENT_STATE_WAITING;
			break;
		case ir.donbee.jade.core.Agent.AP_DELETED:
			ret =  AgentState.AGENT_STATE_DELETED;
			break;
			// FIXME: Correctly handle states defined outside the Agent class
			/*case ir.donbee.jade.core.Agent.AP_TRANSIT:
        ret =  AgentState.AGENT_STATE_INTRANSIT;
        break;*/
		default:
			throw new InternalError("Unknown state: " + jadeState);
		}
		myContainer.releaseLocalAgent(agentID);
		return ret;
	}


}
