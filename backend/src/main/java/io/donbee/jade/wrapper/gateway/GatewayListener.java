package io.donbee.jade.wrapper.gateway;

public interface GatewayListener {
	void handleGatewayConnected();
	void handleGatewayDisconnected();
}
