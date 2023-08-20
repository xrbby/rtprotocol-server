package me.xrbby.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RTServer implements Runnable {

	public interface ServerConfiguration {

		void onServerStart(int port);
		void onServerStop();
		void onClientConnect(Client client);
		void onClientDisconnect(Client client);
	}

	private final List<Client> clients = new ArrayList<>();
	private final Map<URI, EndpointListener> endpoints = new HashMap<>();

	private final Thread serverThread;
	private final ServerSocket serverSocket;
	private final int serverPort;
	private final ServerConfiguration configuration;
	private boolean isRunning;

	public RTServer(int serverPort, ServerConfiguration configuration) throws IOException {

		this.serverThread = new Thread(this);
		this.serverSocket = new ServerSocket(serverPort);
		this.serverPort = serverPort;
		this.configuration = configuration;
	}

	public void start() {

		if(isRunning)
			return;

		configuration.onServerStart(serverPort);

		isRunning = true;
		serverThread.start();
	}

	public boolean registerEndpointListener(URI uri, EndpointListener endpointListener) {

		if(endpoints.containsKey(uri))
			return false;

		endpoints.put(uri, endpointListener);
		return true;
	}

	protected EndpointListener getEndpointListener(URI uri) {

		return endpoints.getOrDefault(uri, null);
	}

	@Override
	public void run() {

		while(isRunning)
			try { new Client(this, serverSocket.accept()); }
			catch(IOException exception) { exception.printStackTrace(); stop(); }
	}

	public void broadcast(URI uri, String data) {

		for(Client client : clients)
			client.send(uri, data);
	}

	public void stop() {

		configuration.onServerStop();

		for(Client client : clients)
			client.close();

		isRunning = false;

		try { serverSocket.close(); }
		catch(IOException exception) { exception.printStackTrace(); }

		serverThread.interrupt();
	}

	protected List<Client> getClients() { return clients; }
	protected ServerConfiguration getConfiguration() { return configuration; }
}