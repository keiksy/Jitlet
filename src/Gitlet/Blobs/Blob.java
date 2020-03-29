package Gitlet.Blobs;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 抽象文件的类，用于表示gitlet内每个快照
 */

public class Blob implements Serializable {
    //在.gitlet文件夹内该快照的位置
    private String dirGit;
    //在工作目录内源文件的地址
    private String dirRaw;

    Blob(String dirGit, String dirRaw) {
        this.dirGit = dirGit;
        this.dirRaw = dirRaw;
    }

    public Path getPathGit() {
        return Paths.get(dirGit);
    }

    public Path getPathRaw() { return Paths.get(dirRaw); }
}
