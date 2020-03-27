package Gitlet;

import Gitlet.Blobs.Blob;
import Gitlet.Blobs.BlobPool;
import Gitlet.Commits.Commit;
import Gitlet.Commits.CommitChain;
import Gitlet.Utility.Exceptions.*;
import Gitlet.Stage.Stage;
import Gitlet.Utility.Utils;

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
     * 新增一个分支，并让这个分支指向head结点
     * @param args 命令行参数
     */
    private void branch(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            commitChain.addBranch(args[1]);
        } catch (AlreadyExistBranchException e) {
            System.err.println("Can't add the branch because it exsits.");
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
            System.err.println("No such branch named " + args[1]);
            System.exit(0);
        }
        //恢复工作目录到目标commit的状态。
        List<String> hashesOfBackupFiles = commitChain.getHeadCommit().getFiles();
        for (String hash : hashesOfBackupFiles) {
            Blob blob = blobPool.getFile(hash);
            try {
                Files.copy(blob.getPathGit(), blob.getPathRaw(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        stage.clear();
    }

    /**
     * 在commitChain上添加一个Commit结点
     *
     * 首先生成提交时间，SHA-1和本次commit要保存的文件夹路径等必要信息
     * 然后比较上次commit中文件的md5和这次是否一样，如果一样的话，停止commit
     * 然后在commitChain上添加一个Commit结点，具体逻辑由commitChain实现
     *
     * 会对本Repo的第一次commit做出一些特别的优化，减少IO次数，其实并没有什么卵用
     *
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
        if (!isFirstCommit) {
            if (stage.getNumberOfStagedFiles() == 0) {
                System.err.println("No thing to commit, please check you staging area.");
                System.exit(0);
            }
            //下面的代码比较了暂存区的文件内容和上次commit的文件内容是否相同
            //所有暂存区文件的hash值构成的容器
            List<String> lastCommitFiles = commitChain.getHeadCommit().getFiles();
            if (lastCommitFiles.containsAll(stagedFiles)) {
                System.err.println("There is no difference between last commit files and staging files.");
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
     * 创建三个文件夹，然后执行第一次commit
     *
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
     * 按时间逆序打印当前branch上的所有提交
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
     * head指针和当前branch的指针都会指向指定的Commit对象
     * @param args 命令行参数
     */
    private void reset(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            commitChain.resetTo(Utils.fromHash2DirName(args[1]));
        } catch (NoSuchCommitException e) {
            System.err.println("No such commit with hash: " + args[1]);
            return;
        }
        //恢复工作目录到目标commit的状态。
        List<String> hashesOfBackupFiles = commitChain.getHeadCommit().getFiles();
        for (String hash : hashesOfBackupFiles) {
            Blob blob = blobPool.getFile(hash);
            try {
                Files.copy(blob.getPathGit(), blob.getPathRaw(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        stage.clear();
    }

    /**
     * 删除暂存区的指定文件，同时也尝试删除工作目录的对应文件
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
            System.err.println("Now checked out at branch: " + args[1] + ", can't delete it");
            System.exit(0);
        } catch (NoSuchBranchException e) {
            System.err.println("No such branch named " + args[1]);
            System.exit(0);
        }
    }

    /**
     * 打印提交状态，分为三种：
     * 1. 成功暂存的文件
     * 2. 在工作目录但是没有暂存的文件
     * 3. 已经暂存但是在工作区已经被修改或者删除的文件
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
            trackingFiles.add(dirRaw.toString());
            if (!Files.exists(dirRaw))
                deletedFiles.add(dirRaw.toString());
            else if (!Utils.encrypt(dirRaw, "SHA-1").equals(hash))
                modifiedFiles.add(dirRaw.toString());
        }
        //检查未暂存的文件
        try {
            Files.list(Paths.get("")).forEach((path -> {
                String p = path.toString();
                if (!(p.charAt(0)=='.') && !trackingFiles.contains(p) && !modifiedFiles.contains(p) && !deletedFiles.contains(p))
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
        Iterator<Map.Entry<String,Commit>> iterator = commitChain.getAllCommitsIterator();
        while(iterator.hasNext()) {
            Map.Entry<String,Commit> temp = iterator.next();
            if (temp.getValue().getLog().equals(args[1]))
                System.out.println(temp.getValue());
        }
    }
}