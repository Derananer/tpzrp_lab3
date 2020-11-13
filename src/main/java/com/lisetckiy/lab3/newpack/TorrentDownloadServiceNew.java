package com.lisetckiy.lab3.newpack;

import com.lisetckiy.lab3.parser.TorrentFile;
import com.lisetckiy.lab3.parser.TorrentFileBuilder;
import com.lisetckiy.lab3.parser.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//@Component
@Slf4j
public class TorrentDownloadServiceNew {

    private TorrentFileBuilder torrentFileBuilder;

    @Autowired
    public TorrentDownloadServiceNew(TorrentFileBuilder torrentFileBuilder) {
        this.torrentFileBuilder = torrentFileBuilder;
    }

    public void downloadFile(String filename) {
        TorrentFile torrentFile = torrentFileBuilder.build(filename);
        log.info("Torrent file was built: {}", torrentFile);
        if (torrentFile != null) {
            FileDownloadManager dm = new FileDownloadManager(Utils.generateID(), torrentFile);
            dm.startListening(6881, 6889);
            dm.startTrackerUpdate();
            dm.blockUntilCompletion();
//            dm.stopTrackerUpdate();
//            dm.closeTempFiles();
        }
    }

}
