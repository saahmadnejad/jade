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


package ir.donbee.jade.domain.JADEAgentManagement;

//#APIDOC_EXCLUDE_FILE

import ir.donbee.jade.util.leap.List;
import ir.donbee.jade.util.leap.ArrayList;
import ir.donbee.jade.util.leap.Iterator;

import ir.donbee.jade.core.AID;
import ir.donbee.jade.content.AgentAction;

/**
   This class represents the <code>sniff-off</code> action, requesting
   a sniffer to start observing a set of agents in the platform.

   @author Giovanni Rimassa -  Universita' di Parma
   @version $Date: 2003-11-24 14:47:00 +0100 (lun, 24 nov 2003) $ $Revision: 4597 $
*/
public class SniffOn implements AgentAction {

    private AID sniffer;
    private List sniffedAgents = new ArrayList();
    private String password;


    /**
       Default constructor. A default constructor is necessary for
       ontological classes.
    */
    public SniffOn() {
    }

    /**
       Set the <code>sniffer</code> slot of this action.
       @param id The agent identifier of the sniffer agent.
    */
    public void setSniffer(AID id) {
	sniffer = id;
    }

    /**
       Retrieve the value of the <code>sniffer</code> slot of this
       action, containing the agent identifier of the sniffer agent.
       @return The value of the <code>sniffer</code> slot, or
       <code>null</code> if no value was set.
    */
    public AID getSniffer() {
	return sniffer;
    }

    /**
       Remove all agent identifiers from the
       <code>sniffed-agents</code> slot collection of this object.
    */
    public void clearAllSniffedAgents() {
	sniffedAgents.clear();
    }

    /**
       Add an agent identifier to the <code>sniffed-agents</code> slot
       collection of this object.
       @param id The agent identifier to add to the collection.
    */
    public void addSniffedAgents(AID id) {
	sniffedAgents.add(id);
    }

    /**
       Remove an agent identifier from the <code>sniffed-agents</code>
       slot collection of this object.
       @param id The agent identifier to remove from the collection.
       @return A boolean, telling whether the element was present in
       the collection or not.
    */
    public boolean removeSniffedAgents(AID id) {
	return sniffedAgents.remove(id);
    }

    /**
       Access all agent identifiers from the
       <code>sniffed-agents</code> slot collection of this object.
       @return An iterator over the properties collection.
    */
    public Iterator getAllSniffedAgents() {
	return sniffedAgents.iterator();
    }

    /**
       This method is called by the AMS in order to prepare an RMI call.
       The <code>getAllSniffedAgents()</code> cannot be used as it returns
       an <code>Iterator</code> that is not serializable.
    */
    public ArrayList getCloneOfSniffedAgents(){
	return (ArrayList) ((ArrayList) sniffedAgents).clone();
    }

    /**
       Set the <code>password</code> slot of this action.
       @param p The password used to authenticate the principal
       requesting this action.
    */
    public void setPassword(String p) {
	password = p;
    }

    /**
       Retrieve the value of the <code>password</code> slot of this
       action, containing the password used to authenticate the
       principal requesting this action.
       @return The value of the <code>password</code> slot, or
       <code>null</code> if no value was set.
    */
    public String getPassword() {
	return password;
    }


}
