import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

public class Commit implements Serializable {

    ZonedDateTime timestamp;
    String message;
    String commitDir;
    String SHA1;
    String author;

    public Commit(ZonedDateTime timestamp, String message, String commitDir, String SHA1, String author) {
        this.timestamp = timestamp;
        this.message = message;
        this.commitDir = commitDir;
        this.SHA1 = SHA1;
        this.author = author;
    }
}
