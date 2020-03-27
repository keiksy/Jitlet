package Gitlet.Blobs;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Blob implements Serializable {
    private String dirGit;
    private String dirRaw;
    private String hash;

    public Blob(String dirGit, String dirRaw, String hash) {
        this.dirGit = dirGit;
        this.dirRaw = dirRaw;
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public Path getPathGit() {
        return Paths.get(dirGit);
    }

    public Path getPathRaw() { return Paths.get(dirRaw); }
}
