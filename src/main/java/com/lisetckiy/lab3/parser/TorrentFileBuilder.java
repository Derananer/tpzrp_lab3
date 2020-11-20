package com.lisetckiy.lab3.parser;

import com.lisetckiy.lab3.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TorrentFileBuilder {

    private BencodeDecoder bencodeDecoder;

    @Autowired
    public TorrentFileBuilder(BencodeDecoder bencodeDecoder) {
        this.bencodeDecoder = bencodeDecoder;
    }

    public TorrentFile build(String filename) {
        Map parsedFile = parseTorrentFile(filename);
        return buildTorrentFile(parsedFile);
    }

    /**
     * Given the path of a torrent, parse the file and represent it as a Map
     */
    private Map parseTorrentFile(String filename) {
        return this.parseTorrentFile(new File(filename));
    }

    private Map parseTorrentFile(File file) {
        try (FileInputStream torrentFile = new FileInputStream(file)) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(torrentFile);
            return bencodeDecoder.decodeStream(bufferedInputStream);
        } catch (IOException e) {
            log.error("Error while parsing torrent file name=" + file.getName(), e.getCause());
        }
        return null;
    }

    private TorrentFile buildTorrentFile(Map map) {
        if (map == null)
            return null;
        TorrentFile torrent = new TorrentFile();
        if (map.containsKey("announce")) {// mandatory key
            String announceUrl = new String((byte[]) map.get("announce"));
            if (announceUrl.matches("http.*")) {torrent.announceURL = announceUrl;}
            else {
                List announceList = (List) map.get("announce-list");
                List announceList1 = new ArrayList();
                for (Object item : announceList) {
                    announceList1.addAll((List) item);
                }
                List<String> list = (List<String>) announceList1.stream().map(item -> new String((byte[]) item)).collect(Collectors.toList());
                for (String item : list) {
                    if (item.matches("http.*")) {
                        torrent.announceURL = announceUrl;
                        break;
                    }
                }
            }
            log.info(torrent.announceURL);
        } else
            return null;
        if (map.containsKey("comment")) // optional key
            torrent.comment = new String((byte[]) map.get("comment"));
        if (map.containsKey("created by")) // optional key
            torrent.createdBy = new String((byte[]) map.get("created by"));
        if (map.containsKey("creation date")) // optional key
            torrent.creationDate = (Long) map.get("creation date");
        if (map.containsKey("encoding")) // optional key
            torrent.encoding = new String((byte[]) map.get("encoding"));

        //Store the info field data
        if (map.containsKey("info")) {
            Map info = (Map) map.get("info");
            try {

                torrent.info_hash_as_binary = Utils.hash(BencodeEncoder.encode(info));
                torrent.info_hash_as_hex = Utils.byteArrayToByteString(torrent.info_hash_as_binary);
                torrent.info_hash_as_url = Utils.byteArrayToURLString(torrent.info_hash_as_binary);
            } catch (IOException ioe) {
                return null;
            }
            if (info.containsKey("name"))
                torrent.saveAs = new String((byte[]) info.get("name"));
            if (info.containsKey("piece length"))
                torrent.pieceLength = ((Long) info.get("piece length")).intValue();
            else
                return null;

            if (info.containsKey("pieces")) {
                byte[] piecesHash2 = (byte[]) info.get("pieces");
                if (piecesHash2.length % 20 != 0)
                    return null;

                for (int i = 0; i < piecesHash2.length / 20; i++) {
                    byte[] temp = Utils.subArray(piecesHash2, i * 20, 20);
                    torrent.piece_hash_values_as_binary.add(temp);
                    torrent.piece_hash_values_as_hex.add(Utils.byteArrayToByteString(temp));
                    torrent.piece_hash_values_as_url.add(Utils.byteArrayToURLString(temp));
                }
            } else
                return null;

            if (info.containsKey("files")) {
                List multFiles = (List) info.get("files");
                torrent.total_length = 0;
                for (int i = 0; i < multFiles.size(); i++) {
                    torrent.length.add(((Long) ((Map) multFiles.get(i)).get("length")).intValue());
                    torrent.total_length += ((Long) ((Map) multFiles.get(i)).get("length")).intValue();
                    List path = (List) ((Map) multFiles.get(i)).get("path");
                    String filePath = "";
                    for (int j = 0; j < path.size(); j++) {
                        filePath += new String((byte[]) path.get(j));
                    }
                    torrent.name.add(filePath);
                }
            } else {
                torrent.length.add(((Long) info.get("length")).intValue());
                torrent.total_length = ((Long) info.get("length")).intValue();
                torrent.name.add(new String((byte[]) info.get("name")));
            }
        } else
            return null;
        return torrent;
    }
}