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
package ir.donbee.jade.core.resource;

import java.util.ArrayList;
import java.util.List;

//#J2ME_EXCLUDE_FILE
//#APIDOC_EXCLUDE_FILE

class SingletonResourceManagementHelper {

	private List<ResourceManagementHelper> helpers;

	private final static SingletonResourceManagementHelper theInstance = new SingletonResourceManagementHelper();
	
	public final static SingletonResourceManagementHelper getInstance() {
		return theInstance;
	}
	
	private SingletonResourceManagementHelper() {
		helpers = new ArrayList<ResourceManagementHelper>();
	}

	public void addHelper(ResourceManagementHelper helper) {
		helpers.add(helper);
	}

	public void removeHelper(ResourceManagementHelper helper) {
		helpers.remove(helper);
	}
	
	public ResourceManagementHelper getHelper() {
		ResourceManagementHelper helper = null;
		if (helpers.size() > 0) {
			helper = helpers.get(0);
		}
		return helper;
	}
}
