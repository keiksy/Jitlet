import Gitlet.Gitlet;

public class TestGitlet {

    public static void main(String[] args) {
        Gitlet.main(new String[]{"init"});
        Gitlet.main(new String[]{"add", "pp.py"});
        Gitlet.main(new String[]{"status"});
    }
}
