package com.lisetckiy.lab3.newpack.peer;

import com.lisetckiy.lab3.parser.Peer;
import com.lisetckiy.lab3.parser.Utils;

import java.util.BitSet;

public class PeerInfo {

    private String id;
    private String ip;
    private int port;
    private boolean interested = false;
    private boolean choked = true;
    private boolean interesting = false;
    private boolean choking = true;
    private BitSet hasPiece;
    private int downloaded = 0;
    private float dlrate = 0;
    private long lastDL = 0;
    private float ulrate = 0;
    private long lastUL = 0;
    private int uploaded = 0;
    private boolean connected = false;

    public PeerInfo() {
        this.hasPiece = new BitSet();
    }

    public PeerInfo(String id, String ip, int port){
        this.lastDL = System.currentTimeMillis();
        this.lastUL = System.currentTimeMillis();
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.hasPiece = new BitSet();
    }

    public void resetDL(){
        this.dlrate = 0;
        this.lastDL = System.currentTimeMillis();
    }

    public void resetUL(){
        this.ulrate = 0;
        this.lastUL = System.currentTimeMillis();
    }

    /**
     * Returns the number of bytes downloaded since the last reset
     * @param reset true if the download rate should be reset
     * @return float
     */
    public float getDLRate(boolean reset){
        if(reset){
            float tmp = this.dlrate;
            this.dlrate = 0;
            return tmp;
        }else
            return this.dlrate;

    }

    /**
     * Returns the number of bytes uploaded since the last reset.
     * @param reset true if the download rate should be reset
     * @return float
     */
    public float getULRate(boolean reset){
        if(reset){
            float tmp = this.ulrate;
            this.ulrate = 0;
            return tmp;
        }else
            return this.ulrate;
    }

    /**
     * Returns the total number of bytes downloaded from this peer
     * @return int
     */
    public int getDL(){
        return this.downloaded;
    }

    /**
     * Returns the total number of bytes uploaded to this peer
     * @return int
     */
    public int getUL(){
        return this.uploaded;
    }

    /**
     * Updates the downloaded values
     * @param dl int
     */
    public void setDLRate(int dl){
        this.dlrate += dl;
        this.downloaded += dl;
    }

    /**
     * Updates the uploaded values
     * @param ul int
     */
    public void setULRate(int ul){
        this.ulrate += ul;
        this.uploaded += ul;
    }

    /**
     * Returns the id of this peer
     * @return String
     */
    public String getID(){
        return this.id;
    }

    /**
     * Returns the IP address of this peer
     * @return String
     */
    public String getIP(){
        return this.ip;
    }

    /**
     * Returns the listening port of this peer
     * @return int
     */
    public int getPort(){
        return this.port;
    }

    /**
     * Returns the pieces availability of this peer
     * @return BitSet
     */
    public BitSet getHasPiece(){
        return this.hasPiece;
    }

    /**
     * Sets the id of this peer
     * @param id String
     */
    public void setID(String id){
        this.id = id;
    }

    /**
     * Sets the IP address of this peer
     * @param ip String
     */
    public void setIP(String ip){
        this.ip = ip;
    }

    /**
     * Sets the listening port of this peer
     * @param port int
     */
    public void setPort(int port){
        this.port = port;
    }
    /**
     * Returns if this peer is interested or not
     * @return boolean
     */
    public boolean isInterested(){
        return this.interested;
    }

    /**
     * Returns if this peer is choked or not
     * @return boolean
     */
    public boolean isChoked(){
        return this.choked;
    }

    /**
     * Returns if this peer is interesting or not
     * @return boolean
     */
    public boolean isInteresting(){
        return this.interesting;
    }

    /**
     * Returns if this peer is choking or not
     * @return boolean
     */
    public boolean isChoking(){
        return this.choking;
    }

    /**
     * Sets if this peer is intereseted or not
     * @param i boolean
     */
    public void setInterested(boolean i){
        this.interested = i;
    }

    /**
     * Sets if this peer is choked or not
     * @param c boolean
     */
    public void setChoked(boolean c){
        this.choked = c;
    }

    /**
     * Sets if this peer is interesting or not
     * @param i boolean
     */
    public void setInteresting(boolean i){
        this.interesting = i;
    }

    /**
     * Sets if this peer is choking or not
     * @param c boolean
     */
    public void setChoking(boolean c){
        this.choking = c;
    }

    /**
     * Updates this peer availability according to the received bitfield
     * @param bitfield byte[]
     */
    public void setHasPiece(byte[] bitfield){
        boolean[] b = Utils.byteArray2BitArray(bitfield);
        for(int i = 0; i < b.length; i++)
            this.hasPiece.set(i,b[i]);
    }

    /**
     * Updates the availability of the piece in parameter
     * @param piece int
     * @param has boolean
     */
    public void setHasPiece(int piece, boolean has){
        this.hasPiece.set(piece, has);
    }

    public boolean isConnected(){
        return this.connected;
    }

    public void setConnected(boolean connectionStatus){
        this.connected = connectionStatus;
    }

    /**
     * Compares if this peer is equal to the peer in parameter
     * @param p Peer
     * @return boolean
     */
    public boolean equals(Peer p){
        if(this.id == p.getID() && this.ip == p.getIP() && this.port == p.getPort())
            return true;
        return false;
    }

    /**
     * Returns this peer characteristics in the form <ip address>:<port>
     * @return String
     */
    public String toString(){
        return (this.ip+":" + this.port);
    }
}
