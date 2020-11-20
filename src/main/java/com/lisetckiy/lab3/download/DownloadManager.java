package com.lisetckiy.lab3.download;

import com.lisetckiy.lab3.parser.TorrentFile;
import com.lisetckiy.lab3.peer.Peer;
import com.lisetckiy.lab3.peer.PeerProtocol;
import com.lisetckiy.lab3.peer.PeerUpdater;
import com.lisetckiy.lab3.peer.messaging.PeerProtocolMessage;
import com.lisetckiy.lab3.util.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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

    /**
     * Create a new manager according to the given torrent and using the client id provided
     */
    public DownloadManager(TorrentFile torrent, final byte[] clientID) {
        this.clientID = clientID;
        this.peerList = new LinkedHashMap<>();
        this.task = new TreeMap<>();
        this.peerAvailabilies = new LinkedHashMap<>();

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
            TreeMap<Integer, Integer> tm = new TreeMap<>();
            int pieceoffset = 0;
            do {
                tm.put(file, fileoffset);
                if (fileoffset + this.torrent.pieceLength - pieceoffset >= (Integer) (torrent.length.get(file)) && i != this.nbPieces - 1) {
                    pieceoffset += (Integer) (torrent.length.get(file)) - fileoffset;
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
            if (this.testComplete(i)) {
                this.setComplete(i, true);
                this.left -= this.pieceList[i].getLength();
            }
        }
    }

    public boolean testComplete(int piece) {
        boolean complete = false;
        this.pieceList[piece].setBlock(0, this.getPieceFromFiles(piece));
        complete = this.pieceList[piece].verify();
        this.pieceList[piece].clearData();
        return complete;
    }

    public void blockUntilCompletion() {
        byte[] b = new byte[0];

        while (true) {
            try {
                synchronized (b) {
                    b.wait(10000);
                    b.notifyAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (this.isComplete())
                log.info("\r\nSharing... Press Ctrl+C to stop client");
        }
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

    public void stopTrackerUpdate() {
        this.pu.end();
    }

    /**
     * Create the ConnectionListener to accept incoming connection from peers
     */
    public boolean startListening(int minPort, int maxPort) {
        this.cl = new ConnectionListener();
        if (this.cl.connect(minPort, maxPort)) {
            this.cl.addConListenerInterface(this);
            return true;
        } else {
            log.error("Could not create listening socket...");
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
                log.error(e.getMessage(), e);
            }
    }

    /**
     * Check the existence of the files specified in the torrent and if necessary,
     * create them
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
                log.error("Could not create temp files", ioe);
            }
        }
        return 0;
    }

    /**
     * Save a piece in the corresponding file(s)
     */
    public synchronized void savePiece(int piece) {
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
     */
    public synchronized boolean isComplete() {
        synchronized (this.isComplete) {
            return (this.isComplete.cardinality() == this.nbPieces);
        }
    }

    /**
     * Check if the piece with the given index is complete and verified
     */
    public synchronized boolean isPieceComplete(int piece) {
        synchronized (this.isComplete) {
            return this.isComplete.get(piece);
        }
    }

    /**
     * Check if the piece with the given index is requested by a peer
     */
    public synchronized boolean isPieceRequested(int piece) {
        synchronized (this.isRequested) {
            return this.isRequested.get(piece);
        }
    }

    /**
     * Mark a piece as complete or not according to the parameters
     */
    public synchronized void setComplete(int piece, boolean is) {
        synchronized (this.isComplete) {
            this.isComplete.set(piece, is);
        }
    }

    /**
     * Mark a piece as requested or not according to the parameters
     */

    public synchronized void setRequested(int piece, boolean is) {
        synchronized (this.isRequested) {
            this.isRequested.set(piece, is);
        }
    }


    /**
     * Returns the index of the piece that could be downloaded by the peer in parameter
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
     */
    public synchronized void taskCompleted(String id, int reason) {
        switch (reason) {
            case DownloadTask.CONNECTION_REFUSED:
                log.error("Connection refused by host " + id);
                break;
            case DownloadTask.MALFORMED_MESSAGE:
                log.error("Malformed message from " + id + ". Task ended...");
                break;
            case DownloadTask.UNKNOWN_HOST:
                log.error("Connection could not be established to " + id + ". Host unknown...");
        }
        this.peerAvailabilies.remove(id);
        this.task.remove(id);
        this.peerList.remove(id);
    }

    /**
     * Received when a piece has been fully downloaded by a task. The piece might
     * have been corrupted, in which case the manager will request it again later.
     * If it has been successfully downloaded and verified, the piece status is
     * set to 'complete', the
     * piece is saved into the corresponding file(s)
     */
    public synchronized void pieceCompleted(String peerID, int i, boolean complete) {
        synchronized (this.isRequested) {
            this.isRequested.clear(i);
        }
        synchronized (this.isComplete) {
            if (complete && !this.isPieceComplete(i)) {
                pu.updateParameters(this.torrent.pieceLength, 0, "");
                this.isComplete.set(i, complete);
                float totaldl = (float) (((float) (100.0)) * ((float) (this.isComplete.cardinality())) / ((float) (this.nbPieces)));
                log.info("Piece completed by " + peerID + " : " + i + " (Total dl = " + totaldl + "% )");
                this.savePiece(i);
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
     */
    public synchronized void pieceRequested(int i, boolean requested) {
        this.isRequested.set(i, requested);
    }

    /**
     * Received when a task is ready to download or upload. In such a case, if
     * there is a piece that can be downloaded from the corresponding peer, then
     * request the piece
     */
    public synchronized void peerReady(String peerID) {
        int piece2request = this.choosePiece2Download(peerID);
        if (piece2request != -1)
            this.task.get(peerID).requestPiece(this.pieceList[piece2request]);
    }

    /**
     * Load piece data from the existing files
     */
    public synchronized byte[] getPieceFromFiles(int piece) {
        byte[] data = new byte[this.pieceList[piece].getLength()];
        int remainingData = data.length;
        for (Iterator it = this.pieceList[piece].getFileAndOffset().keySet().
                iterator(); it.hasNext(); ) {
            try {
                Integer file = (Integer) (it.next());
                int remaining = (Integer) this.torrent.length.get(file) - (Integer) (this.pieceList[piece].getFileAndOffset().get(file));
                this.output_files[file].seek((Integer) (this.pieceList[piece].getFileAndOffset().get(file)));
                this.output_files[file].read(data,
                                             data.length - remainingData,
                                             (remaining < remainingData) ? remaining : remainingData
                                            );
                remainingData -= remaining;
            } catch (IOException ioe) {
                log.error(ioe.getMessage());
            }
        }
        return data;
    }

    /**
     * Update the piece availabilities for a given peer
     */
    public synchronized void peerAvailability(String peerID, BitSet has) {
        this.peerAvailabilies.put(peerID, has);
        BitSet interest = (BitSet) (has.clone());
        interest.andNot(this.isComplete);
        DownloadTask dt = this.task.get(peerID);
        if (dt != null) {
            if (interest.cardinality() > 0 && !dt.peer.isInteresting()) {
                dt.ms.addMessageToQueue(new PeerProtocolMessage(PeerProtocol.INTERESTED, 2));
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
                }
            }
        }
        log.info("Peer List updated from tracker with " + list.size() + " peers");
    }

    /**
     * Called when an update try fail. At the moment, simply display a message
     */
    public void updateFailed(int error, String message) {
        System.err.println(message);
        System.err.flush();
    }

    /**
     * Add the download task to the list of active (i.e. Handshake is ok) tasks
     */
    public synchronized void addActiveTask(String id, DownloadTask dt) {
        synchronized (this.task) {
            this.task.put(id, dt);
        }
    }

    /**
     * Called when a new peer connects to the client. Check if it is already
     * registered in the peer list, and if not, create a new DownloadTask for it
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