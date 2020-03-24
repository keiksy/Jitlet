import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static final String GIT_DIR_NAME = ".gitlet";
    public static final String COMMIT_DIR_NAME = "commit";
    public static final String STAGE_DIR_NAME = "stage";
    public static final String COMMIT_CHAIN_SERIALIZATION_NAME = ".commitchain";

    public static Path getGitDirPath() {
        return Paths.get(GIT_DIR_NAME);
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

    public static String fromHash2DirName(String hash) { return hash.substring(hash.length()-6); }

    public static void checkArgsValid(String[] args, int argsLength) {
        if (args.length != argsLength) {
            if (args.length < argsLength)
                System.err.println("Arguments is less than required length: " + (argsLength-1) + ".");
            else
                System.err.println("Arguments is more than required length: " + (argsLength-1) + ".");
            System.exit(0);
        }
    }

    public static void checkInitialized() {
        if (!(Files.exists(getGitDirPath()) &&
                Files.exists(getCommitPath()) &&
                Files.exists(getStagePath()))) {
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

    public static boolean isSameFiles(Path a, Path b) throws NoSuchFileException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] abytes = md.digest(Files.readAllBytes(a));
            byte[] bbytes = md.digest(Files.readAllBytes(b));
            String amd5 = new BigInteger(1, abytes).toString(16);
            String bmd5 = new BigInteger(1, bbytes).toString(16);
            return amd5.equals(bmd5);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new NoSuchFileException("No such file");
        }
    }

    public static void serializeCommitChain(CommitChain cc) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getCommitChainPath().toString()));
            oos.writeObject(cc);
            oos.close();
        } catch (IOException e) {
            System.err.println("Serialize commitChain error.");
        }
    }

    public static void createDir(Path path) {
        try {
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException e) {
            System.err.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static void copyAndReplace(Path from, Path to) {
        List<Path> srcFiles = new ArrayList<>();
        try {
            Files.list(from).filter((p) -> (!p.getFileName().startsWith("."))).forEach(srcFiles::add);
            for (Path src : srcFiles) {
                Files.copy(src, to.resolve(src.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
