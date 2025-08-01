package com.mentos_koder.pairing;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mentos_koder.wire.PacketParser;


import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

public class PairingPacketParser extends PacketParser {

    private BlockingQueue<Pairingmessage.PairingMessage> mMessagesQueue;

    public PairingPacketParser(InputStream inputStream, BlockingQueue<Pairingmessage.PairingMessage> messagesQueue) {
        super(inputStream);
        mMessagesQueue = messagesQueue;
    }

    @Override
    public void messageBufferReceived(byte[] buf) {
        try {
            Pairingmessage.PairingMessage pairingMessage = Pairingmessage.PairingMessage.parseFrom(buf);
            if (pairingMessage.getStatus() == Pairingmessage.PairingMessage.Status.STATUS_OK) {
                mMessagesQueue.put(pairingMessage);
            }
        } catch (InvalidProtocolBufferException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
