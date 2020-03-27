package Gitlet;

import Gitlet.Blobs.Blob;
import Gitlet.Blobs.BlobPool;
import Gitlet.Commits.Commit;
import Gitlet.Commits.CommitChain;
import Gitlet.Utility.Exceptions.*;
import Gitlet.Stage.Stage;
import Gitlet.Utility.Utils;
import jdk.jshell.execution.Util;

import java.io.*;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Gitlet的主类，Gitlet从这里启动并接受命令实现所有功能
 *
 * Gitlet类实例化时，会首先尝试从当前工作目录下读取.commitChain文件
 * 该文件是Gitlet底层数据结构的序列化文件
 * 然后在commitChain引用的基础上完成业务操作
 * stage对象负责维护暂存区的相关状态和操作
 *
 * @author keiksy
 */

public class Gitlet {

    private static BlobPool blobPool = BlobPool.deSerialFrom(Utils.getBlobsPath());
    private static CommitChain commitChain = CommitChain.deSerialFrom(Utils.getCommitChainPath());
    private static Stage stage = Stage.deSerialFrom(Utils.getStageFilePath());

    public static void main(String[] args) {
        Gitlet gitapp = new Gitlet();

        if (args.length == 0) {
            System.err.println("Please enter a command.");
            return;
        }
        switch (args[0]) {
            case "add": gitapp.add(args); break;
            case "branch": gitapp.branch(args); break;
            case "checkout": gitapp.checkout(args); break;
            case "commit": gitapp.commit(args, false); break;
            case "find": gitapp.find(args); break;
            case "global-log": gitapp.globalLog(args); break;
            case "init": gitapp.init(args); break;
            case "log": gitapp.log(args); break;
            case "merge": gitapp.merge(args); break;
            case "reset": gitapp.reset(args); break;
            case "rm": gitapp.rm(args); break;
            case "rm-branch": gitapp.rmBranch(args); break;
            case "status": gitapp.status(args);break;
            default: System.err.println("No command with that name exists."); break;
        }
        Utils.serializeAll(commitChain, stage, blobPool);
    }

