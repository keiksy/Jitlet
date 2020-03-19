import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

public class CommitChain implements Serializable {

    LinkedList<Commit> chain = new LinkedList<>();
    Path ccPath;

    public static CommitChain deSerialFromPath(Path path) {
        CommitChain commitChain;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(path.toString()));
            commitChain = (CommitChain) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            commitChain = new CommitChain(path);
        }
        return commitChain;
    }

    public CommitChain(Path ccPath) {
        this.ccPath = ccPath;
    }

    public void addCommit(Commit commit) {
        chain.addLast(commit);
    }
}