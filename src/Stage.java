import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Stage {

    private Path stagePath;

    public Stage(Path stagePath) {
        this.stagePath = stagePath;
    }

    public void addFile(Path filePath) {
        try {
            Files.copy(filePath, stagePath.resolve(filePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Can not find file: " + filePath.getFileName() + ".");
            System.exit(0);
        }
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

    public void moveStagedFileTo(Path destDirPath) {
        List<Path> stagedFiles = getStagedFiles();
        try {
            for (Path src : stagedFiles)
                Files.move(src, destDirPath.resolve(src.getFileName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
