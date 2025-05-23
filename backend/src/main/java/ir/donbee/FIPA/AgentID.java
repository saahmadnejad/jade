/*
 * File: ./FIPA/AGENTID.JAVA
 * From: IDL
 * Date: Mon Sep 04 15:08:50 2000
 *   By: idltojava Java IDL 1.2 Nov 10 1997 13:52:11
 */

package ir.donbee.FIPA;
public final class AgentID {
    //	instance variables
    public String name;
    public String[] addresses;
    public AgentID[] resolvers;
    public Property[] userDefinedProperties;
    //	constructors
    public AgentID() { }
    public AgentID(String __name, String[] __addresses, AgentID[] __resolvers, Property[] __userDefinedProperties) {
	name = __name;
	addresses = __addresses;
	resolvers = __resolvers;
	userDefinedProperties = __userDefinedProperties;
    }
}
