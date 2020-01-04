


public class GeneratePom {
    public static void main(String[] args) {
        String dirpath = "D:/workspace";
        MakePomFromJars mp = new MakePomFromJars(dirpath);
        try {
            mp.getJarsXml();
        }
        catch (Exception err) {
            err.printStackTrace();
        }

    }
}
