import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.ListIterator;

public class Gitlet {

    private static final String GIT_MAIN_DIR_NAME = ".gitlet";
    private static final String COMMIT_DIR_NAME = "commit";
    private static final String STAGE_DIR_NAME = "stage";
    private static final String COMMIT_CHAIN_SERIALIZATION_NAME = ".commitchain";

    CommitChain commitChain = CommitChain.deSerialFromPath(Paths.get(GIT_MAIN_DIR_NAME, COMMIT_CHAIN_SERIALIZATION_NAME));
    Stage stage = new Stage(Paths.get(GIT_MAIN_DIR_NAME, STAGE_DIR_NAME));

    public static void main(String[] args) {
        Gitlet gitapp = new Gitlet();

        switch (args[0]) {
            case "init": gitapp.init(args); break;
            case "log": gitapp.log(args); break;
            case "add": gitapp.add(args); break;
            case "commit": gitapp.commit(args, false); break;
            default: System.err.println("Please check your command!"); break;
        }
    }

    private void checkArgsValid(String[] args, int argsLength) {
        if (args.length != argsLength) {
            if (args.length < argsLength)
                System.err.println("Arguments is less than required length:" + (argsLength-1) + ", please check.");
            else
                System.err.println("Arguments is more than required length:" + (argsLength-1) + ", please check.");
            System.exit(1);
        }
    }

    private void log(String[] args) {
        checkArgsValid(args, 1);
        ListIterator<Commit> endIterator = commitChain.getEndIterator();
        while(endIterator.hasPrevious()) {
            Commit curCommit = endIterator.previous();
            if (commitChain.isHead(curCommit)) System.out.println("****HEAD****");
            System.out.println(curCommit);
            System.out.println();
        }
    }

    private void init(String[] args) {
        checkArgsValid(args, 1);
        Path p = Paths.get(GIT_MAIN_DIR_NAME);
        try {
            Files.createDirectory(p);
            Files.createDirectory(p.resolve(COMMIT_DIR_NAME));
            Files.createDirectory(p.resolve(STAGE_DIR_NAME));
        } catch (IOException e) {
            System.err.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(1);
        }
        commit(new String[]{"commit", "initial commit"}, true);
    }

    private void add(String[] args) {
        checkArgsValid(args, 2);
        String filename = args[1];
        Path filePath = Paths.get(filename);
        stage.add(filePath);
    }

    private static String encryptString(String str)  {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] ans = md.digest(str.getBytes());
            BigInteger bi = new BigInteger(1, ans);
            return bi.toString(16);
        } catch (NoSuchAlgorithmException ignored) {
            return "impossible!";
        }
    }

    private void commit(String[] args, boolean isFirstCommit) {
        checkArgsValid(args, 2);
        String msg = args[1];
        ZonedDateTime commitTime = ZonedDateTime.now();
        String hash = encryptString(commitTime.toString()+msg);
        Path destDirPath = Paths.get(GIT_MAIN_DIR_NAME, COMMIT_DIR_NAME, hash.substring(hash.length()-6));
        List<Path> stagedFiles = stage.getStagedFiles();
        if (!isFirstCommit && stagedFiles.size() == 0) {
            System.err.println("No thing to commit, please check you staging area.");
            System.exit(1);
        }

        try {
            Files.createDirectory(destDirPath);
        } catch (IOException e) {
            System.err.println("Can not create a commit directory.");
            return;
        }

        try {
            for (Path src : stagedFiles) {
                Files.move(src, destDirPath.resolve(src.getFileName()));
            }
            commitChain.addCommit(new Commit(commitTime, msg, destDirPath.toString(), hash, System.getProperty("user.name")));
            String p = Paths.get(GIT_MAIN_DIR_NAME, COMMIT_CHAIN_SERIALIZATION_NAME).toString();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Paths.get(GIT_MAIN_DIR_NAME, COMMIT_CHAIN_SERIALIZATION_NAME).toString()));
            oos.writeObject(commitChain);
            oos.close();
        } catch (IOException e) {
            try {
                Files.delete(destDirPath);
            } catch (IOException ignored) { }
            //System.err.println("Can not create commit files.");
            e.printStackTrace();
        }
    }
}
