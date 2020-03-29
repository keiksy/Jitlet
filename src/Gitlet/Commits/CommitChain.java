package Gitlet.Commits;

import Gitlet.Utility.Exceptions.*;

import java.io.*;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * commitChain的数据结构操作类
 *
 * commitChain是一个链表表示的K叉树
 * 所有的commit都在一个hashmap中存储
 * 所有的branch使用{branch : commitStr}的形式在hashmap中存储
 */

public class CommitChain implements Serializable , Iterable<Commit>{

    //commit pool: map a commitStr to a Gitlet.Commits.Commit Object
    private Map<String, Commit> commits = new HashMap<>();
    //branch pool: map a branch name to the commitStr of the Gitlet.Commits.Commit the branch point at.
    private Map<String, String> branches = new HashMap<>();
    //the commit tree's root node.
    private Commit chain;
    //head is the name of current working branch.
    private String head;

    /**
     * 从指定路径反序列化commitChain对象
     *
     * 如果读不到，就实例化一个新的commitChain返回
     * @param ccPath 指定路径
     * @return 反序列化/新生成的commitChain对象的引用
     */
    public static CommitChain deSerialFrom(Path ccPath) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(ccPath.toString()));
            return (CommitChain) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new CommitChain();
        }
    }

    /**
     * 向commitChain的head指针后添加一个新的Commit对象，然后head指向这个新的对象
     * 同时当前branch也要指向这个新的对象
     *
     * 处理了当前chain指向为空，即当前commitChain为空（刚初始化）的特殊情况
     * @param timestamp 时间戳信息
     * @param log log信息
     * @param commitFiles 本commit保存的目录名
     * @param SHA1 sha-1字符串
     * @param author commit的作者
     */
    public void newCommit(ZonedDateTime timestamp, String log, Map<String, String> commitFiles,
                          String SHA1, String author) {
        Commit commit;
        if (chain == null) {
            commit = new Commit(timestamp, log, commitFiles, SHA1, author, "null");
            chain = commit;
            head = "master";
        } else {
            commit = new Commit(timestamp, log, commitFiles, SHA1, author, branches.get(head));
            getHeadCommit().addSonCommit(commit.getCommitStr());
        }
        commits.put(commit.getCommitStr(), commit);
        branches.put(head, commit.getCommitStr());
    }

    /**
     * 获取head指针指向的commit的引用
     */
    public Commit getHeadCommit() {
        try {
            return getCommitByBranch(head);
        } catch (NoSuchBranchException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Commit getCommitByBranch(String branch) throws NoSuchBranchException {
        if (!branches.containsKey(branch))
            throw new NoSuchBranchException();
        try {
            return getCommit(branches.get(branch));
        } catch (NoSuchCommitException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 使用commitStr获得对应的Commit对象
     * @param commitStr 六位commitStr字符串
     * @return
     * @throws NoSuchCommitException 如果找不到对应Commit对象，抛出此异常
     */
    private Commit getCommit(String commitStr) throws NoSuchCommitException {
        Commit temp = commits.get(commitStr);
        if (temp == null)
            throw new NoSuchCommitException();
        return temp;
    }

    public boolean isHead(Commit commit) {
        return commit == getHeadCommit();
    }

    public String getCurBranchName() {
        return head;
    }

    public void addBranch(String branch) throws AlreadyExistBranchException{
        if (branches.containsKey(branch))
            throw new AlreadyExistBranchException();
        branches.put(branch, branches.get(head));
    }

    /**
     * 删除指定名称的branch
     *
     * @param branch branch名称字符串
     * @throws DeleteCurrentBranchException 如果要删除的branch就是当前head指向Commit的branch，不可以删除
     */
    public void deleteBranch(String branch) throws  NoSuchBranchException, DeleteCurrentBranchException {
        if (!branches.containsKey(branch))
            throw new NoSuchBranchException();
        if (head.equals(branch))
            throw new DeleteCurrentBranchException();
        branches.remove(branch);
    }

    /**
     * 将head指针指向commitStr对应的Commit对象
     * @throws NoSuchCommitException
     */
    public void resetTo(String commitStr) throws NoSuchCommitException{
        if (!commits.containsKey(commitStr))
            throw new NoSuchCommitException();
        branches.put(head, commitStr);
    }

    /**
     * 将head指针指向指定branch所指向的Commit对象
     */
    public void changeBranchTo(String branch) throws NoSuchBranchException {
        if (!branches.containsKey(branch))
            throw new NoSuchBranchException();
        head = branch;
    }

    public Iterator<Map.Entry<String,Commit>> getAllCommitsIterator() {
        return commits.entrySet().iterator();
    }

    /**
     * 主要的迭代器，实现了"倒着走"的功能
     */
    private class CommitIterator implements Iterator<Commit> {
        Commit cur = getHeadCommit();

        public CommitIterator() {}

        public CommitIterator(String commitStr) {
            try {
                cur = getCommit(commitStr);
            } catch (NoSuchCommitException ignored) {}
        }

        @Override
        public boolean hasNext() {
            return !cur.getParentCommitStr().equals("null");
        }

        @Override
        public Commit next() {
            Commit dummyCur = cur;
            try {
                cur = getCommit(dummyCur.getParentCommitStr());
            } catch (NoSuchCommitException ignored) { }
            return dummyCur;
        }
    }

    @Override
    public Iterator<Commit> iterator() {
        return new CommitIterator();
    }

    public Commit findLCACommitByBranch(String branchA, String branchB) throws NoSuchBranchException {
        String commitStrA = branches.get(branchA), commitStrB = branches.get(branchB);
        if (commitStrA==null || commitStrB==null)
            throw new NoSuchBranchException();
        try {
            return findLCACommitByCommitStr(commitStrA, commitStrB);
        } catch (NoSuchCommitException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Commit findLCACommitByCommitStr(String commitStrA, String commitStrB) throws NoSuchCommitException {
        Commit a = getCommit(commitStrA), b = getCommit(commitStrB);
        if (commitStrA.equals(commitStrB))
            return a;
        LCAHelper(chain, a, b);
        return LCA;
    }

    private Commit LCA;

    private int LCAHelper(Commit root, Commit a, Commit b) {
        List<String> sons = root.getSons();
        int point = 0;
        if (root==a || root==b)
            point++;
        for (String son : sons) {
            try {
                Commit temp = getCommit(son);
                point += LCAHelper(temp, a, b);
            } catch (NoSuchCommitException ignored) {}
        }
        if (point == 2) {
            LCA = root;
            return 0;
        }
        return point;
    }

    public void mergeWithBranch(ZonedDateTime timestamp, String hash, String author, String branch) throws NoSuchBranchException, ReverseMergeException, MergeException {
        //不存在要合并的branch，异常
        if (!branches.containsKey(branch))
            throw new NoSuchBranchException();
        //共同祖先
        Commit lca = findLCACommitByBranch(head, branch);
        Commit cur = getHeadCommit();
        Commit object = getCommitByBranch(branch);
        if (cur == object)
            return;
        //如果目标分支是当前分支的祖先，合并失败
        if (lca == object)
            throw new ReverseMergeException();
        //如果当前分支是目标分支的祖先，快进调整当前branch指针即可
        if (lca == cur) {
            branches.put(head, branches.get(branch));
            return;
        }
        //其余情况就是分叉了，目标分支和当前分支不在一条线上
        /*
        1. 祖先和head一样，但是obj不一样的，按照obj来
        2. 祖先和obj一样，但是head不一样的，按照head来
        3. Obj和head一样，但是和祖先不一样的，按照obj来（不动）
        4. Obj和head都无，但是祖先有的文件，删掉
        5. Obj，head，祖先都有的文件（名），但是版本都不一样，冲突，不动并报错
        操你妈，傻逼逻辑，写死我了
         */
        Set<String> lcaFileNames = lca.getFileNames(), curFileNames = cur.getFileNames(), objFileNames = object.getFileNames();
        Map<String, String> mergeResultFiles = new HashMap<>();
        for(String filename : lcaFileNames) {
            if (cur.containsFileName(filename) && object.containsFileName(filename)) {
                String curHash = cur.getHashOfFile(filename),
                        lcaHash = lca.getHashOfFile(filename),
                        objHash = object.getHashOfFile(filename);
                if (curHash.equals(lcaHash) && objHash.equals(lcaHash))
                    mergeResultFiles.put(filename, lcaHash);
                else if (curHash.equals(lcaHash) && !objHash.equals(lcaHash))
                    mergeResultFiles.put(filename, objHash);
                else if (!curHash.equals(lcaHash) && objHash.equals(lcaHash))
                    mergeResultFiles.put(filename, curHash);
                else
                    throw new MergeException(filename);
            } else if (cur.containsFileName(filename) && !object.containsFileName(filename))
                mergeResultFiles.put(filename, cur.getHashOfFile(filename));
            else if (!cur.containsFileName(filename) && object.containsFileName(filename))
                mergeResultFiles.put(filename, object.getHashOfFile(filename));
        }

        for(String filename : curFileNames) {
            if (!lcaFileNames.contains(filename)) {
                if (!objFileNames.contains(filename) || object.getHashOfFile(filename).equals(cur.getHashOfFile(filename)))
                    mergeResultFiles.put(filename, cur.getHashOfFile(filename));
                else
                    throw new MergeException(filename);
            }
        }

        for(String filename : objFileNames) {
            if (!lcaFileNames.contains(filename)) {
                if (!curFileNames.contains(filename) || cur.getHashOfFile(filename).equals(object.getHashOfFile(filename)))
                    mergeResultFiles.put(filename, object.getHashOfFile(filename));
                else
                    throw new MergeException(filename);
            }
        }
        newMergeCommit(timestamp, "merged by "+head+" and "+branch, mergeResultFiles, hash, author, branch);
    }

    private void newMergeCommit(ZonedDateTime timestamp, String log, Map<String, String> commitFiles,
                           String SHA1, String author, String objectBranch) {
        Commit commit = new Commit(timestamp, log, commitFiles, SHA1, author, branches.get(head), branches.get(objectBranch));
        getHeadCommit().addSonCommit(commit.getCommitStr());
        try {
            getCommitByBranch(objectBranch).addSonCommit(commit.getCommitStr());
        } catch (NoSuchBranchException ignored) { }
        commits.put(commit.getCommitStr(), commit);
        branches.put(head, commit.getCommitStr());
        branches.put(objectBranch, commit.getCommitStr());
    }
}