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

package ir.donbee.jade.core.messaging;

//#MIDP_EXCLUDE_FILE


import java.io.IOException;
import java.util.Date;

import ir.donbee.jade.core.AID;
import ir.donbee.jade.core.Profile;
import ir.donbee.jade.lang.acl.ACLMessage;

public interface MessageStorage {

  interface LoadListener {
    void loadStarted(String storeName);
    void itemLoaded(String storeName, GenericMessage msg, AID receiver);
    void loadEnded(String storeName);
  }
  
  void init(Profile p);
  String store(GenericMessage msg, AID receiver) throws IOException;
  void delete(String storeName, AID receiver) throws IOException;
  void loadAll(LoadListener ll) throws IOException;

}
