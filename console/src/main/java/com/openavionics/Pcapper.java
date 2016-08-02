package com.openavionics;

import java.io.IOException;

import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.packet.Packet;
import io.pkts.protocol.Protocol;

/**
 * Created by dak on 6/16/2016.
 */
public class Pcapper {
	public static void main(String[] args) {
		try {
			final Pcap pcap = Pcap.openStream("D:\\src\\android\\StudioProjects\\avionics\\AvionicLib\\assets\\aeolus-lhtl.tcpdump");
			pcap.loop(new PacketHandler() {
				@Override
				public boolean nextPacket(final Packet packet) throws IOException {

					// Step 3 - For every new packet the PacketHandler will be
					//          called and you can examine this packet in a few
					//          different ways. You can e.g. check whether the
					//          packet contains a particular protocol, such as UDP.
					if (packet.hasProtocol(Protocol.UDP)) {

						// Step 4 - Now that we know that the packet contains
						//          a UDP packet we get ask to get the UDP packet
						//          and once we have it we can just get its
						//          payload and print it, which is what we are
						//          doing below.
						System.out.println(packet.getPacket(Protocol.UDP).getPayload());
					}

					return true;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
