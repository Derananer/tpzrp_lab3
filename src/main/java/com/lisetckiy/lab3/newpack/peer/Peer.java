package com.lisetckiy.lab3.newpack.peer;

import com.lisetckiy.lab3.parser.TorrentFile;

import java.io.IOException;

public class Peer {

    private PeerInfo info;
    private PeerConnector peerConnector;

    public static class Builder {
        Peer object = new Peer();

        private TorrentFile torrentFile;
        private byte[] clientId;

        public Builder info(PeerInfo peerInfo) {
            object.info = peerInfo;
            return this;
        }

        public Builder torrentFile(TorrentFile torrentFile) {
            this.torrentFile = torrentFile;
            return this;
        }

        public Builder clientId(byte[] clientId) {
            this.clientId = clientId;
            return this;
        }

        public Peer build() {
            object.peerConnector = new PeerConnector(this.torrentFile, object.info, this.clientId);
            return object;
        }
    }

    public static Builder builder(){
        return new Builder();
    }

    public Peer connect() throws IOException {
        this.peerConnector.init();
        return this;
    }

}
