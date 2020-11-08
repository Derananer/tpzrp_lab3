package com.lisetckiy.lab3;

import com.lisetckiy.lab3.parser.TorrentDownloadManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Lab3ApplicationTests {

	@Autowired
	private TorrentDownloadManager torrentDownloadManager;

	@Test
	void contextLoads() {
		torrentDownloadManager.parseFile();
	}

}
