import Exceptions.NoSuchBranchException;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        commitChain.newBranch(ZonedDateTime.now(), System.getProperty("user.name"), args[1]);
        Utils.serializeCommitChain(commitChain);
    }

    private void checkout(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        try {
            commitChain.changeHead(args[1]);
        } catch (NoSuchBranchException e) {
            System.err.println("No branch named " + args[1] +".");
            return;
        }
        Utils.serializeCommitChain(commitChain);
    }

    private void commit(String[] args, boolean isFirstCommit) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        String log = args[1];
        ZonedDateTime commitTime = ZonedDateTime.now();
        String hash = Utils.encrypt2SHA1(commitTime.toString());
        Path destDirPath = Utils.getCommitPath().resolve(hash.substring(hash.length()-6));
        List<Path> stagedFiles = stage.getStagedFiles();
        if (!isFirstCommit && stagedFiles.size() == 0) {
            System.err.println("No thing to commit, please check you staging area.");
            return;
        }
        //比较上次commit中文件的md5和这次是否一样，如果一样的话，停止commit
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
        try {
            Files.createDirectory(Utils.getGitDirPath());
            Files.createDirectory(Utils.getCommitPath());
            Files.createDirectory(Utils.getStagePath());
        } catch (FileAlreadyExistsException e) {
            System.err.println("A Gitlet version-control system already exists in the current directory.");
            return;
        } catch (IOException e) {
            System.err.println("IO Error when init");
        }
        branch(new String[]{"branch", "master"});
    }

    private void log(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 1);
        Iterator<Commit> endIterator = commitChain.iterator();
        while(endIterator.hasNext()) {
            Commit curCommit = endIterator.next();
            if (curCommit.isBranchHead()) continue;
            if (commitChain.isHead(curCommit)) System.out.println("****HEAD****");
            System.out.println(curCommit);
            System.out.println("---------------------------------------------");
        }
    }

    private void rm(String[] args) {
        Utils.checkInitialized();
        Utils.checkArgsValid(args, 2);
        boolean inStaging = Files.exists(Utils.getStagePath().resolve(args[1]));
        if (inStaging) {
            try {
                Files.delete(Utils.getStagePath().resolve(args[1]));
                Files.delete(Paths.get(args[1]));
            } catch (IOException ignored) { }
        } else {
            System.err.println("There isn't any file named " + args[1] + ", please check your spelling.");
        }
    }

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
            if (!Files.exists(temp)) continue;
            if (!Utils.encrypt2MD5(temp).equals(Utils.encrypt2MD5(p))) {
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
