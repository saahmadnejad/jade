/*
 * File: ./FIPA/ENVELOPE.JAVA
 * From: IDL
 * Date: Mon Sep 04 15:08:50 2000
 *   By: idltojava Java IDL 1.2 Nov 10 1997 13:52:11
 */

package io.donbee.FIPA;
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
    public io.donbee.FIPA.AgentID[] intendedReceiver;
    public io.donbee.FIPA.ReceivedObject[] received;
    public io.donbee.FIPA.Property[][] transportBehaviour;
    public io.donbee.FIPA.Property[] userDefinedProperties;
    //	constructors
    public Envelope() { }
    public Envelope(io.donbee.FIPA.AgentID[] __to, io.donbee.FIPA.AgentID[] __from, String __comments, String __aclRepresentation, int __payloadLength, String __payloadEncoding, io.donbee.FIPA.DateTime[] __date, String[] __encrypted, io.donbee.FIPA.AgentID[] __intendedReceiver, io.donbee.FIPA.ReceivedObject[] __received, io.donbee.FIPA.Property[][] __transportBehaviour, io.donbee.FIPA.Property[] __userDefinedProperties) {
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
