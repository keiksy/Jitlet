import org.junit.Test;

public class TestGitlet {

    @Test
    public void testRmbranch() {
        Gitlet.main(new String[]{"init"});
        Gitlet.main(new String[]{"add", "pp.py"});
        Gitlet.main(new String[]{"commit", "1st"});
        Gitlet.main(new String[]{"branch", "newb"});
        Gitlet.main(new String[]{"checkout", "newb"});
        Gitlet.main(new String[]{"add", "2nd"});
        Gitlet.main(new String[]{"commit", "2nd"});
        Gitlet.main(new String[]{"add", "4th"});
        Gitlet.main(new String[]{"commit", "4th"});
        Gitlet.main(new String[]{"checkout", "master"});
        Gitlet.main(new String[]{"add", "3rd"});
        Gitlet.main(new String[]{"commit", "3rd"});
        Gitlet.main(new String[]{"rm-branch", "newb"});
        Gitlet.main(new String[]{"global-log"});
    }
}
