package com.mentos_koder.wire;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class MessageManager {

//    private final Logger logger = LoggerFactory.getLogger(MessageManager.class);
    public ByteBuffer mPacketBuffer = ByteBuffer.allocate(65539);

    public byte[] addLengthAndCreate(byte[] message) {
        int length = message.length;
        mPacketBuffer.put((byte) length).put(message);
        byte[] buf = new byte[mPacketBuffer.position()];
        System.arraycopy(mPacketBuffer.array(), mPacketBuffer.arrayOffset(), buf, 0, mPacketBuffer.position());
        mPacketBuffer.clear();
        return buf;
    }


}
