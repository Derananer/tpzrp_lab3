package com.lisetckiy.lab3.parser;

import java.util.ArrayList;
import java.util.Date;

public class TorrentFile {

    public String announceURL;
    public String comment;
    public String createdBy;
    public long creationDate;
    public String encoding;
    public String saveAs;
    public int pieceLength;

    /* In case of multiple files torrent, saveAs is the name of a directory
     * and name contains the path of the file to be saved in this directory
     */
    public ArrayList name;
    public ArrayList length;

    public byte[] info_hash_as_binary;
    public String info_hash_as_hex;
    public String info_hash_as_url;
    public long total_length;

    public ArrayList piece_hash_values_as_binary;
    public ArrayList piece_hash_values_as_hex;
    public ArrayList piece_hash_values_as_url;

    /**
     * Create the TorrentFile object and initiate its instance variables
     */
    public TorrentFile() {
        super();
        announceURL = new String();
        comment = new String();
        createdBy = new String();
        encoding = new String();
        saveAs = new String();
        creationDate = -1;
        total_length = -1;
        pieceLength = -1;

        name = new ArrayList();
        length = new ArrayList();

        piece_hash_values_as_binary = new ArrayList();
        piece_hash_values_as_url = new ArrayList();
        piece_hash_values_as_hex = new ArrayList();
        info_hash_as_binary = new byte[20];
        info_hash_as_url = new String();
        info_hash_as_hex = new String();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Tracker URL: ").append(this.announceURL).append("\n");
        stringBuilder.append("Torrent created by : ").append(this.createdBy).append("\n");
        stringBuilder.append("Torrent creation date : ").append(new Date(this.creationDate)).append("\n");
        stringBuilder.append("Info hashes: ");
//        stringBuilder.append("\t\t").append(new String(this.info_hash_as_binary));
        stringBuilder.append("hexFormat: ").append(this.info_hash_as_hex);
        stringBuilder.append(" urlFormat: ").append(this.info_hash_as_url);
        stringBuilder.append("\n").append("Comment :").append(this.comment).append("\n");
        stringBuilder.append("Files List :\n");
        for (int i = 0; i < this.length.size(); i++)
            stringBuilder.append("\t- ").append(this.name.get(i)).append(" ( ").append(this.length.get(i)).append(" Bytes )").append("\n");
        stringBuilder.append("\n");
        stringBuilder.append("Pieces hashes (piece length = ").append(this.pieceLength).append(") :\n");
        for (int i = 0; i < this.piece_hash_values_as_binary.size(); i++) {
            stringBuilder.append(i + 1);
//                    .append(":\t\t").append(this.piece_hash_values_as_binary.get(i));
            stringBuilder.append("\t\t").append(this.piece_hash_values_as_hex.get(i)).append("\n");
//            stringBuilder.append("\t\t").append(this.piece_hash_values_as_url.get(i));

        }
        return stringBuilder.toString();
    }

}