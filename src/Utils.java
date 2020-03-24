import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * 工具类，封装一些常用操作
 */

public class Utils {

    /**
     * 分别记录了gitlet文件夹，commit目录，暂存目录和序列化commitChain的文件名称
     * 主要供初始化Gitlet对象时使用，后面一般从对应的对象的getPath()方法中获得路径
     */
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

    /**
     * 从SHA-1字符串中截取后6位
     *
     * 主要作为某次commit的目录名称使用
     * @param hash SHA-1字符串
     * @return hash的后六位
     */
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

    /**
     * 检查工作目录是否已经被初始化
     */
    public static void checkInitialized() {
        if (!(Files.exists(getGitDirPath()) &&
                Files.exists(getCommitPath()) &&
                Files.exists(getStagePath()))) {
            System.err.println("Please init a Gitlet repo first.");
            System.exit(0);
        }
    }

    /**
     * 计算字符串的sha-1值
     * @param str 输入字符串
     * @return 该字符串的sha-1值
     */
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

    /**
     * 基于MD5值判断两个文本文件内容是否一样
     * @param a 第一个文件
     * @param b 第二个文件
     * @return 两者是否一样
     * @throws NoSuchFileException 无法读取其中某一个文件时抛出异常
     */
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

    /**
     * 序列化指定commitChain对象
     *
     * 序列化的位置由本类的getCommitChainPath()函数指定
     * @param cc commitChain对象
     */
    public static void serializeCommitChain(CommitChain cc) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getCommitChainPath().toString()));
            oos.writeObject(cc);
            oos.close();
        } catch (IOException e) {
            System.err.println("Serialize commitChain error.");
        }
    }

    /**
     * 创建指定文件夹
     * @param path 指定路径的path对象
     */
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

    /**
     * 复制并覆盖原有的同名文件，忽略了以.开头的UNIX隐藏文件
     * @param from 源文件夹
     * @param to 目的文件夹
     */
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
