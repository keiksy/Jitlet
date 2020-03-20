import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.ListIterator;

public class CommitChain implements Serializable {

    LinkedList<Commit> chain = new LinkedList<>();
    Commit head;
    String ccPath;

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
        this.ccPath = ccPath.toString();
    }

    public void addCommit(Commit commit) {
        chain.addLast(commit);
        head = commit;
    }

    public ListIterator<Commit> getEndIterator() {
        ListIterator<Commit> iterator = chain.listIterator();
        while (iterator.hasNext())
            iterator.next();
        return iterator;
    }

    public boolean isHead(Commit commit) {
        return commit == head;
    }
}