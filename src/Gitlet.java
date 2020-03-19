import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.List;

public class Gitlet {

    private static final String GIT_MAIN_DIR_NAME = ".gitlet";
    private static final String COMMIT_DIR_NAME = "commit";
    private static final String STAGE_DIR_NAME = "stage";
    private static final String COMMIT_CHAIN_SERIALIZATION_NAME = ".commitchain";

    CommitChain commitChain;
    Stage stage = new Stage(Paths.get(GIT_MAIN_DIR_NAME, STAGE_DIR_NAME));

    public static void main(String[] args) {
        Gitlet gitapp = new Gitlet();
        if (args.length == 0)
            System.err.println("Please input a command!");

        switch (args[0]) {
            case "init": gitapp.init(); break;
            case "log": gitapp.log(); break;
            case "add": gitapp.add(args[1]); break;
            case "commit": gitapp.commit(args[1]); break;
            default: System.err.println("Please check your command!"); break;
        }
    }

    private void log() {

    }

    private void init() {
        Path p = Paths.get(GIT_MAIN_DIR_NAME);
        try {
            Files.createDirectory(p);
            Files.createDirectory(p.resolve(COMMIT_DIR_NAME));
            Files.createDirectory(p.resolve(STAGE_DIR_NAME));
        } catch (IOException e) {
            System.err.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        commit("initial commit");
    }

    private void add(String filename) {
        Path filePath = Paths.get(filename);
        try {
            stage.add(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String encryptString(String str)  {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] ans = md.digest(str.getBytes());
            BigInteger bi = new BigInteger(1, ans);
            return bi.toString(16);
        } catch (NoSuchAlgorithmException e) {
            return "fuck you";
        }
    }

    private void commit(String msg) {
        ZonedDateTime commitTime = ZonedDateTime.now();
        String hash = encryptString(commitTime.toString()+msg);
        Path destDirPath = Paths.get(GIT_MAIN_DIR_NAME, COMMIT_DIR_NAME, hash.substring(hash.length()-6));

        try {
            Files.createDirectory(destDirPath);
            List<Path> stagedFiles = stage.getStagedFiles();
            if (stagedFiles.size() == 0) {
                System.err.println("No thing to commit, please check you staging area.");
                return;
            }
            for (Path src : stagedFiles) {
                Files.move(src, destDirPath.resolve(src.getFileName()));
            }
            commitChain.addCommit(new Commit(commitTime, msg, destDirPath.toString(), hash, System.getProperty("user.name")));
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Paths.get(GIT_MAIN_DIR_NAME, COMMIT_CHAIN_SERIALIZATION_NAME).toString()));
            oos.writeObject(commitChain);
            oos.close();
        } catch (IOException e) {
            System.err.println("Commit IO Error");
        }
    }
}
