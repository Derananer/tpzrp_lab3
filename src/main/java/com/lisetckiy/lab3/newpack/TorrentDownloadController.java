package com.lisetckiy.lab3.newpack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
public class TorrentDownloadController {

    private static String FILENAME = "ubuntu-16.04.7-desktop-amd64.iso.torrent";


    @Autowired
    private TorrentDownloadServiceNew torrentDownloadService;

    @GetMapping("/test")
    public void loadFile(){
        torrentDownloadService.downloadFile(FILENAME);
    }
}
