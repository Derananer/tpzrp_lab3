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

import com.lisetckiy.lab3.download.PeerUpdateListener;
import com.lisetckiy.lab3.parser.BencodeDecoder;
import com.lisetckiy.lab3.parser.TorrentFile;
import com.lisetckiy.lab3.util.Utils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.event.EventListenerList;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PeerUpdater extends Thread {
    private LinkedHashMap<String, Peer> peerList;
    private byte[] id;
    private TorrentFile torrent;


    private long downloaded = 0;
    private long uploaded = 0;
    private long left = 0;
    private String event = "&event=started";
    private int listeningPort = 6881;

    private int interval = 150;
    private boolean first = true;
    private boolean end = false;


    private final EventListenerList listeners = new EventListenerList();

    public PeerUpdater(byte[] id, TorrentFile torrent) {
        peerList = new LinkedHashMap();
        this.id = id;
        this.torrent = torrent;
        this.left = torrent.total_length;
        this.setDaemon(true);
    }

    public void setListeningPort(int port) {
        this.listeningPort = port;
    }

    /**
     * Sets the # of bytes still to download
     *
     * @param left long
     */
    public void setLeft(long left) {
        this.left = left;
    }

    /**
     * Update the parameters for future tracker communication
     *
     * @param dl    int
     * @param ul    int
     * @param event String
     */
    public synchronized void updateParameters(int dl, int ul, String event) {
        synchronized (this) {
            this.downloaded += dl;
            this.uploaded += ul;
            this.left -= dl;
            this.event = event;
        }
    }

    /**
     * Thread method that regularly contact the tracker and process its response
     */
    public void run() {
        int tryNB = 0;
        byte[] b = new byte[0];
        while (!this.end) {
            tryNB++;

            this.peerList = this.processResponse(this.contactTracker(id,
                                                                     torrent, this.downloaded,
                                                                     this.uploaded,
                                                                     this.left, this.event
                                                                    )
                                                );
            if (peerList != null) {
                if (first) {
                    this.event = "";
                    first = false;
                }
                tryNB = 0;
                this.fireUpdatePeerList(this.peerList);
                try {
                    synchronized (b) {
                        b.wait(interval * 1000);
                    }
                } catch (InterruptedException ie) {
                    log.error(ie.getMessage(), ie);
                }
            } else {
                try {
                    synchronized (b) {
                        b.wait(2000);
                    }
                } catch (InterruptedException ie) {
                    log.error(ie.getMessage(), ie);
                }
            }
        }
    }

    /**
     * Process the map representing the tracker response, which should contain
     * either an error message or the peers list and other information such as
     * the interval before next update, aso
     *
     * @param m The tracker response as a Map
     * @return LinkedHashMap A HashMap containing the peers and their ID as keys
     */
    public synchronized LinkedHashMap<String, Peer> processResponse(Map m) {
        LinkedHashMap<String, Peer> l = null;
        if (m != null) {
            if (m.containsKey("failure reason")) {
                this.fireUpdateFailed(0, "The tracker returns the following error message:" + "\t'" + new String((byte[]) m.get("failure reason")) + "'");
                return null;
            } else {
                if (((Long) m.get("interval")).intValue() < this.interval)
                    this.interval = ((Long) m.get("interval")).intValue();
                else
                    this.interval *= 2;

                Object peers = m.get("peers");
                ArrayList peerList = new ArrayList();
                l = new LinkedHashMap<String, Peer>();
                if (peers instanceof List) {
                    peerList.addAll((List) peers);
                    if (peerList != null && peerList.size() > 0) {
                        for (int i = 0; i < peerList.size(); i++) {
                            String peerID = new String((byte[]) ((Map) (peerList.get(i))).get("peer_id"));
                            String ipAddress = new String((byte[]) ((Map) (peerList.get(i))).get("ip"));
                            int port = ((Long) ((Map) (peerList.get(i))).get("port")).intValue();
                            Peer p = new Peer(peerID, ipAddress, port);
                            l.put(p.toString(), p);
                        }
                    }
                } else if (peers instanceof byte[]) {
                    byte[] p = ((byte[]) peers);
                    for (int i = 0; i < p.length; i += 6) {
                        Peer peer = new Peer();
                        peer.setIP(Utils.byteToUnsignedInt(p[i]) + "." +
                                           Utils.byteToUnsignedInt(p[i + 1]) + "." +
                                           Utils.byteToUnsignedInt(p[i + 2]) + "." +
                                           Utils.byteToUnsignedInt(p[i + 3]));
                        peer.setPort(Utils.byteArrayToInt(Utils.subArray(p, i + 4, 2)));
                        l.put(peer.toString(), peer);
                    }
                }
            }
            return l;
        } else
            return null;
    }

    /**
     * Contact the tracker according to the HTTP/HTTPS tracker protocol and using
     * the information in the TorrentFile.
     */
    public synchronized Map contactTracker(byte[] id, TorrentFile t, long dl, long ul, long left, String event) {
        try {
            URL source = new URL(t.announceURL + "?info_hash=" +
                                         t.info_hash_as_url + "&peer_id=" +
                                         Utils.byteArrayToURLString(id) + "&port=" +
                                         this.listeningPort +
                                         "&downloaded=" + dl + "&uploaded=" + ul +
                                         "&left=" + left +
                                         "&numwant=100&compact=1" + event);
            log.info("Contact Tracker. URL source = " + source);   //DAVID
            URLConnection uc = source.openConnection();
            InputStream is = uc.getInputStream();

            BufferedInputStream bis = new BufferedInputStream(is);

            // Decode the tracker bencoded response
            Map m = BencodeDecoder.decodeS(bis);
            log.info(m.toString());
            bis.close();
            is.close();

            return m;
        } catch (MalformedURLException murle) {
            log.error(murle.getMessage(), murle.getCause());
            this.fireUpdateFailed(2, "Tracker URL is not valid... Check if your data is correct and try again");
        } catch (UnknownHostException uhe) {
            log.error(uhe.getMessage(), uhe.getCause());
            this.fireUpdateFailed(3, "Tracker not available... Retrying...");
        } catch (IOException ioe) {
            this.fireUpdateFailed(4, "Tracker unreachable... Retrying");
        } catch (Exception e) {
            this.fireUpdateFailed(5, "Internal error");
        }
        return null;
    }

    /**
     * Stops the update process.
     */
    public void end() {
        this.event = "&event=stopped";
        this.end = true;
        this.contactTracker(this.id, this.torrent, this.downloaded,
                            this.uploaded, this.left, "&event=stopped"
                           );
    }

    public void addPeerUpdateListener(PeerUpdateListener listener) {
        listeners.add(PeerUpdateListener.class, listener);
    }


    public PeerUpdateListener[] getPeerUpdateListeners() {
        return listeners.getListeners(PeerUpdateListener.class);
    }


    protected void fireUpdatePeerList(LinkedHashMap l) {
        for (PeerUpdateListener listener : getPeerUpdateListeners()) {
            listener.updatePeerList(l);
        }
    }

    protected void fireUpdateFailed(int error, String message) {
        for (PeerUpdateListener listener : getPeerUpdateListeners()) {
            listener.updateFailed(error, message);
        }
    }
}