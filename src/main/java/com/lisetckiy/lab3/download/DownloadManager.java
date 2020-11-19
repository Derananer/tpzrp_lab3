package com.lisetckiy.lab3.download;

import com.lisetckiy.lab3.util.Constants;
import com.lisetckiy.lab3.parser.TorrentFile;
import com.lisetckiy.lab3.peer.Peer;
import com.lisetckiy.lab3.peer.PeerProtocol;
import com.lisetckiy.lab3.peer.PeerUpdater;
import com.lisetckiy.lab3.peer.messaging.PeerProtocolMessage;
import com.lisetckiy.lab3.util.Utils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.*;

@Slf4j
public class DownloadManager implements DTListener, PeerUpdateListener, ConListenerInterface {

    // Client ID
    private byte[] clientID;

    private TorrentFile torrent = null;


    private int nbOfFiles = 0;
    private long length = 0;
    private long left = 0;
    private Piece[] pieceList;
    private final BitSet isComplete;
    private final BitSet isRequested;
    private int nbPieces;
    private RandomAccessFile[] output_files;

    private PeerUpdater pu = null;
    private ConnectionListener cl = null;

    private LinkedHashMap<String, Peer> peerList = null;
    private TreeMap<String, DownloadTask> task = null;
    private LinkedHashMap<String, BitSet> peerAvailabilies = null;

    LinkedHashMap unchoken = new LinkedHashMap<String, Integer>();
    private long lastUnchoking = 0;

    /**
     * Create a new manager according to the given torrent and using the client id provided
     *
     * @param torrent  TorrentFile
     * @param clientID byte[]
     */
    public DownloadManager(TorrentFile torrent, final byte[] clientID) {
        this.clientID = clientID;
        this.peerList = new LinkedHashMap<String, Peer>();
        //this.peerList = new LinkedList<Peer>();
        this.task = new TreeMap<String, DownloadTask>();
        this.peerAvailabilies = new LinkedHashMap<String, BitSet>();

        this.torrent = torrent;
        this.nbPieces = torrent.piece_hash_values_as_binary.size();
        this.pieceList = new Piece[this.nbPieces];
        this.nbOfFiles = this.torrent.length.size();

        this.isComplete = new BitSet(nbPieces);
        this.isRequested = new BitSet(nbPieces);
        this.output_files = new RandomAccessFile[this.nbOfFiles];

        this.length = this.torrent.total_length;
        this.left = this.length;

        this.checkTempFiles();

        /**
         * Construct all the pieces with the correct length and hash value
         */
        int file = 0;
        int fileoffset = 0;
        for (int i = 0; i < this.nbPieces; i++) {
            TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>();
            int pieceoffset = 0;
            do {
                tm.put(file, fileoffset);
                if (fileoffset + this.torrent.pieceLength - pieceoffset >= (Integer) (torrent.length.get(file)) && i != this.nbPieces - 1) {
                    pieceoffset += ((Integer) (torrent.length.get(file))).intValue() - fileoffset;
                    file++;
                    fileoffset = 0;
                    if (pieceoffset == this.torrent.pieceLength)
                        break;
                } else {
                    fileoffset += this.torrent.pieceLength - pieceoffset;
                    break;
                }
            } while (true);
            pieceList[i] = new Piece(i, (i != this.nbPieces - 1) ? this.torrent.pieceLength : ((Long) (this.length % this.torrent.pieceLength)).intValue(), 16384, (byte[]) torrent.piece_hash_values_as_binary.get(i), tm);
            //log.info("Piece " + i + " is complete: " + this.testComplete(i));
            if (this.testComplete(i)) {
                this.setComplete(i, true);
                this.left -= this.pieceList[i].getLength();
            }
        }
        this.lastUnchoking = System.currentTimeMillis();
    }

