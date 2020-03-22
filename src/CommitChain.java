import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class CommitChain implements Serializable , Iterable<Commit>{

    //branchHeads保存每一个branch指向Commit对象的引用
    private HashMap<String, Commit> commits;
    private HashMap<String, String> branches;
    private Commit chain;
    private Commit head;
    private String curBranchName;
    private String ccPath;

    public static CommitChain deSerialFromPath(Path path) {
        CommitChain commitChain;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(path.toString()));
            commitChain = (CommitChain) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            commitChain = new CommitChain(path);
        }
        return commitChain;
    }

    private CommitChain(Path ccPath) {
        this.ccPath = ccPath.toString();
        commits = new HashMap<>();
        branches = new HashMap<>();
    }

    public Commit newCommit(ZonedDateTime timestamp, String log, String commitDir,
                          String SHA1, String author) {
        Commit commit;
        if (chain == null) {
            commit = new Commit(timestamp, log, commitDir, SHA1, author, "null");
            chain = commit;
            curBranchName = "master";
        } else {
            commit = new Commit(timestamp, log, commitDir, SHA1, author, head.getCommitStr());
            head.addCommit(commit.getCommitStr());
        }
        commits.put(commit.getCommitStr(), commit);
        head = commit;
        //调用update
        branches.put(curBranchName, head.getCommitStr());
        return commit;
    }

    public Commit updateBranches(String branchName) {
        branches.put(branchName, head.getCommitStr());
        return head;
    }

    public void changeBranchTo(String branchName) {
        Commit temp = getCommitByStr(branches.get(branchName));
        if (temp == null) {
            System.err.println("No branch named " + branchName);
            System.exit(0);
        }
        head = temp;
        curBranchName = branchName;
    }

    public Commit getHeadCommit() {
        return head;
    }

    public Commit getCommitByStr(String commitStr) {
        return commits.get(commitStr);
    }

    public boolean isHead(Commit commit) {
        return commit == head;
    }

    public LinkedList<Commit> findCommitWithLog(String log) {
        LinkedList<Commit> ans = new LinkedList<>();
        for(Commit c : this) {
            if (c.getLog().equals(log))
                ans.addLast(c);
        }
        return ans;
    }

    private class CommitIterator implements Iterator<Commit> {
        Commit cur = head;

        @Override
        public boolean hasNext() {
            return !cur.getParentStr().equals("null");
        }

        @Override
        public Commit next() {
            Commit dummyCur = cur;
            cur = getCommitByStr(dummyCur.getParentStr());
            return dummyCur;
        }
    }

    @Override
    public Iterator<Commit> iterator() {
        return new CommitIterator();
    }
}