package Gitlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class pg {

    public static void main(String[] args) throws IOException {
        Path cur = Paths.get(".");
        //List<Path> subs = Files.walk(cur).filter((path -> !Files.isDirectory(path))).collect(Collectors.toList());
        System.out.println(cur.toAbsolutePath());
        int a = 1;
    }
}
