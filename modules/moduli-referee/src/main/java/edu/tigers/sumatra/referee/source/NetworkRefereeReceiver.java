/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.referee.source;

import com.github.g3force.configurable.ConfigRegistration;
import com.github.g3force.configurable.Configurable;
import edu.tigers.sumatra.network.IReceiver;
import edu.tigers.sumatra.network.MulticastUDPReceiver;
import edu.tigers.sumatra.network.NetworkUtility;
import edu.tigers.sumatra.referee.proto.SslGcRefereeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Optional;


/**
 * New implementation of the referee receiver with the new protobuf message format (2013)
 */
public class NetworkRefereeReceiver extends ARefereeMessageSource implements Runnable, IReceiver
{
	private static final Logger log = LogManager.getLogger(NetworkRefereeReceiver.class.getName());

	private static final int BUFFER_SIZE = 10000;

	@Configurable(defValue = "224.5.23.1")
	private static String address;

	@Configurable
	private static String network;

	private int port;
	private Thread referee;
	private IReceiver receiver;

	private InetAddress refBoxAddress = null;

	private boolean expectIOE = false;

	static
	{
		ConfigRegistration.registerClass("user", NetworkRefereeReceiver.class);
	}


	/** Constructor */
	public NetworkRefereeReceiver()
	{
		super(ERefereeMessageSource.NETWORK);
	}


	@Override
	public void start()
	{
		// --- Choose network-interface
		NetworkInterface nif = NetworkUtility.chooseNetworkInterface(network, 3);

		if (nif == null)
		{
			log.debug("No nif for referee specified, will try all.");
			receiver = new MulticastUDPReceiver(port, address);
		} else
		{
			log.info("Chose nif for referee: {}", nif.getDisplayName());
			receiver = new MulticastUDPReceiver(port, address, nif);
		}

		referee = new Thread(this, "External Referee");
		referee.start();
	}


	@Override
	public void run()
	{
		final DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);

		while (!Thread.currentThread().isInterrupted())
		{
			try
			{
				receive(packet);
			} catch (final IOException err)
			{
				if (!expectIOE)
				{
					log.error("Error while receiving referee-message!", err);
				}
				break;
			}

			refBoxAddress = packet.getAddress();
			final ByteArrayInputStream packetIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());

			SslGcRefereeMessage.Referee sslRefereeMsg;
			try
			{
				sslRefereeMsg = SslGcRefereeMessage.Referee.parseFrom(packetIn);

				// Notify the receipt of a new RefereeMessage to any other observers
				notifyNewRefereeMessage(sslRefereeMsg);
			} catch (IOException err)
			{
				log.error("Could not read referee message ", err);
			}
		}

		// Cleanup
		expectIOE = false;
	}


	@Override
	public DatagramPacket receive(final DatagramPacket store) throws IOException
	{
		return receiver.receive(store);
	}


	@Override
	public void cleanup()
	{
		if (referee != null)
		{
			referee.interrupt();
			referee = null;
		}

		if (receiver != null)
		{
			expectIOE = true;

			try
			{
				receiver.cleanup();
			} catch (final IOException err)
			{
				log.debug("Socket closed...", err);
			}

			receiver = null;
		}
	}


	@Override
	public void stop()
	{
		cleanup();
	}


	@Override
	public boolean isReady()
	{
		return true;
	}


	@Override
	public Optional<InetAddress> getRefBoxAddress()
	{
		return Optional.ofNullable(refBoxAddress);
	}


	public void setPort(final int port)
	{
		this.port = port;
	}
}
