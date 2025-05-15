/*
 * File: ./FIPA/ENVELOPE.JAVA
 * From: IDL
 * Date: Mon Sep 04 15:08:50 2000
 *   By: idltojava Java IDL 1.2 Nov 10 1997 13:52:11
 */

package ir.donbee.FIPA;
public final class Envelope {
    //	instance variables
    public AgentID[] to;
    public AgentID[] from;
    public String comments;
    public String aclRepresentation;
    public int payloadLength;
    public String payloadEncoding;
    public DateTime[] date;
    public String[] encrypted;
    public ir.donbee.FIPA.AgentID[] intendedReceiver;
    public ir.donbee.FIPA.ReceivedObject[] received;
    public ir.donbee.FIPA.Property[][] transportBehaviour;
    public ir.donbee.FIPA.Property[] userDefinedProperties;
    //	constructors
    public Envelope() { }
    public Envelope(ir.donbee.FIPA.AgentID[] __to, ir.donbee.FIPA.AgentID[] __from, String __comments, String __aclRepresentation, int __payloadLength, String __payloadEncoding, ir.donbee.FIPA.DateTime[] __date, String[] __encrypted, ir.donbee.FIPA.AgentID[] __intendedReceiver, ir.donbee.FIPA.ReceivedObject[] __received, ir.donbee.FIPA.Property[][] __transportBehaviour, ir.donbee.FIPA.Property[] __userDefinedProperties) {
	to = __to;
	from = __from;
	comments = __comments;
	aclRepresentation = __aclRepresentation;
	payloadLength = __payloadLength;
	payloadEncoding = __payloadEncoding;
	date = __date;
	encrypted = __encrypted;
	intendedReceiver = __intendedReceiver;
	received = __received;
	transportBehaviour = __transportBehaviour;
	userDefinedProperties = __userDefinedProperties;
    }
}
