package Gitlet.Utility.Exceptions;

public class MergeException extends Exception {

    private String conflictSource;

    public MergeException(String conflictSource) {
        this.conflictSource = conflictSource;
    }

    public String getConflictSource() {
        return conflictSource;
    }
}
