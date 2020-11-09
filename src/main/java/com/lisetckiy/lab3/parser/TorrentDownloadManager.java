package com.lisetckiy.lab3.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TorrentDownloadManager {

    private static String FILENAME = "test_file.torrent";

    private TorrentFileBuilder torrentFileBuilder;

    @Autowired
    public TorrentDownloadManager(TorrentFileBuilder torrentFileBuilder) {
        this.torrentFileBuilder = torrentFileBuilder;
    }

    public void downloadFile(String filename) {
        TorrentFile torrentFile = torrentFileBuilder.build(filename);
        log.info("Torrent file was built: {}", torrentFile);
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
