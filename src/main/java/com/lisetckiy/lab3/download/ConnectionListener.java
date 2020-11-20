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

package com.lisetckiy.lab3.download;

import lombok.extern.slf4j.Slf4j;

import javax.swing.event.EventListenerList;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


@Slf4j
public class ConnectionListener extends Thread {
    private ServerSocket ss = null;
    private int minPort = -1;
    private int maxPort = -1;
    private int connectedPort = -1;
    private final EventListenerList listeners = new EventListenerList();
    private boolean acceptConnection = true;

    public ConnectionListener() {
    }


    public int getConnectedPort() {
        return this.connectedPort;
    }

    /**
     * Try to create a server socket for remote peers to connect on within the
     * specified port range
     */
    public boolean connect(int minPort, int maxPort) {
        this.minPort = minPort;
        this.maxPort = maxPort;
        for (int i = minPort; i <= maxPort; i++)
            try {
                this.ss = new ServerSocket(i);
                this.connectedPort = i;
                this.setDaemon(true);
                this.start();
                return true;
            } catch (IOException ioe) {
            }
        return false;
    }

    public void run() {
        byte[] b = new byte[0];
        try {
            while (true) {
                if (this.acceptConnection) {
                    this.fireConnectionAccepted(ss.accept());
                    sleep(1000);
                } else {
                    synchronized (b) {
                        log.info("No more connection accepted for the moment...");
                        b.wait();
                    }
                }
            }
        } catch (IOException ioe) {
            log.error("Error in connection listener: " + ioe.getMessage(), ioe);
        } catch (InterruptedException ie) {
            log.error("Error in connection listener: " + ie.getMessage(), ie);
        }
    }

    public void addConListenerInterface(ConListenerInterface listener) {
        listeners.add(ConListenerInterface.class, listener);
    }

    public ConListenerInterface[] getConListenerInterfaces() {
        return listeners.getListeners(ConListenerInterface.class);
    }

    /**
     * Method used to send message to all object currently listening on this thread
     * when a new connection has been accepted. It provides the socket the connection
     * is bound to.
     *
     * @param s Socket
     */
    protected void fireConnectionAccepted(Socket s) {
        for (ConListenerInterface listener : getConListenerInterfaces()) {
            listener.connectionAccepted(s);
        }
    }

}
