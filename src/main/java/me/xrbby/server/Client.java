package me.xrbby.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.util.UUID;

public class Client implements Runnable {

	private final Thread thread;
	private final RTServer server;
	private final Socket socket;
	private final UUID uuid;

	private BufferedReader reader;
	private PrintWriter writer;
	private boolean isRunning;

	public Client(RTServer server, Socket socket) {

		this.thread = new Thread(this);
		this.server = server;
		this.socket = socket;
		this.uuid = UUID.randomUUID();

		try {
			this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.writer = new PrintWriter(socket.getOutputStream(), true);

			server.getClients().add(this);
			server.getConfiguration().onClientConnect(this);

			this.isRunning = true;
			thread.start();
		} catch(IOException exception) {
			exception.printStackTrace();

			if(reader != null)
				try { reader.close(); }
				catch(IOException e) { e.printStackTrace(); }

			if(writer != null)
				writer.close();

			try { socket.close(); }
			catch(IOException e1) { e1.printStackTrace(); }
		}
	}

	@Override
	public void run() {

		while(isRunning)
			try {
				String receivedData = reader.readLine();

				if(receivedData != null) {
					String[] splitData = receivedData.split(" ", 2);

					URI uri = URI.create(splitData[0]);

					EndpointListener endpointListener = server.getEndpointListener(uri);

					if(endpointListener != null)
						endpointListener.onDataReceive(this, splitData[1]);
				}
			} catch(IOException ignored) { close(); }
	}

	public void send(URI uri, String data) {

		String packedData = uri.getPath() + " " + data;

		writer.println(packedData);
	}

	public void close() {

		server.getConfiguration().onClientDisconnect(this);
		server.getClients().remove(this);

		try { reader.close(); }
		catch(IOException exception) { exception.printStackTrace(); }

		writer.close();

		isRunning = false;

		try { socket.close(); }
		catch(IOException exception) { exception.printStackTrace(); }

		thread.interrupt();
	}

	public UUID getUuid() { return uuid; }
}