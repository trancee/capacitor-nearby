package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.EndpointID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings.Secure;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NearbyConfig {

    @NonNull
    final Context context;

    @NonNull
    String endpointID;

    @Nullable
    String endpointName;

    @Nullable
    byte[] endpointInfo;

    @Nullable
    String serviceID;

    /*
        30	00110000	0
        31	00110001	1
        32	00110010	2
        33	00110011	3
        34	00110100	4
        35	00110101	5
        36	00110110	6
        37	00110111	7
        38	00111000	8
        39	00111001	9

        41	01000001	A
        61	01100001	a
        42	01000010	B
        62	01100010	b
        43	01000011	C
        63	01100011	c
        44	01000100	D
        64	01100100	d
        45	01000101	E
        65	01100101	e
        46	01000110	F
        66	01100110	f
     */
    private static final byte[] nibbles = {
        0x00,
        0x01,
        0x02,
        0x03,
        0x04,
        0x05,
        0x06,
        0x07,
        0x08,
        0x09,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        0x0a,
        0x0b,
        0x0c,
        0x0d,
        0x0e,
        0x0f,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        0x0a,
        0x0b,
        0x0c,
        0x0d,
        0x0e,
        0x0f
    };

    public NearbyConfig(
        @NonNull Context context,
        @Nullable String endpointName,
        @Nullable byte[] endpointInfo,
        @Nullable String serviceID
    ) {
        this.context = context;

        // https://developer.android.com/identity/user-data-ids
        @SuppressLint("HardwareIds")
        var deviceID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

        byte[] bytes = new byte[deviceID.length() / 2];
        for (int i = 0, n = deviceID.length(); i < n; i++) {
            char c = deviceID.charAt(i);
            byte v = nibbles[c - '0'];
            bytes[i >> 1] |= (byte) (v << (((i % 2) == 0) ? 4 : 0));
        }
        this.endpointID = EndpointID.fromBytes(bytes);

        this.setEndpointName(endpointName);
        this.setEndpointInfo(endpointInfo);

        this.setServiceID(serviceID);
    }

    public void setEndpointName(@Nullable String endpointName) {
        this.endpointName = endpointName;
    }

    public void setEndpointInfo(@Nullable byte[] endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    public void setServiceID(@Nullable String serviceID) {
        this.serviceID = serviceID;
    }

    @Nullable
    public String getEndpointName() {
        return endpointName;
    }

    @Nullable
    public byte[] getEndpointInfo() {
        return endpointInfo;
    }

    @Nullable
    public String getServiceID() {
        return serviceID;
    }

    @NonNull
    String getEndpointID() {
        return endpointID;
    }

    @NonNull
    Context getContext() {
        return context;
    }
}
