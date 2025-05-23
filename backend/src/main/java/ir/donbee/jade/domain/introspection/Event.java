/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

The updating of this file to JADE 2.0 has been partially supported by
the IST-1999-10211 LEAP Project

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


package ir.donbee.jade.domain.introspection;

import ir.donbee.jade.content.Concept;


/**
   The generic interface for all introspection events.

   @author Giovanni Rimassa -  Universita' di Parma
   @version $Date: 2003-11-19 17:04:37 +0100 (mer, 19 nov 2003) $ $Revision: 4567 $
*/
public interface Event extends Concept {

    /**
       Retrieve the name of this event.
       @return The event name.
    */
    String getName();

}
