/*
 * File: ./FIPA/FIPAMESSAGE.JAVA
 * From: IDL
 * Date: Mon Sep 04 15:08:50 2000
 *   By: idltojava Java IDL 1.2 Nov 10 1997 13:52:11
 */

package ir.donbee.FIPA;
public final class FipaMessage {
    //	instance variables
    public Envelope[] messageEnvelopes;
    public byte[] messageBody;
    //	constructors
    public FipaMessage() { }
    public FipaMessage(Envelope[] __messageEnvelopes, byte[] __messageBody) {
	messageEnvelopes = __messageEnvelopes;
	messageBody = __messageBody;
    }
}
