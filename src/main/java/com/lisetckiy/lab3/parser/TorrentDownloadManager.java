package com.lisetckiy.lab3.parser;

import com.lisetckiy.lab3.jBittorrentAPI.DownloadManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TorrentDownloadManager {

    private static String FILENAME = "test_file.torrent";

    private TorrentFileBuilder torrentFileBuilder;

    @Autowired
    public TorrentDownloadManager(TorrentFileBuilder torrentFileBuilder) {
        this.torrentFileBuilder = torrentFileBuilder;
    }

    public void downloadFile(String filename) {
        TorrentFile torrentFile = torrentFileBuilder.build(filename);
        if (torrentFile != null) {
            DownloadManager dm = new DownloadManager(torrentFile, Utils.generateID());
            dm.startListening(6881, 6889);
            dm.startTrackerUpdate();
            dm.blockUntilCompletion();
            dm.stopTrackerUpdate();
            dm.closeTempFiles();
        }
    }


}
