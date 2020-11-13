package com.lisetckiy.lab3.newpack.peer.messaging;

import com.lisetckiy.lab3.parser.Utils;

public class HandshakeMessage extends Message{
    private byte[] length;
    private byte[] protocol;
    private byte[] reserved;
    private byte[] fileID;
    private byte[] peerID;

    public static class Builder{

        private HandshakeMessage object = new HandshakeMessage();

        public Builder infoHash(byte[] infoHash){
            this.object.fileID = infoHash;
            return this;
        }

        public Builder peerId(byte[] peerID){
            this.object.peerID = peerID;
            return this;
        }

        public HandshakeMessage build(){
            return object;
        }

        public byte[] generate(){
            return object.generate();
        }
    }

    public static Builder builder(){
        return new Builder();
    }


    /**
     * Create values of the fields according to the parameters
     * @param length byte[]
     * @param protocol byte[]
     * @param reserved byte[]
     * @param fileID byte[]
     * @param peerID byte[]
     */
    public HandshakeMessage(byte[] length, byte[] protocol, byte[] reserved,
                            byte[] fileID, byte[] peerID){
        super(-1, 0);
        this.length = length;
        this.protocol = protocol;
        this.reserved = reserved;
        this.fileID = fileID;
        this.peerID = peerID;
    }

    private HandshakeMessage(){
        this(new byte[]{19}, "BitTorrent protocol".getBytes(),
                new byte[]{0,0,0,0,0,0,0,0});
    };

//   /**
//     * Creates a HS message with the given infoHash and peerID and default values
//     * @param infoHash byte[]
//     * @param peerID byte[]
//     */
//    public HandshakeMessage(byte[] infoHash, byte[] peerID){
//        this(new byte[]{19}, "BitTorrent protocol".getBytes(),
//                new byte[]{0,0,0,0,0,0,0,0}, infoHash, peerID);
//    }

    /**
     * Create a HS message with all given parameters
     * @param length byte[]
     * @param protocol byte[]
     * @param reserved byte[]
     */
    public HandshakeMessage(byte[] length, byte[] protocol, byte[] reserved){
        super(-1,0);
        this.length = length;
        this.protocol = protocol;
        this.reserved = reserved;
//        this.fileID = fileID;
//        this.peerID = peerID;
    }
    /**
     * Return the length of the protocol string as a byte
     * @return byte
     */
    public byte getLength(){
        return length[0];
    }
    /**
     * Return the protocol string as a byte array
     * @return byte[]
     */
    public byte[] getProtocol(){
        return this.protocol;
    }
    /**
     * Return the reserved bytes as a byte array
     * @return byte[]
     */
    public byte[] getReserved(){
        return this.reserved;
    }
    /**
     * Return the infoHash as a byte array
     * @return byte[]
     */
    public byte[] getFileID(){
        return this.fileID;
    }
    /**
     * Return the peerID as a byte array
     * @return byte[]
     */
    public byte[] getPeerID(){
        return this.peerID;
    }

    /**
     * Set the values of the fields according to the parameters
     * @param length byte[]
     * @param protocol byte[]
     * @param reserved byte[]
     * @param fileID byte[]
     * @param peerID byte[]
     */
    public void setData(byte[] length, byte[] protocol, byte[] reserved,
                        byte[] fileID, byte[] peerID){
        this.length = length;
        this.protocol = protocol;
        this.reserved = reserved;
        this.fileID = fileID;
        this.peerID = peerID;
    }
    /**
     * Generate the byte array representing the whole message that can then be transmitted
     * @return byte[]
     */
    public byte[] generate(){
        return Utils.concat(this.length, Utils.concat(this.protocol, Utils.concat(this.reserved, Utils.concat(this.fileID, this.peerID))));
    }
    /**
     * Display the message in a readable format
     * @return String
     */
    public String toString(){
        String toString = "";
        toString += this.length[0]+"+";
        toString += new String(this.protocol)+"+";
        toString += Utils.byteArrayToByteString(this.reserved)+"+";
        toString += Utils.byteArrayToByteString(this.fileID)+"+";
        toString += Utils.byteArrayToByteString(this.peerID);

        return toString;
    }
}
