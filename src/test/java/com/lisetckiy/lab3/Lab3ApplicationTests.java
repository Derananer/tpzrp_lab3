package com.lisetckiy.lab3;

import com.lisetckiy.lab3.parser.TorrentDownloadService;
import com.lisetckiy.lab3.parser.TorrentFile;
import com.lisetckiy.lab3.parser.TorrentFileBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Lab3ApplicationTests {

	@Autowired
	private TorrentDownloadService torrentDownloadService;

	@Autowired
	private TorrentFileBuilder torrentFileBuilder;

	private static String FILENAME = "xubuntu-20.10-desktop-amd64.iso.torrent";
	private static String FILENAME1 = "ubuntu-16.04.7-desktop-amd64.iso.torrent";

	@Test
	void contextLoads() {
		torrentDownloadService.downloadFile(FILENAME);
	}

	@Test
	void torrentFileBuild(){

		System.out.println("http://bt.rutor.org:2710".matches("http.*"));
		TorrentFile torrentFile = torrentFileBuilder.build(FILENAME1);
		TorrentFile torrentFile1 = torrentFileBuilder.build(FILENAME);

	}
}
