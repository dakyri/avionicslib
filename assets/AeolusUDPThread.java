public class AeolusUDPThread implements Runnable {

    public class AeolusPacket {
        byte id;
        byte b[];
        double ts;
        float f[];
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


    public final static int AEOLUSDEVICE_MAXGDL90PACKETSIZE = 500 + 5; // 1 packet = flag + id + 500 bytes data + crc1 + crc2 + flag
    public final static int AEOLUSDEVICE_MINUDPPACKETSIZE = 5; // 1 packets (flag+id+2*crc+flag+
    public final static int AEOLUSDEVICE_MAXUDPPACKETSIZE = 1500; 
    public final static int AEOLUSDEVICE_PORT = 5001;


    private static Handler mHandler = null;
    private static Thread mThread = null;

    private byte[] udpbuffer = new byte[AEOLUSDEVICE_MAXUDPPACKETSIZE];
    private int udpbuffersize = 0;
    private byte[] packetBuffer = new byte[AEOLUSDEVICE_MAXGDL90PACKETSIZE];
    private boolean mKeepRunning = true;

    public AeolusUDPThread(Handler ah) {
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
        DatagramPacket packet = new DatagramPacket(udpbuffer, AEOLUSDEVICE_MAXUDPPACKETSIZE);
        DatagramSocket udpSocket = null;
        mKeepRunning = true;
        try {
            udpSocket = new DatagramSocket(AEOLUSDEVICE_PORT);
            while ((!mThread.currentThread().isInterrupted()) && (mKeepRunning)) {
                udpSocket.receive(packet);

                udpbuffersize = packet.getLength();
                if ((udpbuffersize < AEOLUSDEVICE_MINUDPPACKETSIZE) || (udpbuffersize > AEOLUSDEVICE_MAXUDPPACKETSIZE)) {
                    Log.d("AeolusUDPThread", "DeviceAeolus.readdata ERROR: bytesRead = " + udpbuffersize);
                } else
                    parseUDPBuffer();
            } // while
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (udpSocket != null) udpSocket.close();
    }

    private int findnextflagbyte(int astartpos) {
        while (astartpos < udpbuffersize) {
            if (udpbuffer[astartpos] == GDL90_FLAGBYTE) return astartpos;
            astartpos++;
        }
        return -1;
    }

    private void parseUDPBuffer() {
        int i0 = -1;
        int i1 = 0;
        while (i0 + 1 < udpbuffersize) {
            i1 = findnextflagbyte(i0 + 1);

            if (i1 < 0) {
                if (i0 < 0)
                    Log.d("AeolusUDPThread", String.format("flag byte not found, pack size=%d", udpbuffersize));
                else
                    Log.d("AeolusUDPThread", String.format("Packet end not found. pack start=%d , pack size=%d", i0, udpbuffersize));
                break;
            }

            if ((i0 >= 0) && ((i1 - i0) > 1)) {
                int i = i0 + 1;
                int packetlen = 0;
                while (i < i1) {
                    if (udpbuffer[i] != GDL90_ESCAPEBYTE) {
                        packetBuffer[packetlen] = udpbuffer[i];
                        i++;
                        packetlen++;
                    } else {
                        i++; // petame to escape charakter
                        packetBuffer[packetlen] = (byte) (udpbuffer[i] ^ GDL90_ESCAPEBYTE);
                        i++;
                        packetlen++;
                    }
                } //while

                if (packetlen >=3) { // id + crc

                    byte packetid=packetBuffer[0];
                    //  Log.d("AeolusUDPThread",String.format("Got id=%d",p.id));

                    if ((packetid == TALOSCOM_ID_LOG)||(packetid == TALOSCOM_ID_TOAST)) {
                        AeolusPacket p = new AeolusPacket();
                        p.id = packetBuffer[0];
                        p.b=new byte[packetlen-3];
                        for (int j = 0; j<packetlen-3; j++)  p.b[j] = packetBuffer[j+1];
                        mHandler.obtainMessage(packetid, 6, -1, p).sendToTarget();
                    } else if (packetlen>= 35) { // 1 id + 8 bytes + 8 ts + 4 * 4 floats min = 33 + 2 crc = 35 bytes minimum
                        //   Log.d("AeolusUDPThread",String.format("Got id=%d  packetlen=%d >35",p.id,packetlen));

                        ByteBuffer b = ByteBuffer.wrap(packetBuffer, 1,  packetlen-3);// nbytes=packetlen - id - crc
                        b.order(ByteOrder.LITTLE_ENDIAN);
                        FloatBuffer fb = b.asFloatBuffer();
                        DoubleBuffer db = b.asDoubleBuffer();

                        AeolusPacket p = new AeolusPacket();
                        p.id = packetBuffer[0];
                        p.id =   packetBuffer[0];
                        p.b=new byte[8];
                        p.b[0] = packetBuffer[1];    //data byte 0
                        p.b[1] = packetBuffer[2];
                        p.b[2] = packetBuffer[3];
                        p.b[3] = packetBuffer[4];
                        p.b[4] = packetBuffer[5];
                        p.b[5] = packetBuffer[6];
                        p.b[6] = packetBuffer[7];    //data byte 6
                        p.b[7] = packetBuffer[8];    //data byte 8
                        p.ts = db.get(1);           //data bytes 8-15

                        int nfloats=(packetlen-3-16)/4;
                        p.f=new float[nfloats];
                        for (int j = 0; j<nfloats; j++)  p.f[j] = fb.get(j+4);    //data bytes 16-..

                        mHandler.obtainMessage(packetid, 6, -1, p).sendToTarget();
                    } else {
                        Log.d("AeolusUDPThread", String.format("ERROR: recieved %d bytes", packetlen));
                    } //if ((packetlen <=32
                } else { // if (packetlen >=3)
                    Log.d("AeolusUDPThread", String.format("ERROR: recieved %d bytes", packetlen));
                } //if (packetlen >=3)
            } // if ((i0 >= 0)&&((i1 - i0)> 1)){
            i0 = i1;
        }  // while
    }  // parseUDPBuffer
} // class