    /**
     * 暂存（跟踪）指定文件
     * @param args 命令行参数
     */
    private void add(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            stage.trackFile(Paths.get(args[1]));
            blobPool.addFile(Paths.get(args[1]));
        } catch (IOException e) {
            System.err.println("No such file with name: " + args[1] + ".");
            System.exit(0);
        }
    }

    /**
     * 新增一个分支，并让这个分支指向head所指向的commit
     * @param args 命令行参数
     */
    private void branch(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            commitChain.addBranch(args[1]);
        } catch (AlreadyExistBranchException e) {
            System.err.println("A branch with that name already exists.");
            System.exit(0);
        }
    }

    /**
     * 切换到指定分支
     * @param args 命令行参数
     */
    private void checkout(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            commitChain.changeBranchTo(args[1]);
        } catch (NoSuchBranchException e) {
            System.err.println("No such branch exists.");
            System.exit(0);
        }
        Utils.syncFilesWithHeadCommit(commitChain, blobPool);
        stage.clear();
    }

    /**
     * 在commitChain上添加一个Commit结点
     *
     * 首先生成提交时间，SHA-1和本次commit要保存的文件夹路径等必要信息
     * 然后比较上次commit中文件的hash和这次是否一样，如果一样的话，停止commit
     * 然后在commitChain上添加一个Commit结点，具体逻辑由commitChain实现
     * @param args 命令行参数
     * @param isFirstCommit 指示本次commit是否为本Repo的第一次commit
     */
    private void commit(String[] args, boolean isFirstCommit) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        String log = args[1];
        ZonedDateTime commitTime = ZonedDateTime.now();
        String hash = Utils.encrypt(commitTime.toString(), "SHA-1");
        List<String> stagedFiles = stage.getHashesOfStagedFiles();
        List<String> lastCommitFiles = commitChain.getHeadCommit().getFiles();
        //第一次提交不需要检查提交文件的状况，因为没有上次提交，暂存区也不会有任何文件
        if (!isFirstCommit) {
            //如果跟踪文件为0个或者这次提交的文件和上次完全一样，就不用提交了
            if (stage.getNumberOfStagedFiles()==0 ||
                    (lastCommitFiles.containsAll(stagedFiles) && (lastCommitFiles.size()==stagedFiles.size()))) {
                System.err.println("No changes added to the commit.");
                System.exit(0);
            }
        }
        commitChain.newCommit(commitTime, log, stagedFiles, hash, System.getProperty("user.name"));
    }

    /**
     * 打印本Repo中所有的提交记录
     * @param args 命令行参数
     */
    private void globalLog(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 1);
        Iterator<Map.Entry<String, Commit>> commitIterator = commitChain.getAllCommitsIterator();
        while(commitIterator.hasNext()) {
            Commit temp = commitIterator.next().getValue();
            if (commitChain.isHead(temp)) System.out.println("****current HEAD****");
            System.out.println(temp);
            System.out.println("===");
        }
    }

    /**
     * 初始化Repo
     *
     * 创建2个文件夹，然后执行第一次commit
     * @param args 命令行参数
     */
    private void init(String[] args) {
        Utils.checkArgsValid(args, 1);
        try {
            Files.createDirectory(Utils.getGitDirPath());
            Files.createDirectory(Utils.getFilesPath());
        } catch (IOException e) {
            System.err.println("A Gitlet.Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        commit(new String[]{"commit", "initial commit"}, true);
    }

    /**
     * 按时间逆序打印当前branch上的所有提交历史
     * @param args 命令行参数
     */
    private void log(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 1);
        System.out.println("****current HEAD****");
        for (Commit temp : commitChain) {
            System.out.println(temp);
            System.out.println("===");
        }
    }

    private void merge(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);

    }

    /**
     * 将head改变到指定commit，同时文件夹内容也会恢复到commit时的快照内容
     *
     * 指定commit内所有文件快照都会被复制到它们原来所在的目录，替代现有的版本（如果现在存在的话）
     * @param args 命令行参数
     */
    private void reset(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            commitChain.resetTo(Utils.fromHash2DirName(args[1]));
        } catch (NoSuchCommitException e) {
            System.err.println("No commit with that id exists.");
            System.exit(0);
        }
        Utils.syncFilesWithHeadCommit(commitChain, blobPool);
        stage.clear();
    }

    /**
     * 删除暂存区的指定文件，同时也删除工作目录的对应文件
     * @param args 命令行参数
     */
    private void rm(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            String hashOfRemovedFile = stage.untrackFile(Paths.get(args[1]));
            blobPool.rmFile(hashOfRemovedFile);
        } catch (NotStagedException e) {
            System.err.println("Not staged yet.");
            System.exit(0);
        } catch (IOException e) {
            System.err.println("No file with this path exists.");
            System.exit(0);
        }
    }

    /**
     * 删除指定分支
     * @param args 命令行参数
     */
    private void rmBranch(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            commitChain.deleteBranch(args[1]);
        } catch (DeleteCurrentBranchException e) {
            System.err.println("Cannot remove the current branch.");
            System.exit(0);
        } catch (NoSuchBranchException e) {
            System.err.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }

    /**
     * 打印状态，分为三种：
     * 1. 跟踪中的文件
     * 2. 已经暂存但是在工作区已经被修改或者删除的文件
     * 3. 工作目录中没有被跟踪的文件
     * @param args 命令行参数
     */
    private void status(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 1);
        List<String> hashesOfStagedFiles = stage.getHashesOfStagedFiles();
        List<String> untrackFiles = new ArrayList<>(), modifiedFiles = new ArrayList<>(),
                deletedFiles = new ArrayList<>(), trackingFiles = new ArrayList<>();
        //检查已暂存文件的跟踪情况
        for(String hash : hashesOfStagedFiles) {
            Path dirRaw = blobPool.getFile(hash).getPathRaw();
            //只要还在暂存区里，就是正在跟踪的文件
            trackingFiles.add(dirRaw.toString());
            //用户使用shell的命令删除或移动了文件，导致原路径的文件找不到了，那就标记为被删除
            if (!Files.exists(dirRaw))
                deletedFiles.add(dirRaw.toString());
            //文件还在，但是跟暂存区的最新版本不一样了，那就是被修改过了，但是还没暂存
            else if (!Utils.encrypt(dirRaw, "SHA-1").equals(hash))
                modifiedFiles.add(dirRaw.toString());
        }
        //检查工作目录下未跟踪的文件
        try {
            //一个文件，如果他不属于上面三种的任何一个，就是未跟踪的文件
            Files.list(Paths.get("")).forEach((path -> {
                String p = path.toString();
                if (!(p.equals(Utils.GIT_DIR_NAME)) && !trackingFiles.contains(p) && !modifiedFiles.contains(p) && !deletedFiles.contains(p))
                    untrackFiles.add(p);
            }));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println("current working branch: " + commitChain.getCurBranchName());
        System.out.println();
        System.out.println("tracking files:");
        trackingFiles.forEach(System.out::println);
        System.out.println();
        System.out.println("Staged but modified files:");
        modifiedFiles.forEach(System.out::println);
        System.out.println();
        System.out.println("Staged but removed files:");
        deletedFiles.forEach(System.out::println);
        System.out.println();
        System.out.println("Untracked files:");
        untrackFiles.forEach(System.out::println);
    }

    /**
     * 遍历所有Commit对象，打印出具有指定log的Commit对象
     * @param args
     */
    private void find(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        boolean noSuchCommit = true;
        Iterator<Map.Entry<String,Commit>> iterator = commitChain.getAllCommitsIterator();
        while(iterator.hasNext()) {
            Map.Entry<String,Commit> temp = iterator.next();
            if (temp.getValue().getLog().equals(args[1])) {
                System.out.println(temp.getValue());
                noSuchCommit = false;
            }
        }
        if (noSuchCommit)
            System.out.println("Found no commit with that message.");
    }
}