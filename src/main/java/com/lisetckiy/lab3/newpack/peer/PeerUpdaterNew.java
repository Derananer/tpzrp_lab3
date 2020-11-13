package com.lisetckiy.lab3.newpack.peer;

import com.lisetckiy.lab3.newpack.FileDownloadManager;
import com.lisetckiy.lab3.parser.BDecoder;
import com.lisetckiy.lab3.parser.TorrentFile;
import com.lisetckiy.lab3.parser.Utils;
import lombok.extern.slf4j.Slf4j;

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
public class PeerUpdaterNew extends Thread {

    private int interval = 150;
    private int listeningPort = 6881;
    private long downloaded = 0;
    private long uploaded = 0;
    private long left = 0;
    private String event = "&event=started";
    private boolean end = false;
    private boolean first = true;

    private FileDownloadManager downloadManager;

    private TorrentFile torrent;
    private byte[] id;


    private LinkedHashMap<String, PeerInfo> peerInfoList;


    public PeerUpdaterNew(byte[] id, TorrentFile torrent) {
        peerInfoList = new LinkedHashMap<>();
        this.id = id;
        this.torrent = torrent;
        this.left = torrent.total_length;
        this.setDaemon(true);
    }

    public void setDownloadManager(FileDownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void setListeningPort(int port) {
        this.listeningPort = port;
    }

    /**
     * Thread method that regularly contact the tracker and process its response
     */
    public void run() {
        int tryNB = 0;
        byte[] b = new byte[0];
        while (!this.end) {
            tryNB++;

            this.peerInfoList = this.processResponse(this.contactTracker(id, torrent, this.downloaded, this.uploaded, this.left, this.event));
            if (peerInfoList != null) {
                if (first) {
                    this.event = "";
                    first = false;
                }
                tryNB = 0;
                this.fireUpdatePeerList(this.peerInfoList);
                try {
                    synchronized (b) {
                        b.wait(interval * 1000);
                    }
                } catch (InterruptedException ie) {
                }
            } else {
                try {
                    synchronized (b) {
                        b.wait(2000);
                    }
                } catch (InterruptedException ie) {
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
    public synchronized LinkedHashMap<String, PeerInfo> processResponse(Map m) {
        log.info("Start processing response.");
        LinkedHashMap<String, PeerInfo> l = null;
        if (m != null) {
            if (m.containsKey("failure reason")) {
//                this.fireUpdateFailed(0, "The tracker returns the following error message:" + "\t'" + new String((byte[]) m.get("failure reason")) + "'");
                return null;
            } else {
                if (((Long) m.get("interval")).intValue() < this.interval)
                    this.interval = ((Long) m.get("interval")).intValue();
                else
                    this.interval *= 2;

                Object peers = m.get("peers");
                l = new LinkedHashMap<>();
                if (peers instanceof List<?>) {
                    ArrayList<Object> peerList = new ArrayList<>((List<?>) peers);
                    if (peerList.size() > 0) {
                        for (Object o : peerList) {
                            String peerID = new String((byte[]) ((Map) o).get("peer_id"));
                            String ipAddress = new String((byte[]) ((Map) o).get("ip"));
                            int port = ((Long) ((Map) o).get("port")).intValue();
                            PeerInfo p = new PeerInfo(peerID, ipAddress, port);
                            l.put(p.toString(), p);
                        }
                    }
                } else if (peers instanceof byte[]) {
                    byte[] p = ((byte[]) peers);
                    for (int i = 0; i < p.length; i += 6) {
                        PeerInfo peer = new PeerInfo();
                        peer.setIP(Utils.byteToUnsignedInt(p[i]) + "." +
                                           Utils.byteToUnsignedInt(p[i + 1]) + "." +
                                           Utils.byteToUnsignedInt(p[i + 2]) + "." +
                                           Utils.byteToUnsignedInt(p[i + 3]));
                        peer.setPort(Utils.byteArrayToInt(Utils.subArray(p, i + 4, 2)));
                        l.put(peer.toString(), peer);
                    }
                }
            }
            log.info("End processing response: {}", l);
            return l;
        } else {
            log.info("End processing response: message = null");
            return null;
        }
    }

    /**
     * Contact the tracker according to the HTTP/HTTPS tracker protocol and using
     * the information in the TorrentFile.
     *
     * @param id    byte[]
     * @param t     TorrentFile
     * @param dl    long
     * @param ul    long
     * @param left  long
     * @param event String
     * @return A Map containing the decoded tracker response
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
            log.info("Contact Tracker. URL_source={}", source);   //DAVID
            URLConnection uc = source.openConnection();
            InputStream is = uc.getInputStream();

            BufferedInputStream bis = new BufferedInputStream(is);

            // Decode the tracker bencoded response
            Map m = BDecoder.decodeS(bis);
            log.info("Decoded the tracker bencoded response: {}", m);
            System.out.println(m);
            bis.close();
            is.close();

            return m;
        } catch (MalformedURLException murle) {
            log.error(murle.getMessage(), murle.getCause());
//            this.fireUpdateFailed(2, "Tracker URL is not valid... Check if your data is correct and try again");
        } catch (UnknownHostException uhe) {
            log.error(uhe.getMessage(), uhe.getCause());
//            this.fireUpdateFailed(3, "Tracker not available... Retrying...");
        } catch (IOException ioe) {
//            this.fireUpdateFailed(4, "Tracker unreachable... Retrying");
        } catch (Exception e) {
//            this.fireUpdateFailed(5, "Internal error");
        }
        return null;
    }

    /**
     * Sends a message to all listeners with a HashMap containg the list of all
     * peers present in the last tracker response
     *
     * @param peersInfo LinkedHashMap
     */
    private void fireUpdatePeerList(LinkedHashMap<String, PeerInfo> peersInfo) {
        log.info("Fire update Peer list.");
        this.downloadManager.updatePeerList(peersInfo);
//        for (PeerUpdateListener listener : getPeerUpdateListeners()) {
//            listener.updatePeerList(l);
//        }
    }

    public void end() {
        this.end = true;
    }
}
