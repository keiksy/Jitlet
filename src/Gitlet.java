import Exceptions.*;

import java.io.*;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

/**
 * gitlet类负责初始化环境，IO和错误输出相关工作
 * Utils类封装一些常用操作和辅助函数
 * 具体业务逻辑交给commitchain类和stage类去执行
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

    private void add(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            stage.addFile(Paths.get(args[1]));
        } catch (NoSuchFileException e) {
            System.err.println("No such file with name: " + args[1] + ".");
        }
    }

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
            //比较上次commit中文件的md5和这次是否一样，如果一样的话，停止commit
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

    private void init(String[] args) {
        Utils.checkArgsValid(args, 1);
        Utils.createDir(Utils.getGitDirPath());
        Utils.createDir(Utils.getCommitPath());
        Utils.createDir(stage.getStagePath());
        commit(new String[]{"commit", "initial commit"}, true);
    }

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

    private void rm(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        stage.removeFile(args[1]);
        try {
            Files.delete(Paths.get(args[1]));
        } catch (IOException ignored) { }
    }

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

    //不够优雅，可是我也不知道该怎么重构了，法克
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

    private void find(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        List<Commit> ans = commitChain.findCommitWithLog(args[1]);
        ans.forEach(System.out::println);
    }
}