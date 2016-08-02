package com.openavionics.aeolus;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dak on 6/14/2016.
 */
public class Aeolus {

	public static class Packet {
		byte id;
		byte b[];
		double ts;
		float f[];

		public Packet(byte id) {
			this.id = id;
			b = null;
			f = null;
			ts = 0;
		}

		public void setBytes(byte[] buf, int from, int n) {
			b = new byte[n];
			for (int i=0; i<n; i++) {
				b[i] = buf[from+i];
			}
		}

		public void setFloats(byte[] packetBuffer, int from, int n) {
			FloatBuffer fb = ByteBuffer.wrap(packetBuffer, from,  n*4)
								.order(ByteOrder.LITTLE_ENDIAN)
								.asFloatBuffer();
			f=new float[n];
			for (int j = 0; j<n; j++) {
				f[j] = fb.get(j);    //data bytes 16-..
			}

		}

		public void setTs(byte[] packetBuffer, int from) {
			DoubleBuffer db = ByteBuffer.wrap(packetBuffer, from, 8)
					.order(ByteOrder.LITTLE_ENDIAN)
					.asDoubleBuffer();
			ts = db.get(0);
		}
	}

	public final static int GDL90_FLAGBYTE = 126;
	public final static int GDL90_ESCAPEBYTE = 125;

	public final static int TALOSCOM_ID_AEOLUSINFO = 51;
	public final static int TALOSCOM_ID_PRESSURE = 61;
	public final static int TALOSCOM_ID_GYRO = 62;
	public final static int TALOSCOM_ID_ACCEL = 63;
	public final static int TALOSCOM_ID_MAGNET = 64;
	public final static int TALOSCOM_ID_TEMP = 65;
	public final static int TALOSCOM_ID_GPS = 66;
	public final static int TALOSCOM_ID_RPY = 71;
	public final static int TALOSCOM_ID_COMPASS = 72;
	public final static int TALOSCOM_ID_WIND = 73;
	public final static int TALOSCOM_ID_CALIBINFO = 75;
	public final static int TALOSCOM_ID_DEBUG = 76;
	public final static int TALOSCOM_ID_LOG = 81;
	public final static int TALOSCOM_ID_TOAST = 82;


	public final static int DEVICE_MAXGDL90PACKETSIZE = 500 + 5; // 1 packet = flag + id + 500 bytes data + crc1 + crc2 + flag
	public final static int DEVICE_MINUDPPACKETSIZE = 5; // 1 packets (flag+id+2*crc+flag+
	public final static int DEVICE_MAXUDPPACKETSIZE = 1500;
	public final static int DEVICE_PORT = 5001;


	private static int findNextFlagByte(byte[] buffer, int bufferLen, int fromi) {
		while (fromi < bufferLen) {
			if (buffer[fromi] == GDL90_FLAGBYTE) return fromi;
			fromi++;
		}
		return -1;
	}

	public static List<Packet> parseBuffer(byte[] buffer, int bufferLen) {
		List<Packet> list = new ArrayList<Packet>();
		
		if ((bufferLen < DEVICE_MINUDPPACKETSIZE) || (bufferLen > DEVICE_MAXUDPPACKETSIZE)) {
			Log.d("Aeolus.parseBuffer()", "DeviceAeolus.readdata ERROR: bytesRead = " + bufferLen);
			return list;
		}
		byte[] packetBuffer = new byte[DEVICE_MAXGDL90PACKETSIZE];
		int i0 = -1;
		int i1 = 0;
		while (i0 + 1 < bufferLen) {
			i1 = findNextFlagByte(buffer, bufferLen, i0 + 1);

			if (i1 < 0) {
				if (i0 < 0)
					Log.d("Aeolus.parseBuffer()", String.format("flag byte not found, bufferLen=%d", bufferLen));
				else
					Log.d("Aeolus.parseBuffer()", String.format("Packet end not found. pack start=%d , bufferLen=%d", i0, bufferLen));
				break;
			}

			if ((i0 >= 0) && ((i1 - i0) > 1)) {
				int i = i0 + 1;
				int packetlen = 0;
				while (i < i1) {
					if (buffer[i] != GDL90_ESCAPEBYTE) {
						packetBuffer[packetlen] = buffer[i];
						i++;
						packetlen++;
					} else {
						i++; // petame to escape charakter
						packetBuffer[packetlen] = (byte) (buffer[i] ^ GDL90_ESCAPEBYTE);
						i++;
						packetlen++;
					}
				}

				if (packetlen >=3) { // id + crc
					byte packetId=packetBuffer[0];
					if ((packetId == TALOSCOM_ID_LOG)||(packetId == TALOSCOM_ID_TOAST)) {
						Packet p = new Packet(packetId);
						p.setBytes(packetBuffer, 1, packetlen-3);
						list.add(p);
					} else if (packetlen>= 35) { // 1 id + 8 bytes + 8 ts + 4 * 4 floats min = 33 + 2 crc = 35 bytes minimum
						Packet p = new Packet(packetId);
						p.setBytes(packetBuffer, 1, 8); // data byte 0..7
						p.setTs(packetBuffer, 9); // data bytes 8-15
						p.setFloats(packetBuffer, 17, (packetlen-3-16)/4); // data bytes 16 ...
						list.add(p);
					} else { // packet len >= 3 but < 35
						Log.d("Aeolus.parseBuffer()", String.format("ERROR: bad packet length, recieved %d bytes", packetlen));
					}
				} else { // packet < 3
					Log.d("Aeolus.parseBuffer()", String.format("ERROR: packet length < 3 recieved %d bytes", packetlen));
				}
			} // if ((i0 >= 0)&&((i1 - i0)> 1)){
			i0 = i1;
		}
		return list;
	}
}
