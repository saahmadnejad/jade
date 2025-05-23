/*
 * File: ./FIPA/OPTRECEIVEDOBJECTHOLDER.JAVA
 * From: IDL
 * Date: Mon Sep 04 15:08:50 2000
 *   By: idltojava Java IDL 1.2 Nov 10 1997 13:52:11
 */

package ir.donbee.FIPA;
public final class OptReceivedObjectHolder
    implements org.omg.CORBA.portable.Streamable
{
    //	instance variable 
    public ReceivedObject[] value;
    //	constructors 
    public OptReceivedObjectHolder() {
	this(null);
    }
    public OptReceivedObjectHolder(ReceivedObject[] __arg) {
	value = __arg;
    }
    public void _write(org.omg.CORBA.portable.OutputStream out) {
        OptReceivedObjectHelper.write(out, value);
    }

    public void _read(org.omg.CORBA.portable.InputStream in) {
        value = OptReceivedObjectHelper.read(in);
    }

    public org.omg.CORBA.TypeCode _type() {
        return OptReceivedObjectHelper.type();
    }
}
