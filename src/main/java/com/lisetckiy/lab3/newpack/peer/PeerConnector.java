package com.lisetckiy.lab3.newpack.peer;

import com.lisetckiy.lab3.newpack.peer.messaging.*;
import com.lisetckiy.lab3.parser.PeerProtocol;
import com.lisetckiy.lab3.parser.TorrentFile;
import com.lisetckiy.lab3.parser.Utils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;

@Slf4j
public class PeerConnector {

    private static final int IDLE = 0;
    private static final int WAIT_HS = 1;
    private static final int WAIT_BFORHAVE = 2;
    private static final int WAIT_UNCHOKE = 4;
    private static final int READY_2_DL = 5;
    private static final int DOWNLOADING = 6;
    private static final int WAIT_BLOCK = 7;

    public static final int TASK_COMPLETED = 0;
    public static final int UNKNOWN_HOST = 1;
    public static final int CONNECTION_REFUSED = 2;
    public static final int BAD_HANDSHAKE = 3;
    public static final int MALFORMED_MESSAGE = 4;
    public static final int TIMEOUT = 5;

    // Client ID
    private byte[] clientID;
    private TorrentFile torrentFile;
    private PeerInfo peer;
    private Socket connectionSocket;
    private boolean connected;
    private boolean initiate = true;

    private MessageSender messageSender;
    private MessageReceiver messageReceiver;

    public PeerConnector(TorrentFile torrentFile, PeerInfo peer, byte[] clientID) {
        this.torrentFile = torrentFile;
        this.peer = peer;
        this.clientID = clientID;
    }


    public void init() throws IOException {
//        if (this.peerConnection == null && !this.peer.isConnected()) {
        this.connectionSocket = new Socket(this.peer.getIP(), this.peer.getPort());

        this.peer.setConnected(true);
//        }


        this.messageSender = new MessageSender(this.peer.toString(), connectionSocket.getOutputStream());
//        this.ms.addOutgoingListener(this);
        this.messageSender.start();
        this.messageReceiver = new MessageReceiver(this.peer.toString(), this.connectionSocket.getInputStream(), this);
//        this.messageReceiver.addIncomingListener(this);
        this.messageReceiver.start();

        handshake();
    }

    public void addMessage(Message message) {
        messageSender.addMessage(message);
    }

    private void handshake() {
//        OutputStream os = this.connectionSocket.getOutputStream();
//        InputStream is = this.connectionSocket.getInputStream();
        HandshakeMessage handshakeMessage = HandshakeMessage.builder()
                                                            .infoHash(torrentFile.info_hash_as_binary)
                                                            .peerId(clientID)
                                                            .build();
        log.info("Add handshake Peer:{} message:{}", peer, handshakeMessage);
        messageSender.addMessage(handshakeMessage);
    }

