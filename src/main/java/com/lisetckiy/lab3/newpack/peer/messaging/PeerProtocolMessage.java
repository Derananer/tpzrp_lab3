package com.lisetckiy.lab3.newpack.peer.messaging;

import com.lisetckiy.lab3.parser.Bits;
import com.lisetckiy.lab3.parser.PeerProtocol;
import com.lisetckiy.lab3.parser.Utils;

public class PeerProtocolMessage extends Message{
    private byte[] length = new byte[4];
    private byte[] id = new byte[1];
    private byte[] payload;

    public PeerProtocolMessage(){
        super();
    }

    public PeerProtocolMessage(int type, int p) {
        super(type, p);
        this.setData(type);
    }

    public PeerProtocolMessage(int type){
        this(type, 0);
    }

    public PeerProtocolMessage(int type, byte[] payload, int p) {
        super(type, p);
        this.setData(type, payload);
    }

    public PeerProtocolMessage(int type, byte[] payload){
        this(type, payload, 0);
    }


    public byte[] getLength() {
        return this.length;
    }

    public byte[] getID() {
        return this.id;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public void setLength(byte[] length) {
        this.length = length;
    }

    public void setID(int id) {
        this.id[0] = (byte) id;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public void setData(int type) {
        this.type = type;
        switch (type) {
            case 0:
                this.length = new byte[] {0, 0, 0, 0};
                break;
            case 1:
                this.length = new byte[] {0, 0, 0, 1};
                this.id[0] = 0;
                break;
            case 2:
                this.length = new byte[] {0, 0, 0, 1};
                this.id[0] = 1;
                break;
            case 3:
                this.length = new byte[] {0, 0, 0, 1};
                this.id[0] = 2;
                break;
            case 4:
                this.length = new byte[] {0, 0, 0, 1};
                this.id[0] = 3;
                break;
        }
    }

    public void setData(int type, byte[] payload) {
        this.type = type;
        switch (type) {
            case 5:
                this.length = new byte[] {0, 0, 0, 5};
                this.id[0] = 4;
                this.payload = payload;
                break;
            case 6:
                this.length = Utils.intToByteArray(1 + payload.length);
                this.id[0] = 5;
                this.payload = payload;
                break;
            case 7:
                this.length = new byte[] {0, 0, 0, 13};
                this.id[0] = 6;
                this.payload = payload;
                break;
            case 8:
                this.length = Utils.intToByteArray(1 + payload.length);
                this.id[0] = 7;
                this.payload = payload;
                break;
            case 9:
                this.length = new byte[] {0, 0, 0, 13};
                this.id[0] = 8;
                this.payload = payload;
                break;
            case 10:
                this.length = new byte[] {0, 0, 0, 3};
                this.id[0] = 9;
                this.payload = payload;
                break;
        }
    }

    public void setData(byte[] length, byte id, byte[] payload) {
        this.length = length;
        this.id[0] = id;
        this.payload = payload;
    }

    public byte[] generate() {
        if (this.type > 4)
            return Utils.concat(Utils.concat(this.length, this.id),
                                this.payload);
        else if (this.type > 0)
            return Utils.concat(this.length, this.id);
        else
            return this.length;
    }

    public String toString() {
        String toString = "";

        int length = Utils.byteArrayToInt(this.length);
        toString += "<length=" + length + ">";
        if (length > 0) {
            toString += "<id=" + (int)this.id[0] + ">";
            if (length > 1) {
                switch(this.id[0]+1){
                    case PeerProtocol.HAVE:
                        toString += "<index=" + Utils.byteArrayToInt(this.payload) +
                                ">";
                        break;
                    case PeerProtocol.BITFIELD:
                        toString += "<bitfield="+(new Bits(this.payload))+">";
                        break;
                    case PeerProtocol.REQUEST:
                        toString += "<index=" + Utils.byteArrayToInt(Utils.subArray(this.payload,0,4)) +">";
                        toString += "<begin=" + Utils.byteArrayToInt(Utils.subArray(this.payload,4,4)) +">";
                        toString += "<length=" + Utils.byteArrayToInt(Utils.subArray(this.payload,8,4)) +">";
                        break;
                    case PeerProtocol.PIECE:
                        toString += "<index=" + Utils.byteArrayToInt(Utils.subArray(this.payload,0,4)) +">";
                        toString += "<begin=" + Utils.byteArrayToInt(Utils.subArray(this.payload,4,4)) +">";
                        toString += "<block= "+(this.payload.length-8)+"bytes>";
                        break;
                    case PeerProtocol.CANCEL:
                        toString += "<index=" + Utils.byteArrayToInt(Utils.subArray(this.payload,0,4)) +">";
                        toString += "<begin=" + Utils.byteArrayToInt(Utils.subArray(this.payload,4,4)) +">";
                        toString += "<length=" + Utils.byteArrayToInt(Utils.subArray(this.payload,8,4)) +">";
                        break;
                    case PeerProtocol.PORT:
                        break;
                }
                // + ">";
            }
        }
        return toString;
    }
}
