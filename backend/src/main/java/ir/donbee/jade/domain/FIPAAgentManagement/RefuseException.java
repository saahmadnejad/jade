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


package ir.donbee.jade.domain.FIPAAgentManagement;

import ir.donbee.jade.domain.FIPAException;
import ir.donbee.jade.lang.acl.ACLMessage;

/**
This class represents a generic RefuseException 
@author Fabio Bellifemine - CSELT S.p.A. 
@version $Date: 2009-03-03 15:02:51 +0100 (mar, 03 mar 2009) $ $Revision: 6097 $
 */
public class RefuseException extends FIPAException {

	public RefuseException(String message) {
		super(message); 
	}

	public RefuseException(ACLMessage refuse) {
		super(refuse); 
	}

	public ACLMessage getACLMessage() {
		if (msg == null) {
			msg = new ACLMessage(ACLMessage.REFUSE);
			msg.setContent(getMessage()); 
		} 
		return msg;
	}
}