    public void messageReceived(Message m) {
//        if (m == null) {
////            this.fireTaskCompleted(this.peer.toString(), this.MALFORMED_MESSAGE);
//            return;
//        }
////        this.lmrt = System.currentTimeMillis();
//        if (m.getType() == PeerProtocol.HANDSHAKE) {
//            HandshakeMessage hs = (HandshakeMessage) m;
//            // Check that the requested file is the one this client is sharing
//            if (Utils.bytesCompare(hs.getFileID(), torrentFile.info_hash_as_binary)) {
//                if (!initiate) { // If not already done, send handshake message
//                    this.peer.setID(new String(hs.getPeerID()));
////                    this.ms.addMessageToQueue(new HandshakeMessage(this.fileID, this.myID));
//                    handshake();
//                }
//                addMessage(new PeerProtocolMessage(PeerProtocol.BITFIELD, this.bitfield));
////                this.creationTime = System.currentTimeMillis();
////                this.changeState(this.WAIT_BFORHAVE);
//            } else{
////                this.fireTaskCompleted(this.peer.toString(),this.BAD_HANDSHAKE);
//            }
//            hs = null;
//
//        } else {
//            PeerProtocolMessage message = (PeerProtocolMessage) m;
//            switch (message.getType()) {
//                case PeerProtocol.KEEP_ALIVE:
//                    // Nothing to do, just keep the connection open
//                    break;
//
//                case PeerProtocol.CHOKE:
//                    /*
//                     * Change the choking state to true, meaning remote peer
//                     * will not accept any request message from this client
//                     */
//                    this.peer.setChoking(true);
//                    this.isDownloading = false;
//
//                    break;
//
//                case PeerProtocol.UNCHOKE:
//                    /*
//                     * Change the choking state to false, meaning this client now
//                     * accepts request messages from this client.
//                     * If this task was already downloading a piece, then continue.
//                     * Otherwise, advertise DownloadManager that it is ready to do so
//                     */
//                    this.peer.setChoking(false);
//                    if (this.downloadPiece == null) {
//                        this.changeState(READY_2_DL);
//                    } else
//                        this.changeState(DOWNLOADING);
//                    break;
//
//                case PeerProtocol.INTERESTED:
//                    /*
//                     * Change the interested state of the remote peer to true,
//                     * meaning this peer will start downloading from this client if
//                     * it is unchoked
//                     */
//                    this.peer.setInterested(true);
//                    break;
//
//                case PeerProtocol.NOT_INTERESTED:
//                    /*
//                     * Change the interested state of the remote peer to true,
//                     * meaning this peer will not start downloading from this client
//                     * if it is unchoked
//                     */
//
//                    this.peer.setInterested(false);
//                    break;
//
//                case PeerProtocol.HAVE:
//                    /*
//                     * Update the peer piece list with the piece described in this
//                     * message and advertise DownloadManager of the change
//                     */
//                    this.peer.setHasPiece(Utils.byteArrayToInt(message.getPayload()), true);
//                    this.firePeerAvailability(this.peer.toString(), this.peer.getHasPiece());
//                    break;
//
//                case PeerProtocol.BITFIELD:
//                    /*
//                     * Update the peer piece list with the piece described in this
//                     * message and advertise DownloadManager of the change
//                     */
//                    this.peer.setHasPiece(message.getPayload());
//                    this.firePeerAvailability(this.peer.toString(), this.peer.getHasPiece());
//                    this.changeState(this.WAIT_UNCHOKE);
//                    break;
//
//                case PeerProtocol.REQUEST:
//                    /*
//                     * If the peer is not choked, advertise the DownloadManager of
//                     * this request. Otherwise, end connection since the peer does
//                     * not respect the Bittorrent protocol
//                     */
//
//                    if(!this.peer.isChoked()){
//                        this.firePeerRequest(this.peer.toString(),
//                                             Utils.byteArrayToInt(Utils.subArray(message.getPayload(),0, 4)),
//                                             Utils.byteArrayToInt(Utils.subArray(message.getPayload(), 4, 4)),
//                                             Utils.byteArrayToInt(Utils.subArray(message.getPayload(), 8, 4))
//                                            );
//                    }else{
//                        this.fireTaskCompleted(this.peer.toString(), this.MALFORMED_MESSAGE);
//                    }
//                    break;
//
//                case PeerProtocol.PIECE:
//                    /**
//                     * Sets the block of data downloaded in the piece block list and
//                     * update the peer download rate. Removes the piece block from
//                     * the pending request list and change state.
//                     */
//                    int begin = Utils.byteArrayToInt(Utils.subArray(message.getPayload(), 4, 4));
//                    byte[] data = Utils.subArray(message.getPayload(), 8, message.getPayload().length - 8);
//                    this.downloadPiece.setBlock(begin, data);
//                    this.peer.setDLRate(data.length);
//                    this.pendingRequest.remove(new Integer(begin));
//                    if (this.pendingRequest.size() == 0)
//                        this.isDownloading = false;
//                    this.changeState(this.DOWNLOADING);
//                    break;
//
//                case PeerProtocol.CANCEL:
//                    // TODO: Still to implement the cancel message. Not used here
//                    break;
//
//                case PeerProtocol.PORT:
//                    // TODO: Still to implement the port message. Not used here
//                    break;
//            }
//            message = null;
//        }
//        m = null;
//    }
    }

}