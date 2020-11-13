package com.lisetckiy.lab3.newpack;

//import com.lisetckiy.lab3.parser.ConnectionListener;

import com.lisetckiy.lab3.newpack.peer.ConnectionListener;
import com.lisetckiy.lab3.newpack.peer.Peer;
import com.lisetckiy.lab3.newpack.peer.PeerInfo;
import com.lisetckiy.lab3.newpack.peer.PeerUpdaterNew;
import com.lisetckiy.lab3.parser.TorrentFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

@Slf4j
public class FileDownloadManager {

    // Client ID
    private byte[] clientID;

    private TorrentFile torrent;
    private PeerUpdaterNew peerUpdater;
    private ConnectionListener connectionListener;

    private HashMap<String, Peer> peers;


    public FileDownloadManager(byte[] clientID, TorrentFile torrent) {
        this.clientID = clientID;
        this.torrent = torrent;
        this.peers = new HashMap<>();
    }

    /**
     * Create and start the peer updater to retrieve new peers sharing the file
     */
    public void startTrackerUpdate() {
        log.info("Perform starting Tracker Update.");
        this.peerUpdater = new PeerUpdaterNew(this.clientID, this.torrent);
//        this.peerUpdater.addPeerUpdateListener(this);
        this.peerUpdater.setListeningPort(this.connectionListener.getConnectedPort());
        this.peerUpdater.setDownloadManager(this);
//        this.peerUpdater.setLeft(this.left);
        this.peerUpdater.start();
    }

    /**
     * Create the ConnectionListener to accept incoming connection from peers
     *
     * @param minPort The minimal port number this client should listen on
     * @param maxPort The maximal port number this client should listen on
     * @return True if the listening process is started, false else
     * @todo Should it really be here? Better create it in the implementation
     */
    public boolean startListening(int minPort, int maxPort) {
        this.connectionListener = new ConnectionListener();
        if (this.connectionListener.connect(minPort, maxPort)) {
//            this.connectionListener.addConListenerInterface(this);
            return true;
        } else {
            log.error("Could not create listening socket...");
            return false;
        }
    }

    /**
     * Given the list in parameter, check if the peers are already present in
     * the peer list. If not, then add them and create a new task for them
     *
     * @param list LinkedHashMap
     */
    public synchronized void updatePeerList(LinkedHashMap<String, PeerInfo> list) {

        //this.lastUnchoking = System.currentTimeMillis();
//        synchronized (this.task) {
        //this.peerList.putAll(list);
//            Set keyset = list.keySet();
//            for (Iterator i = keyset.iterator(); i.hasNext(); ) {
//                String key = (String) i.next();
        for (Map.Entry<String, PeerInfo> entry : list.entrySet()) {
            Peer peer = null;
            try {
                peer = Peer.builder()
                           .clientId(clientID)
                           .info(entry.getValue())
                           .torrentFile(torrent)
                           .build()
                           .connect();
            } catch (IOException e) {
                log.error("Error while connecting to peer: {}", e.getMessage());
            }
            if(peer != null)
            this.peers.putIfAbsent(entry.getKey(), peer);
        }
//                if (!this.task.containsKey(key)) {
//                    Peer p = (Peer) list.get(key);
//                    this.peerList.put(p.toString(), p);
//                    this.connect(p);
//                }
//            }
//        }
//        System.out.println("Peer List updated from tracker with " + list.size() + " peers");
    }

    /**
     * Periodically call the unchokePeers method. This is an infinite loop.
     * User have to exit with Ctrl+C, which is not good... Todo is change this
     * method...
     */
    public void blockUntilCompletion() {
        byte[] b = new byte[0];

        while (true) {
            try {
                synchronized (b) {
                    b.wait(10000);
//                    this.unchokePeers();
                    b.notifyAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
//            if (this.isComplete())
//                System.out.println("\r\nSharing... Press Ctrl+C to stop client");
        }
        /*
                 new IOManager().readUserInput(
                "\r\n*****************************************\r\n" +
                "* Press ENTER to stop sharing the files *\r\n" +
                "*****************************************");
         */
    }

    /**
     * Stop the tracker updates
     */
    public void stopTrackerUpdate() {
        this.peerUpdater.end();
    }

}
