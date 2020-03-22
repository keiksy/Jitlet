import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class Stage {

    private Path stagePath;
    private int fileNumbers;

    public Stage(Path stagePath) {
        this.stagePath = stagePath;
        fileNumbers = getStagedFiles().size();
    }

    public void addFile(Path filePath) {
        try {
            if (!Files.exists(stagePath.resolve(filePath.getFileName())))
                fileNumbers++;
            Files.copy(filePath, stagePath.resolve(filePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Can not find file: " + filePath.getFileName() + ".");
            System.exit(0);
        }
    }

    public boolean inStaging(String filename) {
        return Files.exists(stagePath.resolve(filename));
    }

    public List<Path> getStagedFiles(){
        List<Path> stagedFilePaths = new ArrayList<>();
        try {
            Files.list(stagePath).filter((p) -> (!p.startsWith("."))).forEach(stagedFilePaths::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stagedFilePaths;
    }

    public int getNumberOfStagedFile() {
        return fileNumbers;
    }

    public void removeFile(String filename) {
        try {
            Files.delete(stagePath.resolve(filename));
            fileNumbers--;
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
                fileNumbers--;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