    public boolean testComplete(int piece) {
        boolean complete = false;
        this.pieceList[piece].setBlock(0, this.getPieceFromFiles(piece));
        complete = this.pieceList[piece].verify();
        this.pieceList[piece].clearData();
        return complete;
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
            if (this.isComplete())
                log.info("\r\nSharing... Press Ctrl+C to stop client");
        }
        /*
                 new IOManager().readUserInput(
                "\r\n*****************************************\r\n" +
                "* Press ENTER to stop sharing the files *\r\n" +
                "*****************************************");
         */
    }

    /**
     * Create and start the peer updater to retrieve new peers sharing the file
     */
    public void startTrackerUpdate() {
        this.pu = new PeerUpdater(this.clientID, this.torrent);
        this.pu.addPeerUpdateListener(this);
        this.pu.setListeningPort(this.cl.getConnectedPort());
        this.pu.setLeft(this.left);
        this.pu.start();
    }

    /**
     * Stop the tracker updates
     */
    public void stopTrackerUpdate() {
        this.pu.end();
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
        this.cl = new ConnectionListener();
        if (this.cl.connect(minPort, maxPort)) {
            this.cl.addConListenerInterface(this);
            return true;
        } else {
            System.err.println("Could not create listening socket...");
            System.err.flush();
            return false;
        }
    }

    /**
     * Close all open files
     */
    public void closeTempFiles() {
        for (int i = 0; i < this.output_files.length; i++)
            try {
                this.output_files[i].close();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    /**
     * Check the existence of the files specified in the torrent and if necessary,
     * create them
     *
     * @return int
     * @todo Should return an integer representing some error message...
     */
    public synchronized int checkTempFiles() {
        String saveas = Constants.SAVEPATH; // Should be configurable
        if (this.nbOfFiles > 1)
            saveas += this.torrent.saveAs + "/";
        new File(saveas).mkdirs();
        for (int i = 0; i < this.nbOfFiles; i++) {
            File temp = new File(saveas + ((String) (this.torrent.name.get(i))));
            try {
                this.output_files[i] = new RandomAccessFile(temp, "rw");
                this.output_files[i].setLength((Integer) this.torrent.length.get(i));
            } catch (IOException ioe) {
                System.err.println("Could not create temp files");
                ioe.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Save a piece in the corresponding file(s)
     *
     * @param piece int
     */
    public synchronized void savePiece(int piece) {
        //int remaining = this.pieceList[piece].getLength();
        byte[] data = this.pieceList[piece].data();
        int remainingData = data.length;
        for (Iterator it = this.pieceList[piece].getFileAndOffset().keySet().iterator(); it.hasNext(); ) {
            try {
                Integer file = (Integer) (it.next());
                int remaining = (Integer) this.torrent.length.get(file.intValue()) - (Integer) (this.pieceList[piece].getFileAndOffset().get(file));
                this.output_files[file].seek((Integer) (this.pieceList[piece].getFileAndOffset().get(file)));
                this.output_files[file].write(data, data.length - remainingData, (remaining < remainingData) ? remaining : remainingData);
                remainingData -= remaining;
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
        data = null;
        this.pieceList[piece].clearData();
    }

    /**
     * Check if the current download is complete
     *
     * @return boolean
     */
    public synchronized boolean isComplete() {
        synchronized (this.isComplete) {
            return (this.isComplete.cardinality() == this.nbPieces);
        }
    }

    /**
     * Returns the number of pieces currently requested to peers
     *
     * @return int
     */
    public synchronized int cardinalityR() {
        return this.isRequested.cardinality();
    }

    /**
     * Returns the piece with the given index
     *
     * @param index The piece index
     * @return Piece The piece with the given index
     */
    public synchronized Piece getPiece(int index) {
        synchronized (this.pieceList) {
            return this.pieceList[index];
        }
    }

    /**
     * Check if the piece with the given index is complete and verified
     *
     * @param piece The piece index
     * @return boolean
     */
    public synchronized boolean isPieceComplete(int piece) {
        synchronized (this.isComplete) {
            return this.isComplete.get(piece);
        }
    }

    /**
     * Check if the piece with the given index is requested by a peer
     *
     * @param piece The piece index
     * @return boolean
     */
    public synchronized boolean isPieceRequested(int piece) {
        synchronized (this.isRequested) {
            return this.isRequested.get(piece);
        }
    }

    /**
     * Mark a piece as complete or not according to the parameters
     *
     * @param piece The index of the piece to be updated
     * @param is    True if the piece is now complete, false otherwise
     */
    public synchronized void setComplete(int piece, boolean is) {
        synchronized (this.isComplete) {
            this.isComplete.set(piece, is);
        }
    }

    /**
     * Mark a piece as requested or not according to the parameters
     *
     * @param piece The index of the piece to be updated
     * @param is    True if the piece is now requested, false otherwise
     */

    public synchronized void setRequested(int piece, boolean is) {
        synchronized (this.isRequested) {
            this.isRequested.set(piece, is);
        }
    }


    /**
     * Returns the index of the piece that could be downloaded by the peer in parameter
     *
     * @param id The id of the peer that wants to download
     * @return int The index of the piece to request
     */
    private synchronized int choosePiece2Download(String id) {
        synchronized (this.isComplete) {
            int index = 0;
            ArrayList<Integer> possible = new ArrayList<Integer>(this.nbPieces);
            for (int i = 0; i < this.nbPieces; i++) {
                if ((!this.isPieceRequested(i) || (this.isComplete.cardinality() > this.nbPieces - 3)) &&
                        //(this.isRequested.cardinality() == this.nbPieces)) &&
                        (!this.isPieceComplete(i)) &&
                        this.peerAvailabilies.get(id) != null) {
                    if (this.peerAvailabilies.get(id).get(i))
                        possible.add(i);
                }
            }
            //log.info(this.isRequested.cardinality()+" "+this.isComplete.cardinality()+" " + possible.size());
            if (possible.size() > 0) {
                Random r = new Random(System.currentTimeMillis());
                index = possible.get(r.nextInt(possible.size()));
                this.setRequested(index, true);
                return (index);
            }
            return -1;
        }
    }


    /**
     * Removes a task and peer after the task sends a completion message.
     * Completion can be caused by an error (bad request, ...) or simply by the
     * end of the connection
     *
     * @param id     Task idendity
     * @param reason Reason of the completion
     */
    public synchronized void taskCompleted(String id, int reason) {
        switch (reason) {
            case DownloadTask.CONNECTION_REFUSED:
                //System.err.println("Connection refused by host " + id);
                break;
            case DownloadTask.MALFORMED_MESSAGE:
                //System.err.println("Malformed message from " + id + ". Task ended...");
                break;
            case DownloadTask.UNKNOWN_HOST:
                //System.err.println("Connection could not be established to " + id + ". Host unknown...");
        }
        this.peerAvailabilies.remove(id);
        this.task.remove(id);
        this.peerList.remove(id);
        //System.err.flush();
    }

    /**
     * Received when a piece has been fully downloaded by a task. The piece might
     * have been corrupted, in which case the manager will request it again later.
     * If it has been successfully downloaded and verified, the piece status is
     * set to 'complete', a 'HAVE' message is sent to all connected peers and the
     * piece is saved into the corresponding file(s)
     *
     * @param peerID   String
     * @param i        int
     * @param complete boolean
     */
    public synchronized void pieceCompleted(String peerID, int i, boolean complete) {
        synchronized (this.isRequested) {
            this.isRequested.clear(i);
        }
        synchronized (this.isComplete) {
            if (complete && !this.isPieceComplete(i)) {
                pu.updateParameters(this.torrent.pieceLength, 0, "");
                this.isComplete.set(i, complete);
                float totaldl = (float) (((float) (100.0)) *
                        ((float) (this.isComplete.cardinality())) /
                        ((float) (this.nbPieces)));
                for (Iterator it = this.task.keySet().iterator(); it.hasNext(); )
                    try {
                        this.task.get(it.next()).ms.addMessageToQueue(new PeerProtocolMessage(PeerProtocol.HAVE, Utils.intToByteArray(i), 1));
                    } catch (NullPointerException npe) {
                    }
                log.info("Piece completed by " + peerID + " : " + i + " (Total dl = " + totaldl + "% )");
                this.savePiece(i);
                this.getPieceBlock(i, 0, 15000);
            } else {
            }
            if (this.isComplete.cardinality() == this.nbPieces) {
                log.info("Task completed");
                this.notify();
            }
        }
    }

    /**
     * Set the status of the piece to requested or not
     *
     * @param i         int
     * @param requested boolean
     */
    public synchronized void pieceRequested(int i, boolean requested) {
        this.isRequested.set(i, requested);
    }

    /**
     * Choose which of the connected peers should be unchoked and authorized to
     * upload from this client. A peer gets unchoked if it is not interested, or
     * if it is interested and has one of the 5 highest download rate among the
     * interested peers. \r\n Every 3 times this method is called, calls the
     * optimisticUnchoke method, which unchoke a peer no matter its download rate,
     * in a try to find a better source
     */
    private synchronized void unchokePeers() {
//        synchronized (this.task) {
//            int nbNotInterested = 0;
//            int nbDownloaders = 0;
//            int nbChoked = 0;
//            this.unchoken.clear();
//            List<Peer> l = new LinkedList<Peer>(this.peerList.values());
//            if (!this.isComplete())
//                Collections.sort(l, new DLRateComparator());
//            else
//                Collections.sort(l, new ULRateComparator());
//            for (Iterator it = l.iterator(); it.hasNext(); ) {
//                Peer p = (Peer) it.next();
//                if (p.getDLRate(false) > 0)
//                    log.info(p + " rate: " + p.getDLRate(true) / (1024 * 10) + "ko/s");
//                DownloadTask dt = this.task.get(p.toString());
//                if (nbDownloaders < 5 && dt != null) {
//                    if (!p.isInterested()) {
//                        this.unchoken.put(p.toString(), p);
//                        if (p.isChoked())
//                            dt.ms.addMessageToQueue(new Message_PP(PeerProtocol.UNCHOKE));
//                        p.setChoked(false);
//                        while (this.unchokeList.remove(p)) ;
//                        nbNotInterested++;
//                    } else if (p.isChoked()) {
//                        this.unchoken.put(p.toString(), p);
//                        dt.ms.addMessageToQueue(new Message_PP(PeerProtocol.UNCHOKE));
//                        p.setChoked(false);
//                        while (this.unchokeList.remove(p)) ;
//                        nbDownloaders++;
//                    }
//                } else {
//                    if (!p.isChoked()) {
//                        dt.ms.addMessageToQueue(new Message_PP(PeerProtocol.CHOKE));
//                        p.setChoked(true);
//                    }
//                    if (!this.unchokeList.contains(p))
//                        this.unchokeList.add(p);
//                    nbChoked++;
//                }
//                p = null;
//                dt = null;
//            }
//        }
//        this.lastUnchoking = System.currentTimeMillis();
//        if (this.optimisticUnchoke-- == 0) {
//            this.optimisticUnchoke();
//            this.optimisticUnchoke = 3;
//        }
    }

    /**
     * Received when a task is ready to download or upload. In such a case, if
     * there is a piece that can be downloaded from the corresponding peer, then
     * request the piece
     *
     * @param peerID String
     */
    public synchronized void peerReady(String peerID) {
        if (System.currentTimeMillis() - this.lastUnchoking > 10000)
            this.unchokePeers();
        int piece2request = this.choosePiece2Download(peerID);
        if (piece2request != -1)
            this.task.get(peerID).requestPiece(this.pieceList[piece2request]);
    }

    /**
     * Received when a peer request a piece. If the piece is available (which
     * should always be the case according to Bittorrent protocol) and we are
     * able and willing to upload, the send the piece to the peer
     *
     * @param peerID String
     * @param piece  int
     * @param begin  int
     * @param length int
     */
    public synchronized void peerRequest(String peerID, int piece, int begin, int length) {
        if (this.isPieceComplete(piece)) {
            DownloadTask dt = this.task.get(peerID);
            if (dt != null) {
                dt.ms.addMessageToQueue(new PeerProtocolMessage(PeerProtocol.PIECE,
                                                                Utils.concat(
                                                               Utils.intToByteArray(piece),
                                                               Utils.concat(Utils.intToByteArray(begin), this.getPieceBlock(piece, begin, length))
                                                                   )
                                        )
                                       );
                dt.peer.setULRate(length);
            }
            dt = null;
            this.pu.updateParameters(0, length, "");
        } else {
            try {
                this.task.get(peerID).end();
            } catch (Exception e) {
            }
            this.task.remove(peerID);
            this.peerList.remove(peerID);
            this.unchoken.remove(peerID);
        }

    }

    /**
     * Load piece data from the existing files
     *
     * @param piece int
     * @return byte[]
     */
    public synchronized byte[] getPieceFromFiles(int piece) {
        byte[] data = new byte[this.pieceList[piece].getLength()];
        int remainingData = data.length;
        for (Iterator it = this.pieceList[piece].getFileAndOffset().keySet().
                iterator(); it.hasNext(); ) {
            try {
                Integer file = (Integer) (it.next());
                int remaining = ((Integer) this.torrent.length.get(file.intValue())).
                                                                                            intValue()
                        -
                        ((Integer) (this.pieceList[piece].
                                                                 getFileAndOffset().
                                                                 get(file))).intValue();
                this.output_files[file.intValue()].seek(((Integer)
                        (this.pieceList[piece].getFileAndOffset().get(file))).
                                                                                     intValue());
                this.output_files[file.intValue()].read(data,
                                                        data.length - remainingData,
                                                        (remaining < remainingData) ? remaining : remainingData
                                                       );
                remainingData -= remaining;
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
        return data;
    }

    /**
     * Get a piece block from the existing file(s)
     *
     * @param piece  int
     * @param begin  int
     * @param length int
     * @return byte[]
     */
    public synchronized byte[] getPieceBlock(int piece, int begin, int length) {
        return Utils.subArray(this.getPieceFromFiles(piece), begin, length);
    }

    /**
     * Update the piece availabilities for a given peer
     *
     * @param peerID String
     * @param has    BitSet
     */
    public synchronized void peerAvailability(String peerID, BitSet has) {
        this.peerAvailabilies.put(peerID, has);
        BitSet interest = (BitSet) (has.clone());
        interest.andNot(this.isComplete);
        DownloadTask dt = this.task.get(peerID);
        if (dt != null) {
            if (interest.cardinality() > 0 &&
                    !dt.peer.isInteresting()) {
                dt.ms.addMessageToQueue(new PeerProtocolMessage(
                        PeerProtocol.INTERESTED, 2));
                dt.peer.setInteresting(true);
            }
        }
        dt = null;
    }

    public synchronized void connect(Peer p) {
        DownloadTask dt = new DownloadTask(p,
                                           this.torrent.info_hash_as_binary,
                                           this.clientID, true,
                                           this.getBitField()
        );
        dt.addDTListener(this);
        dt.start();
    }

    /**
     * Given the list in parameter, check if the peers are already present in
     * the peer list. If not, then add them and create a new task for them
     *
     * @param list LinkedHashMap
     */
    public synchronized void updatePeerList(LinkedHashMap list) {
        synchronized (this.task) {
            Set keyset = list.keySet();
            for (Iterator i = keyset.iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                if (!this.task.containsKey(key)) {
                    Peer p = (Peer) list.get(key);
                    this.peerList.put(p.toString(), p);
                    this.connect(p);
//                    break;
                }
            }
        }
        log.info("Peer List updated from tracker with " + list.size() + " peers");
    }

    /**
     * Called when an update try fail. At the moment, simply display a message
     *
     * @param error   int
     * @param message String
     */
    public void updateFailed(int error, String message) {
        System.err.println(message);
        System.err.flush();
    }

    /**
     * Add the download task to the list of active (i.e. Handshake is ok) tasks
     *
     * @param id String
     * @param dt DownloadTask
     */
    public synchronized void addActiveTask(String id, DownloadTask dt) {
        synchronized (this.task) {
            this.task.put(id, dt);
        }
    }

    /**
     * Called when a new peer connects to the client. Check if it is already
     * registered in the peer list, and if not, create a new DownloadTask for it
     *
     * @param socket Socket
     */
    public synchronized void connectionAccepted(Socket socket) {
        synchronized (this.task) {

            String id = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            if (!this.task.containsKey(id)) {
                DownloadTask dt = new DownloadTask(null, this.torrent.info_hash_as_binary, this.clientID, false, this.getBitField(), socket);
                dt.addDTListener(this);
                this.peerList.put(dt.getPeer().toString(), dt.getPeer());
                this.task.put(dt.getPeer().toString(), dt);
                dt.start();
            }
        }
    }

    /**
     * Compute the bitfield byte array from the isComplete BitSet
     *
     * @return byte[]
     */
    public byte[] getBitField() {
        int l = (int) Math.ceil((double) this.nbPieces / 8.0);
        byte[] bitfield = new byte[l];
        for (int i = 0; i < this.nbPieces; i++)
            if (this.isComplete.get(i)) {
                bitfield[i / 8] |= 1 << (7 - i % 8);
            }
        return bitfield;
    }

}