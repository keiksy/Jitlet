package Gitlet.Commits;

import Gitlet.Utility.Utils;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * Commit结点
 */

public class Commit implements Serializable {

    //the commitStr of parent commit.
    private String parent;
    //holds the commitStrs of son commits.
    private List<String> sons = new LinkedList<>();

    private ZonedDateTime timestamp;
    private String log;
    private String SHA1;
    private String author;
    //holds the hash values of commited files.
    //files can be retrieved using hash value through the Gitlet.Gitlet.Blobs.BlobPool object.
    private List<String> files;

    public Commit(ZonedDateTime timestamp, String log, List<String> commitFiles,
                    String SHA1, String author, String parent) {
        this.timestamp = timestamp;
        this.log = log;
        this.SHA1 = SHA1;
        this.author = author;

        this.files = commitFiles;
        this.parent = parent;
    }

    public List<String> getFiles() { return files; }

    public String getLog() {
        return log;
    }

    public String getParentCommitStr() { return parent; }

    public String getCommitStr() { return Utils.fromHash2DirName(SHA1); }

    public void addSonCommit(String commitStr) {
        sons.add(commitStr);
    }

    @Override
    public String toString() {
        return "Hash: "+SHA1+"\n"+
                "Gitlet.Commits.Commit time: "+timestamp.toString()+"\n"+
                "Gitlet.Commits.Commit log: "+log+"\n"+
                "Author: "+author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Commit commit = (Commit) o;

        if (timestamp != null ? !timestamp.equals(commit.timestamp) : commit.timestamp != null) return false;
        if (log != null ? !log.equals(commit.log) : commit.log != null) return false;
        if (SHA1 != null ? !SHA1.equals(commit.SHA1) : commit.SHA1 != null) return false;
        return author != null ? author.equals(commit.author) : commit.author == null;
    }

    @Override
    public int hashCode() {
        return SHA1.hashCode();
    }
}
