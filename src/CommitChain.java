import Exceptions.NoSuchBranchException;

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

    //branchHeads保存每一个branch开头Commit对象的引用
    private HashMap<String, Commit> branchHeads = new HashMap<>();
    private Commit chain = new Commit(ZonedDateTime.now(), "dummy", null, null, null, null);
    private Commit head = chain;
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

    public CommitChain(Path ccPath) {
        this.ccPath = ccPath.toString();
    }

    public Commit newCommit(ZonedDateTime timestamp, String log, String commitDir,
                          String SHA1, String author) {
        Commit commit = new Commit(timestamp, log, commitDir, SHA1, author, head);
        head.addCommit(commit);
        head = commit;
        return commit;
    }

    public Commit newBranch(ZonedDateTime timestamp, String author, String branchName) {
        Commit branch = new Commit(timestamp, branchName, author, head);
        head.addBranch(branch);
        branchHeads.put(branchName, branch);
        return branch;
    }

    public void changeHead(String branchName) throws NoSuchBranchException {
        Commit temp = branchHeads.get(branchName);
        if (temp == null) throw new NoSuchBranchException();
        head = temp;
    }

    public Iterator<Map.Entry<String, Commit>> getBranchMapIterator() {
        return branchHeads.entrySet().iterator();
    }

    public Commit getHeadCommit() {
        return head;
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
            return cur.getParent() != null;
        }

        @Override
        public Commit next() {
            Commit dummyCur = cur;
            cur = dummyCur.getParent();
            return dummyCur;
        }
    }

    @Override
    public Iterator<Commit> iterator() {
        return new CommitIterator();
    }
}