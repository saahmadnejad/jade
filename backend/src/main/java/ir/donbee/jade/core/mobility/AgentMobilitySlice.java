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

package ir.donbee.jade.core.mobility;

//#MIDP_EXCLUDE_FILE


import ir.donbee.jade.core.Service;
import ir.donbee.jade.core.Filter;
import ir.donbee.jade.core.AID;
import ir.donbee.jade.core.ContainerID;
import ir.donbee.jade.core.Location;
import ir.donbee.jade.core.IMTPException;
import ir.donbee.jade.core.ServiceException;
import ir.donbee.jade.core.NotFoundException;
import ir.donbee.jade.core.NameClashException;

import ir.donbee.jade.security.Credentials;
import ir.donbee.jade.security.JADESecurityException;

import ir.donbee.jade.util.leap.List;

/**

   The horizontal interface for the JADE kernel-level service managing
   the mobility-related agent life cycle: migration and clonation.

   @author Giovanni Rimassa - FRAMeTech s.r.l.
*/
public interface AgentMobilitySlice extends Service.Slice {


    /**
       The name of this service.
    */
    public static final String NAME = "ir.donbee.jade.core.mobility.AgentMobility";


    // Constants for the names of horizontal commands associated to methods
    static final String H_CREATEAGENT = "1";
    static final String H_FETCHCLASSFILE = "2";
    static final String H_MOVEAGENT = "3";
    static final String H_COPYAGENT = "4";
    static final String H_PREPARE = "5";
    static final String H_TRANSFERIDENTITY = "6";
    static final String H_HANDLETRANSFERRESULT = "7";
    static final String H_CLONEDAGENT = "8";
	//#J2ME_EXCLUDE_BEGIN
    static final String H_CLONECODELOCATORENTRY = "9";
    static final String H_REMOVECODELOCATORENTRY = "10";
	//#J2ME_EXCLUDE_END

    void createAgent(AID agentID, byte[] serializedInstance, String classSiteName, boolean isCloned, boolean startIt) throws IMTPException, ServiceException, NotFoundException, NameClashException, JADESecurityException;
    byte[] fetchClassFile(String className, String agentName) throws IMTPException, ClassNotFoundException;

    void moveAgent(AID agentID, Location where) throws IMTPException, NotFoundException;
    void copyAgent(AID agentID, Location where, String newName) throws IMTPException, NotFoundException;

    boolean prepare() throws IMTPException;

    boolean transferIdentity(AID agentID, Location src, Location dest) throws IMTPException, NotFoundException;
    void handleTransferResult(AID agentID, boolean result, List messages) throws IMTPException, NotFoundException;
    void clonedAgent(AID agentID, ContainerID cid, Credentials creds) throws IMTPException, JADESecurityException, NotFoundException, NameClashException;
    
	//#J2ME_EXCLUDE_BEGIN
    void cloneCodeLocatorEntry(AID oldAgentID, AID newAgentID) throws IMTPException, NotFoundException;
    void removeCodeLocatorEntry(AID name) throws IMTPException, NotFoundException;
	//#J2ME_EXCLUDE_END
    
}
