import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class Stage {

    private Path stagePath;

    public Stage(Path stagePath) {
        this.stagePath = stagePath;
    }

    public void add(Path filePath) throws IOException{
        Files.copy(filePath, stagePath.resolve(filePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    public List<Path> getStagedFiles() throws IOException{
        List<Path> stagedFilePaths = new ArrayList<>();
        Files.list(stagePath).forEach(stagedFilePaths::add);
        return stagedFilePaths;
    }
}
