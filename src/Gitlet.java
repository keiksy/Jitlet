import Exceptions.*;

import java.io.*;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

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

    private CommitChain commitChain = CommitChain.deSerialFromPath(Utils.getCommitChainPath());
    private Stage stage = new Stage(Utils.getStagePath());

    public static void main(String[] args) {
        Gitlet gitapp = new Gitlet();

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
            default: System.err.println("Unknown command."); break;
        }
    }

    /**
     * 将指定的文件复制到暂存区目录
     * @param args 命令行参数
     */
    private void add(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            stage.addFile(Paths.get(args[1]));
        } catch (NoSuchFileException e) {
            System.err.println("No such file with name: " + args[1] + ".");
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
            return;
        }
        Utils.serializeCommitChain(commitChain);
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
            return;
        }
        Utils.copyAndReplace(Paths.get(commitChain.getHeadCommit().getCommitDir()), Paths.get(""));
        Utils.serializeCommitChain(commitChain);
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
        String hash = Utils.encrypt2SHA1(commitTime.toString());
        Path destDirPath = Utils.getCommitPath().resolve(Utils.fromHash2DirName(hash));
        if (!isFirstCommit) {
            if (stage.getNumberOfStagedFile() == 0) {
                System.err.println("No thing to commit, please check you staging area.");
                return;
            }
            List<Path> stagedFiles = stage.getStagedFiles();
            boolean isDiff = false;
            for (Path src : stagedFiles) {
                Path temp = Paths.get(commitChain.getHeadCommit().getCommitDir(), src.getFileName().toString());
                try {
                    if (!Utils.isSameFiles(src, temp)) {
                        isDiff = true;
                        break;
                    }
                } catch (IOException e) {
                    isDiff = true;
                    break;
                }
            }
            if (!isDiff) {
                System.err.println("There is no difference between head commit and staging files.");
                System.err.println("Any changes won't be made.");
                return;
            }
        }
        Utils.createDir(destDirPath);
        if (!isFirstCommit) stage.moveStagedFileTo(destDirPath);
        commitChain.newCommit(commitTime, log, destDirPath.toString(), hash, System.getProperty("user.name"));
        Utils.serializeCommitChain(commitChain);
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
        Utils.createDir(Utils.getGitDirPath());
        Utils.createDir(Utils.getCommitPath());
        Utils.createDir(stage.getStagePath());
        commit(new String[]{"commit", "initial commit"}, true);
    }

    /**
     * 按时间逆序打印当前branch上的所有提交
     * @param args 命令行参数
     */
    private void log(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 1);
        Iterator<Commit> endIterator = commitChain.iterator();
        while(endIterator.hasNext()) {
            Commit temp = endIterator.next();
            if (commitChain.isHead(temp)) System.out.println("****current HEAD****");
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
        Utils.copyAndReplace(Paths.get(commitChain.getHeadCommit().getCommitDir()), Paths.get(""));
        stage.clear();
        Utils.serializeCommitChain(commitChain);
    }

    /**
     * 删除暂存区的指定文件，同时也尝试删除工作目录的对应文件
     * @param args 命令行参数
     */
    private void rm(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        stage.removeFile(args[1]);
        try {
            Files.delete(Paths.get(args[1]));
        } catch (IOException ignored) { }
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
            return;
        } catch (NoSuchBranchException e) {
            System.err.println("No such branch named " + args[1]);
            return;
        }
        Utils.serializeCommitChain(commitChain);
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
        //检查stage中和主文件夹的同步情况
        List<Path> stageFiles = stage.getStagedFiles();
        List<Path> untrackFiles = new ArrayList<>();
        List<Path> modifiedFiles = new ArrayList<>();
        Iterator<Path> i = stageFiles.iterator();
        while(i.hasNext()) {
            Path p = i.next();
            Path temp = p.getFileName();
            try {
                if (!Utils.isSameFiles(temp, p)) {
                    modifiedFiles.add(temp);
                    i.remove();
                }
            } catch (NoSuchFileException ignored) { }
        }
        //检查主文件夹下没有在stage中出现过的文件
        try {
            Stream<Path> files = Files.list(Paths.get("")).filter(path -> !(path.getFileName().toString().charAt(0)=='.'));
            i = files.iterator();
            while(i.hasNext()) {
                Path p = i.next();
                Path temp = stage.getStagePath().resolve(p);
                if (!Files.exists(temp)) untrackFiles.add(p);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("current working branch: " + commitChain.getCurBranchName());
        System.out.println();
        System.out.println("Staged to be committed files:");
        stageFiles.forEach((p) -> System.out.println(p.getFileName().toString()));
        System.out.println();
        System.out.println("Untracked files:");
        untrackFiles.forEach((p) -> System.out.println(p.getFileName().toString()));
        System.out.println();
        System.out.println("Staged but modified files:");
        modifiedFiles.forEach((p) -> System.out.println(p.getFileName().toString()));
    }

    /**
     * 遍历所有Commit对象，打印出具有指定log的Commit对象
     * @param args
     */
    private void find(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        List<Commit> ans = commitChain.findCommitWithLog(args[1]);
        ans.forEach(System.out::println);
    }
}