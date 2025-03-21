package com.getcapacitor.community;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class NearbyHelper {

    // The most significant bits and the least significant bits or Bluetooth Base UUID.
    // See Bluetooth Core Specification 6.0 Vol.3, Part B, Section 2.5.1
    public static final long BLUETOOTH_BASE_UUID_MSB = 0x0000000000001000L;
    public static final long BLUETOOTH_BASE_UUID_LSB = 0x800000805f9b34fbL;

    public static final int ENDPOINT_ID_LENGTH = 4;

    private static final Random random = new Random();
    private static final char[] kEndpointIdChars = {
        'A',
        'B',
        'C',
        'D',
        'E',
        'F',
        'G',
        'H',
        'I',
        'J',
        'K',
        'L',
        'M',
        'N',
        'O',
        'P',
        'Q',
        'R',
        'S',
        'T',
        'U',
        'V',
        'W',
        'X',
        'Y',
        'Z',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        '0'
    };

    public static byte[] hash(byte[] data, Integer length) {
        byte[] output = new byte[0];

        try {
            output = MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException ignored) {}

        if (output.length < length) {
            return output;
        } else {
            byte[] truncated = new byte[length];
            System.arraycopy(output, 0, truncated, 0, length);
            return truncated;
        }
    }

    @Nullable
    public static UUID makeUUID(@Nullable byte[] data) {
        return makeUUID(data, false);
    }

    @Nullable
    public static UUID makeUUID(@Nullable byte[] data, boolean withBase) {
        if (data == null) return null;

        ByteBuffer byteBuffer = ByteBuffer.allocate(16);

        if (withBase && data.length <= 8) {
            byteBuffer.putLong(BLUETOOTH_BASE_UUID_MSB).putLong(BLUETOOTH_BASE_UUID_LSB);
            byteBuffer.rewind();
        }

        byteBuffer.put(data);

        long msb = byteBuffer.getLong(0);
        long lsb = byteBuffer.getLong(8);

        return new UUID(msb, lsb);
    }

    @Nullable
    public static byte[] makeBytes(@Nullable UUID uuid) {
        if (uuid == null) return null;

        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        return ByteBuffer.allocate(16).putLong(msb).putLong(lsb).array();
    }

    public record EndpointID() {
        @Nullable
        public static String fromUUID(@Nullable UUID uuid) {
            if (uuid == null) return null;

            byte[] array = makeBytes(uuid);

            if (array == null) return null;

            return new String(Arrays.copyOfRange(array, 0, ENDPOINT_ID_LENGTH));
        }

        @Nullable
        public static UUID toUUID(@Nullable String id) {
            if (id == null) return null;

            byte[] data = id.getBytes();
            assert data.length == ENDPOINT_ID_LENGTH : "name must be 4 characters in length";

            ByteBuffer byteBuffer = ByteBuffer.allocate(16).putLong(BLUETOOTH_BASE_UUID_MSB).putLong(BLUETOOTH_BASE_UUID_LSB);

            byteBuffer.rewind();
            byteBuffer.put(data);

            long msb = byteBuffer.getLong(0);
            long lsb = byteBuffer.getLong(8);

            return new UUID(msb, lsb);
        }

        @NonNull
        public static String fromString(@Nullable String name) {
            return fromBytes(name != null ? name.getBytes() : null);
        }

        @NonNull
        public static String fromBytes(@Nullable byte[] name) {
            StringBuilder endpointID = new StringBuilder(ENDPOINT_ID_LENGTH);

            byte[] input = new byte[1 + (name != null ? name.length : 0)];

            input[0] = (byte) (random.nextInt() & 0xff);

            if (name != null) {
                System.arraycopy(name, 0, input, 1, name.length);
            }

            byte[] data = hash(input, ENDPOINT_ID_LENGTH);

            for (byte c : data) {
                endpointID.append(kEndpointIdChars[(c & 0xff) % kEndpointIdChars.length]);
            }

            return endpointID.toString();
        }
    }
}
