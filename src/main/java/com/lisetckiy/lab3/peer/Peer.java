/*
 * Java Bittorrent API as its name indicates is a JAVA API that implements the Bittorrent Protocol
 * This project contains two packages:
 * 1. jBittorrentAPI is the "client" part, i.e. it implements all classes needed to publish
 *    files, share them and download them.
 *    This package also contains example classes on how a developer could create new applications.
 * 2. trackerBT is the "tracker" part, i.e. it implements a all classes needed to run
 *    a Bittorrent tracker that coordinates peers exchanges. *
 *
 * Copyright (C) 2007 Baptiste Dubuis, Artificial Intelligence Laboratory, EPFL
 *
 * This file is part of jbittorrentapi-v1.0.zip
 *
 * Java Bittorrent API is free software and a free user study set-up;
 * you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Java Bittorrent API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Bittorrent API; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @version 1.0
 * @author Baptiste Dubuis
 * To contact the author:
 * email: baptiste.dubuis@gmail.com
 *
 * More information about Java Bittorrent API:
 *    http://sourceforge.net/projects/bitext/
 */

package com.lisetckiy.lab3.peer;

import com.lisetckiy.lab3.util.Utils;

import java.util.*;

public class Peer {
    private String id;
    private String ip;
    private int port;
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

    public Peer() {
        this.hasPiece = new BitSet();
    }

    public Peer(String id, String ip, int port){
        this.lastDL = System.currentTimeMillis();
        this.lastUL = System.currentTimeMillis();
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.hasPiece = new BitSet();
    }

    public void setDLRate(int dl){
        this.dlrate += dl;
        this.downloaded += dl;
    }

    public String getID(){
        return this.id;
    }

    public String getIP(){
        return this.ip;
    }

    public int getPort(){
        return this.port;
    }

    public BitSet getHasPiece(){
        return this.hasPiece;
    }

    public void setID(String id){
        this.id = id;
    }

    public void setIP(String ip){
        this.ip = ip;
    }

    public void setPort(int port){
        this.port = port;
    }

    public boolean isInteresting(){
        return this.interesting;
    }

    public boolean isChoking(){
        return this.choking;
    }

    public void setInteresting(boolean i){
        this.interesting = i;
    }

    public void setChoking(boolean c){
        this.choking = c;
    }

    public void setHasPiece(byte[] bitfield){
        boolean[] b = Utils.byteArray2BitArray(bitfield);
        for(int i = 0; i < b.length; i++)
            this.hasPiece.set(i,b[i]);
    }

    public boolean isConnected(){
        return this.connected;
    }

    public void setConnected(boolean connectionStatus){
        this.connected = connectionStatus;
    }

    public boolean equals(Peer p){
        if(this.id == p.getID() && this.ip == p.getIP() && this.port == p.getPort())
            return true;
        return false;
    }

    public String toString(){
        return (this.ip+":" + this.port);
    }
}