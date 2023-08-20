package me.xrbby.server;

@FunctionalInterface
public interface EndpointListener {

	void onDataReceive(Client client, String data);
}