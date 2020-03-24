import Exceptions.*;

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

    private static final long serialVersionUID = 11111111L;

    private HashMap<String, Commit> commits;
    private HashMap<String, String> branches;
    private Commit chain;
    private Commit head;
    private String curBranchName;

    /**
     * 从指定路径反序列化commitChain对象
     *
     * 如果读不到，就实例化一个新的commitChain返回
     * @param ccPath 指定路径
     * @return 反序列化/新生成的commitChain对象的引用
     */
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

    /**
     * 向commitChain的head指针后添加一个新的Commit对象，然后head指向这个新的对象
     * 同时当前branch也要指向这个新的对象
     *
     * 处理了当前chain指向为空，即当前commitChain为空（刚初始化）的特殊情况
     * @param timestamp 时间戳信息
     * @param log log信息
     * @param commitDir 本commit保存的目录名
     * @param SHA1 sha-1字符串
     * @param author commit的作者
     */
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

    /**
     * 获取head指针指向的commit的引用
     */
    public Commit getHeadCommit() {
        return head;
    }

    /**
     * 使用commitStr获得对应的Commit对象
     * @param commitStr 六位commitStr字符串
     * @return
     * @throws NoSuchCommitException 如果找不到对应Commit对象，抛出此异常
     */
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

    /**
     * 删除指定名称的branch
     *
     * 先查hashmap获得commitStr，再根据commitStr获得Commit对象
     * 然后在这个Commit对象上倒着走遍历，如果遇到的Commit被另一个branch指向
     * 或者这个Commit的孩子数量大于1，就停止遍历，然后删掉刚才遍历过的所有Commit对象
     * @param branch branch名称字符串
     * @throws DeleteCurrentBranchException 如果要删除的branch就是当前head指向Commit的branch，不可以删除
     */
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

    /**
     * 将head指针指向commitStr对应的Commit对象
     * @throws NoSuchCommitException
     */
    public void resetTo(String commitStr) throws NoSuchCommitException{
        head = getCommitByStr(commitStr);
        branches.put(curBranchName, commitStr);
    }

    /**
     * 将head指针指向指定branch所指向的Commit对象
     */
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

    /**
     * 主要的迭代器，实现了"倒着走"的功能
     */
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

    /**
     * 获取从指定commitStr对应的Commit对象开始的迭代器
     *
     * 就是从commitStr对应的那个Commit对象开始"倒着走"
     */
    private Iterator<Commit> getIteratorFromCommit(String commitStr) {
        return new CommitIterator(commitStr);
    }
}