import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    private static final String GIT_MAIN_DIR_NAME = ".gitlet";
    private static final String COMMIT_DIR_NAME = "commit";
    private static final String STAGE_DIR_NAME = "stage";
    private static final String COMMIT_CHAIN_SERIALIZATION_NAME = ".commitchain";

    public static Path getGitDirPath() {
        return Paths.get(GIT_MAIN_DIR_NAME);
    }

    public static Path getCommitPath() {
        return getGitDirPath().resolve(COMMIT_DIR_NAME);
    }

    public static Path getStagePath() {
        return getGitDirPath().resolve(STAGE_DIR_NAME);
    }

    public static Path getCommitChainPath() {
        return getGitDirPath().resolve(COMMIT_CHAIN_SERIALIZATION_NAME);
    }

    public static void checkArgsValid(String[] args, int argsLength) {
        if (args.length != argsLength) {
            if (args.length < argsLength)
                System.err.println("Arguments is less than required length:" + (argsLength-1) + ", please check.");
            else
                System.err.println("Arguments is more than required length:" + (argsLength-1) + ", please check.");
            System.exit(0);
        }
    }

    public static void checkInitialized() {
        if (!(Files.exists(Paths.get(GIT_MAIN_DIR_NAME)) &&
                Files.exists(Paths.get(GIT_MAIN_DIR_NAME, COMMIT_DIR_NAME)) &&
                Files.exists(Paths.get(GIT_MAIN_DIR_NAME, STAGE_DIR_NAME)))) {
            System.err.println("Please init a Gitlet repo first.");
            System.exit(0);
        }
    }

    public static String encrypt2SHA1(String str)  {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] ans = md.digest(str.getBytes());
            BigInteger bi = new BigInteger(1, ans);
            return bi.toString(16);
        } catch (NoSuchAlgorithmException ignored) {
            return "impossible!";
        }
    }

    public static String encrypt2MD5(Path file) {
        try {
            byte[] ans = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file));
            BigInteger bi = new BigInteger(1, ans);
            return bi.toString(16);
        } catch (NoSuchAlgorithmException ignored) {
            return "impossible!";
        } catch (IOException e) {
            e.printStackTrace();
            return "impossible!";
        }
    }

    public static boolean isSameFiles(Path a, Path b) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] abytes = md.digest(Files.readAllBytes(a));
            byte[] bbytes = md.digest(Files.readAllBytes(b));
            String amd5 = new BigInteger(1, abytes).toString(16);
            String bmd5 = new BigInteger(1, bbytes).toString(16);
            return amd5.equals(bmd5);
        } catch (IOException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    public static void serializeCommitChain(CommitChain cc) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Paths.get(GIT_MAIN_DIR_NAME, COMMIT_CHAIN_SERIALIZATION_NAME).toString()));
            oos.writeObject(cc);
            oos.close();
        } catch (IOException e) {
            System.err.println("Serialize commitChain error.");
        }
    }

    public static void createDir(Path path) {
        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
