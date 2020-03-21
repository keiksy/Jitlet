import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Iterator;

public class Playground {
    public static void main(String[] args) {
        Iterator<Path> iterator;
        int a = 3;
        try {
            iterator =  Files.list(Paths.get("")).iterator();
            while(iterator.hasNext()) {
                System.out.println(iterator.next().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
