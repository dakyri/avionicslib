package com.openavionics.aeolus;

import android.os.Handler;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collection;


/**
 * Created by dak on 6/16/2016.
 */
public class AeolusUDPRunnable implements Runnable {
	private static Handler mHandler = null;
	private static Thread mThread = null;

	private byte[] udpbuffer = new byte[Aeolus.DEVICE_MAXUDPPACKETSIZE];
	private int udpbuffersize = 0;
	private boolean mKeepRunning = true;

	public AeolusUDPRunnable(Handler ah) {
		mThread = null;
		mHandler = ah;
	}

	public boolean udpStart() {
		mThread = new Thread(this, "Aeolus UDP Thread");
		mThread.start();
		mKeepRunning = true;
		return true;
	}

	public void udpStop() {
		mKeepRunning = false;

		boolean retry = true;
		//mThread.setRunning(false);
		while (retry) {
			try {
				mThread.interrupt();
				mThread.join(1);
				retry = false;
				mThread = null;
				//     ((Activity)getContext()).finish();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void run() {
		DatagramPacket packet = new DatagramPacket(udpbuffer, Aeolus.DEVICE_MAXUDPPACKETSIZE);
		DatagramSocket udpSocket = null;
		mKeepRunning = true;
		try {
			udpSocket = new DatagramSocket(Aeolus.DEVICE_PORT);
			while ((!mThread.currentThread().isInterrupted()) && (mKeepRunning)) {
				udpSocket.receive(packet);
				udpbuffersize = packet.getLength();
				Collection<Aeolus.Packet> packets = Aeolus.parseBuffer(udpbuffer, udpbuffersize);
				for (Aeolus.Packet p: packets) {
					mHandler.obtainMessage(p.id, 6, -1, p).sendToTarget();
				}
			} // while
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if (udpSocket != null) udpSocket.close();
	}
}

