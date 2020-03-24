import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 抽象暂存区相关操作的类
 * 事实上只是一个抽象了暂存区IO操作的类，并不记录和校验任何信息
 */
public class Stage {

    private Path stagePath;

    public Stage(Path stagePath) {
        this.stagePath = stagePath;
    }

    public Path getStagePath() {
        return stagePath;
    }

    public void addFile(Path filePath) throws NoSuchFileException {
        try {
            Files.copy(filePath, stagePath.resolve(filePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new NoSuchFileException("some words useless");
        }
    }

    public List<Path> getStagedFiles(){
        List<Path> stagedFilePaths = new ArrayList<>();
        try {
            Files.list(stagePath).filter((p) -> (!p.getFileName().startsWith("."))).forEach(stagedFilePaths::add);
        } catch (IOException ignored) { }
        return stagedFilePaths;
    }

    public int getNumberOfStagedFile() {
        return getStagedFiles().size();
    }

    public void clear() {
        List<Path> temp = getStagedFiles();
        temp.forEach((path -> removeFile(path.toString())));
    }

    public void removeFile(String filename) {
        try {
            Files.delete(stagePath.resolve(filename));
        } catch (IOException e) {
            System.err.println("No file named " + filename + " in staging area!");
            System.exit(0);
        }
    }

    public void moveStagedFileTo(Path destDirPath) {
        List<Path> stagedFiles = getStagedFiles();
        try {
            for (Path src : stagedFiles) {
                Files.move(src, destDirPath.resolve(src.getFileName()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
