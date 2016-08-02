package com.openavionics.aeolus;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.RxNetty;
import rx.Observable;

/**
 * Created by dak on 6/16/2016.
 *
 * an observable that will spit out the Aeolus device packets parsed on a given port
 */
public class AeolusCnx {
	protected Observable<DatagramPacket> connectUdp(int port) {
		return RxNetty.createUdpClient("localhost",port).connect()
				// new Func1<ObservableConnection<DatagramPacket, DatagramPacket>, Observable<DatagramPacket>>()
				.flatMap(connection-> { return connection.getInput(); });
	}

	public Observable<Aeolus.Packet> connect() {
		return connectUdp(Aeolus.DEVICE_PORT)
				// new Func1<DatagramPacket, List<Aeolus.Packet>>()
				.map(datagramPacket-> {
						ByteBuf content = datagramPacket.content();
						return Aeolus.parseBuffer(content.array(), content.readableBytes());
				})
				// new Func1<List<Aeolus.Packet>, Observable<Aeolus.Packet>>()
				.flatMap(Observable::from);
	}
}
