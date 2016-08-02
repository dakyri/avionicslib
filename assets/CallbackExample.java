static Callback AeolusDataCallback = new Callback() {
    public boolean handleMessage(android.os.Message msg) {
        int what = msg.what;
        String s;
        AeolusUDPThread.AeolusPacket p= (AeolusUDPThread.AeolusPacket) msg.obj;
        mDeviceAeolusLastTime_msec=SystemClock.elapsedRealtime();
        if (mDeviceAeolusStatus <= SENSOR_STATUS_OFF) return true;
        try {
            switch (what) {
                case AeolusUDPThread.TALOSCOM_ID_TOAST:
                    s = new String(p.b);
                    Log.i("Aeolus", String.format("Aeolus: %s", s));
                    App.toast(String.format("Aeolus: %s", s));
                    break;
                case AeolusUDPThread.TALOSCOM_ID_LOG:
                    s = new String(p.b);
                    Log.i("Aeolus", String.format("Aeolus: %s", s));
                    break;
                case AeolusUDPThread.TALOSCOM_ID_AEOLUSINFO:
                    App.aeoluscheck(100 * p.b[1] + 10 * p.b[2] + p.b[3], p.b[0]);
                    break;
                case AeolusUDPThread.TALOSCOM_ID_CALIBINFO:
                    break;
                case AeolusUDPThread.TALOSCOM_ID_DEBUG:
                    break;
                case AeolusUDPThread.TALOSCOM_ID_PRESSURE:
                    onSPChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.f[0]);
                    onAirspeedChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.f[2]);
                    break;
                case AeolusUDPThread.TALOSCOM_ID_GYRO:
                    // Log.d("AeolusDataCallback", String.format("SENSOR_MODE_GYRO %f, %f %f ",p.f[0], p.f[1], p.f[2]));
                    //aeolusiter=256*p.b[6]+p.b[7];
                    onGyroChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.f[0], p.f[1], p.f[2]);
                    mGyroCalibrationStatus = p.b[2];
                    mGyroCalibrationCount = (CALIBRATION_GYRO_COUNTMAX * p.b[3]) / 100;
                    break;
                case AeolusUDPThread.TALOSCOM_ID_ACCEL:
                    // //Log.d("AeolusDataCallback", String.format("TALOSCOM_ID_ACCEL %f, %f %f ",p.f[0], p.f[1], p.f[2]));
                    onAccelChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.f[0], p.f[1], p.f[2]);
                    mAccelCalibrationStatus = p.b[2]; // enbled
                    mAccelCalibrationCount = (CALIBRATION_ACCEL_COUNTMAX * p.b[3]) / 100;
                    mAccelCalibrationAHRSStatus = p.b[4];
                    mAccelCalibrationAHRSCount = (CALIBRATION_AHRS_COUNTMAX * p.b[5]) / 100;
                    mDeviceAeolus_calib_offset_rpy0 = p.f[3];
                    mDeviceAeolus_calib_offset_rpy1 = p.f[4];
                    mDeviceAeolus_calib_offset_rpy2 = p.f[5];
                    break;
                case AeolusUDPThread.TALOSCOM_ID_MAGNET:
                    onMagnetChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.f[0], p.f[1], p.f[2]);
                    break;
                case AeolusUDPThread.TALOSCOM_ID_TEMP:
                    //Log.d("AeolusDataCallback", "temp");
                    onTemperatureChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.f[0]);
                    break;
                case AeolusUDPThread.TALOSCOM_ID_GPS:
                    onGPSChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.f[0], p.f[1], p.f[2], p.f[3]);
                    break;
                case AeolusUDPThread.TALOSCOM_ID_RPY:
                    onKalmanChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.f[0], p.f[1], p.f[2], p.f[3], p.f[4]);
                    break;
                case AeolusUDPThread.TALOSCOM_ID_COMPASS:
                    onCompassChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.b[1], p.b[3], p.b[4], p.f[0]);
                    break;
                case AeolusUDPThread.TALOSCOM_ID_WIND:
                    onWindChanged(mDeviceAeolusLastTime_msec, p.ts, SENSOR_SOURCE_AEOLUS, p.f[0], p.f[1]);
                    break;
            }
            return true;
        }  catch (Exception e) {
            Log.d("EXCEPTION  ", "sensormaster.AeolusDataCallback:" + e.toString());
            return false;
        }
    } // handle message
}; // callback
