package Gitlet.Stage;

import Gitlet.Utility.Exceptions.NotStagedException;
import Gitlet.Utility.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.*;
import java.util.*;

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
            return new Stage();
        }
    }

    public void trackFile(Path file) {
        String filename = file.getFileName().toString();
        String sha1 = Utils.encrypt(file, "SHA-1");
        tracking.put(filename, sha1);
    }

    public List<String> getHashesOfStagedFiles(){
        return new ArrayList<>(tracking.values());
    }

    //不能直接暴露，重写
    public Map<String, String> getTracking() { return tracking; }

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
