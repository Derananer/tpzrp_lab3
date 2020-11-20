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

package com.lisetckiy.lab3.peer.messaging;

import com.lisetckiy.lab3.download.IncomingListener;
import com.lisetckiy.lab3.peer.PeerProtocol;
import com.lisetckiy.lab3.util.Utils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import javax.swing.event.EventListenerList;

@Slf4j
public class MessageReceiver extends Thread {

    private boolean run = true;
    private InputStream is = null;
    private DataInputStream dis = null;
    private boolean hsOK = false;
    private final EventListenerList listeners = new EventListenerList();


    public MessageReceiver(String id, InputStream is) throws IOException {
        this.setName("MR_" + id);
        this.is = is;
        this.dis = new DataInputStream(is);
    }

    private int read(byte[] data){
        try{
            this.dis.readFully(data);
        }catch(IOException ioe){
            return -1;
        }
        return data.length;
    }

    /**
     * Reads data from the inputstream, creates new messages according to the
     * received data and fires MessageReceived method of the listeners with the
     * new message in parameter. Loops as long as the 'run' variable is true
     */
    public void run() {
        //Message m = null;
        int read = 0;
        byte[] lengthHS = new byte[1];
        byte[] protocol = new byte[19];
        byte[] reserved = new byte[8];
        byte[] fileID = new byte[20];
        byte[] peerID = new byte[20];
        byte[] length = new byte[4];
        HandshakeMessage hs = new HandshakeMessage();
        PeerProtocolMessage mess = new PeerProtocolMessage();

        while (this.run) {
            int l = 1664;
            try {
                if (!hsOK) {
                    //log.info("Wait for hs");
                    if ((read = this.read(lengthHS)) > 0) {
                        for (int i = 0; i < 19; i++)
                            protocol[i] = (byte) is.read();
                        for (int i = 0; i < 8; i++)
                            reserved[i] = (byte) is.read();
                        for (int i = 0; i < 20; i++)
                            fileID[i] = (byte) is.read();
                        for (int i = 0; i < 20; i++)
                            peerID[i] = (byte) is.read();

                        hs.setData(lengthHS, protocol, reserved,
                                           fileID, peerID);
                        //this.hsOK = true;
                    } else {
                        hs = null;
                    }
                } else {
                    int id;
                    if ((read = this.read(length)) > 0) {
                        l = Utils.byteArrayToInt(length);
                        if (l == 0) {
                            mess.setData(PeerProtocol.KEEP_ALIVE);
                        } else {
                            id = is.read();
                            if(id == -1){
                                System.err.println("id");
                                mess = null;
                            }else{
                                if (l == 1)
                                    mess.setData(id + 1);
                                else {
                                    l = l - 1;
                                    byte[] payload = new byte[l];
                                    if (this.read(payload) > 0)
                                        mess.setData(id + 1, payload);
                                    payload = null;
                                }
                            }
                        }
                    } else {
                        mess = null;
                    }
                }
            } catch (IOException ioe) {
//                ioe.printStackTrace();
                log.error(ioe.getMessage(), ioe.getMessage());
                this.fireMessageReceived(null);
                return;
                // m = null;
            } catch (Exception e) {
                System.err.println(l+"Error in MessageReceiver..."+e.getMessage() +" " + e.toString());
                this.fireMessageReceived(null);
                return;
                // m = null;
            }

            if(!this.hsOK){
                this.fireMessageReceived(hs);
                this.hsOK = true;
            }else{
                this.fireMessageReceived(mess);
            }
            // m = null;
        }
        try{
            this.dis.close();
            this.dis = null;
        }catch(Exception e){
            log.error(e.getMessage(), e);
        }

    }

    public void addIncomingListener(IncomingListener listener) {
        listeners.add(IncomingListener.class, listener);
    }

    public IncomingListener[] getIncomingListeners() {
        return listeners.getListeners(IncomingListener.class);
    }

    protected void fireMessageReceived(Message m) {
        log.trace("Message received {}", m);
        for (IncomingListener listener : getIncomingListeners()) {
            listener.messageReceived(m);
        }
    }

    /**
     * Stops the current thread by completing the run() method
     */
    public void stopThread(){
        this.run = false;
    }
}
