package Gitlet.Stage;

import Gitlet.Utility.Exceptions.NotStagedException;
import Gitlet.Utility.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 抽象暂存区相关操作的类
 */
public class Stage implements Serializable {

    //tracking files list.
    //map the name of a file to the hash of the newest version of the file.
    private Map<String, String> tracking = new HashMap<>();

    public static Stage deSerialFrom(Path path) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(path.toString()));
            return (Stage) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void trackFile(List<Path> files) {
        for(Path file : files) {
            String s = Utils.getRelativeDir(file);
            String sha1 = Utils.encrypt(file, "SHA-1");
            tracking.put(s, sha1);
        }
    }

    public List<String> getHashesOfStagedFiles(){
        return new ArrayList<>(tracking.values());
    }

    public Map<String, String> getTrackingFiles() { return Map.copyOf(tracking); }

    public int getNumberOfStagedFiles() {
        return tracking.size();
    }

    public void clear() {
        tracking.clear();
    }

    public String untrackFile(Path file) throws NotStagedException{
        String filename = file.getFileName().toString();
        if (!tracking.containsKey(filename))
            throw new NotStagedException();
        return tracking.remove(filename);
    }
}
