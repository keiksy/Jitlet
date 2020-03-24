import Exceptions.*;

import java.io.*;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

public class CommitChain implements Serializable , Iterable<Commit>{

    private static final long serialVersionUID = 11111111L;

    private HashMap<String, Commit> commits;
    private HashMap<String, String> branches;
    private Commit chain;
    private Commit head;
    private String curBranchName;

    public static CommitChain deSerialFromPath(Path ccPath) {
        CommitChain commitChain;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(ccPath.toString()));
            commitChain = (CommitChain) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            commitChain = new CommitChain();
        }
        return commitChain;
    }

    private CommitChain() {
        commits = new HashMap<>();
        branches = new HashMap<>();
    }

    public void newCommit(ZonedDateTime timestamp, String log, String commitDir,
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
        branches.put(curBranchName, head.getCommitStr());
    }

    public Commit getHeadCommit() {
        return head;
    }

    private Commit getCommitByStr(String commitStr) throws NoSuchCommitException {
        Commit temp = commits.get(commitStr);
        if (temp == null)
            throw new NoSuchCommitException();
        return temp;
    }

    public boolean isHead(Commit commit) {
        return commit == head;
    }

    public String getCurBranchName() {
        return curBranchName;
    }

    public void addBranch(String branch) throws AlreadyExistBranchException{
        if (branches.containsKey(branch))
            throw new AlreadyExistBranchException();
        branches.put(branch, head.getCommitStr());
    }

    public void deleteBranch(String branch) throws  NoSuchBranchException, DeleteCurrentBranchException {
        if (!branches.containsKey(branch))
            throw new NoSuchBranchException();
        if (curBranchName.equals(branch))
            throw new DeleteCurrentBranchException();
        String rmCommitStr = branches.remove(branch);
        Iterator<Commit> commitIterator = getIteratorFromCommit(rmCommitStr);
        while(commitIterator.hasNext()) {
            Commit temp = commitIterator.next();
            String commitStr = temp.getCommitStr();
            if (branches.containsValue(commitStr) || temp.getSonsSize() > 1) {
                temp.deleteSon(commitStr);
                break;
            } else {
                commits.remove(commitStr);
            }
        }
    }

    public void resetTo(String commitStr) throws NoSuchCommitException{
        head = getCommitByStr(commitStr);
        branches.put(curBranchName, commitStr);
    }

    public void changeBranchTo(String branch) throws NoSuchBranchException {
        try {
            head = getCommitByStr(branches.get(branch));
        } catch (NoSuchCommitException e) {
            throw new NoSuchBranchException();
        }
        curBranchName = branch;
    }

    public Iterator<Map.Entry<String,Commit>> getAllCommitsIterator() {
        return commits.entrySet().iterator();
    }

    public LinkedList<Commit> findCommitWithLog(String log) {
        LinkedList<Commit> ans = new LinkedList<>();
        Iterator<Map.Entry<String,Commit>> iterator = getAllCommitsIterator();
        while(iterator.hasNext()) {
            Map.Entry<String,Commit> temp = iterator.next();
            if (temp.getValue().getLog().equals(log))
                ans.addLast(temp.getValue());
        }
        return ans;
    }

    private class CommitIterator implements Iterator<Commit> {
        Commit cur = head;

        public CommitIterator() {}

        public CommitIterator(String commitStr) {
            try {
                cur = getCommitByStr(commitStr);
            } catch (NoSuchCommitException ignored) {}
        }

        @Override
        public boolean hasNext() {
            return !cur.getParentStr().equals("null");
        }

        @Override
        public Commit next() {
            Commit dummyCur = cur;
            try {
                cur = getCommitByStr(dummyCur.getParentStr());
            } catch (NoSuchCommitException ignored) { }
            return dummyCur;
        }
    }

    @Override
    public Iterator<Commit> iterator() {
        return new CommitIterator();
    }

    private Iterator<Commit> getIteratorFromCommit(String commitStr) {
        return new CommitIterator(commitStr);
    }
}