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
    public void newCommit(ZonedDateTime timestamp, String log, List<String> commitFiles,
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
            return getCommit(branches.get(head));
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

    /**
     * 获取从指定commitStr对应的Commit对象开始的迭代器
     *
     * 就是从commitStr对应的那个Commit对象开始"倒着走"
     */
    private Iterator<Commit> getIteratorFromCommit(String commitStr) {
        return new CommitIterator(commitStr);
    }
}