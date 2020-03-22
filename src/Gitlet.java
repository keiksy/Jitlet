import java.io.*;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.*;

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
            case "init": gitapp.init(args); break;
            case "log": gitapp.log(args); break;
            case "rm": gitapp.rm(args); break;
            //case "find": gitapp.find(args); break;
            case "status": gitapp.status(args);break;
            default: System.err.println("Please check your command!"); break;
        }
    }

    private void add(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        stage.addFile(Paths.get(args[1]));
    }

    private void branch(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        commitChain.newBranch(args[1]);
        Utils.serializeCommitChain(commitChain);
    }

    private void checkout(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        commitChain.changeBranchTo(args[1]);
        Utils.serializeCommitChain(commitChain);
    }

    //todo:commit的逻辑交给cc去做，这里只负责整体文件读写和控制台输出。
    private void commit(String[] args, boolean isFirstCommit) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        //commit必要信息
        String log = args[1];
        ZonedDateTime commitTime = ZonedDateTime.now();
        String hash = Utils.encrypt2SHA1(commitTime.toString());
        Path destDirPath = Utils.getCommitPath().resolve(hash.substring(hash.length()-6));
        if (!isFirstCommit && stage.getNumberOfStagedFile()==0) {
            System.err.println("No thing to commit, please check you staging area.");
            return;
        }
        //比较上次commit中文件的md5和这次是否一样，如果一样的话，停止commit
        List<Path> stagedFiles = stage.getStagedFiles();
        boolean isDiff = false;
        for (Path src : stagedFiles) {
            Path temp = Paths.get(commitChain.getHeadCommit().getCommitDir(), src.getFileName().toString());
            if (!Utils.isSameFiles(src, temp)) {
                isDiff = true;
                break;
            }
        }
        if (!isFirstCommit && !isDiff) {
            System.err.println("There is no difference between head commit and staging files.");
            System.err.println("Any changes won't be made.");
            return;
        }
        Utils.createDir(destDirPath);
        stage.moveStagedFileTo(destDirPath);
        commitChain.newCommit(commitTime, log, destDirPath.toString(), hash, System.getProperty("user.name"));
        Utils.serializeCommitChain(commitChain);
    }

    private void init(String[] args) {
        Utils.checkArgsValid(args, 1);
        Utils.createDir(Utils.getGitDirPath());
        Utils.createDir(Utils.getCommitPath());
        Utils.createDir(Utils.getStagePath());
        commitChain.newBranch("master");
        commit(new String[]{"commit", "initial commit"}, true);
        commitChain.changeBranchTo("master");
    }

    private void log(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 1);
        Iterator<Commit> endIterator = commitChain.iterator();
        //DFS正向打印
        while(endIterator.hasNext()) {
            Commit curCommit = endIterator.next();
            if (commitChain.isHead(curCommit)) System.out.println("****HEAD****");
            System.out.println(curCommit);
            System.out.println("---------------------------------------------");
        }
    }

    private void rm(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        stage.removeFile(args[1]);
        try {
            Files.delete(Paths.get(args[1]));
        } catch (IOException ignored) { }
    }

    //TODO: refactor 去除隐藏文件夹的显示
    private void status(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 1);
        System.out.println("on branch " + commitChain.getHeadCommit().getBranchName() + "\n");
        List<Path> stageFiles = stage.getStagedFiles();
        List<Path> untrackFiles = new ArrayList<>();
        List<Path> modifiedFiles = new ArrayList<>();
        Iterator<Path> i = stageFiles.iterator();
        while(i.hasNext()) {
            Path p = i.next();
            Path temp = p.getFileName();
            if (!Utils.isSameFiles(temp, p)) {
                modifiedFiles.add(temp);
                i.remove();
            }
        }
        try {
            i = Files.list(Paths.get("")).filter((p) -> (!p.startsWith("."))).iterator();
            while(i.hasNext()) {
                Path p = i.next();
                Path temp = Utils.getStagePath().resolve(p);
                if (!Files.exists(temp)) untrackFiles.add(p);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        String pattern = args[1];
        LinkedList<Commit> results = commitChain.findCommitWithLog(pattern);
        if (results.size() == 0) {
            System.err.println("Found no commit with that message.");
        } else {
            for(Commit c : results)
                System.out.println(c.getSHA1());
        }
    }

}
