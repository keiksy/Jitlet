import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class Stage {

    private Path stagePath;

    public Stage(Path stagePath) {
        this.stagePath = stagePath;
    }

    public void add(Path filePath) {
        try {
            Files.copy(filePath, stagePath.resolve(filePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Can not find file: " + filePath.getFileName() + ".");
        }
    }

    public List<Path> getStagedFiles(){
        List<Path> stagedFilePaths = new ArrayList<>();
        try {
            Files.list(stagePath).forEach(stagedFilePaths::add);
        } catch (IOException e) {
            System.err.println("Can not fetch staging files");
            System.exit(1);
        }
        return stagedFilePaths;
    }
}
