import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Commit implements Serializable {

    private Commit parent;
    private List<Commit> branches;
    private String branchName;
    private boolean isBranchHead;

    private ZonedDateTime timestamp;
    private String log;
    private String commitDir;
    private String SHA1;
    private String author;

    public Commit(ZonedDateTime timestamp, String branchName, String author, Commit parent) {
        this.timestamp = timestamp;
        this.branchName = branchName;
        this.author = author;

        this.parent = parent;
        branches = new LinkedList<>();
        isBranchHead = true;
    }

    public Commit(ZonedDateTime timestamp, String log, String commitDir,
                    String SHA1, String author, Commit parent) {
        this.timestamp = timestamp;
        this.log = log;
        this.commitDir = commitDir;
        this.SHA1 = SHA1;
        this.author = author;

        this.branchName = parent==null ? "master" : parent.getBranchName();
        this.parent = parent;
        branches = new LinkedList<>();
    }

    public String getCommitDir() {
        return commitDir;
    }

    public String getLog() {
        return log;
    }

    public String getSHA1() {
        return SHA1;
    }

    public String getBranchName() {
        return branchName;
    }

    public Commit getParent() { return parent; }

    public void addCommit(Commit commit) {
        branches.add(commit);
    }

    public void addBranch(Commit branch) {
        addCommit(branch);
    }

    public boolean isBranchHead() {
        return isBranchHead;
    }

    @Override
    public String toString() {
        return "Hash: "+SHA1+"\n"+
                "Branch: " + branchName+"\n"+
                "Commit time: "+timestamp.toString()+"\n"+
                "Commit log: "+log+"\n"+
                "Author: "+author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Commit commit = (Commit) o;

        if (branchName != null ? !branchName.equals(commit.branchName) : commit.branchName != null) return false;
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
