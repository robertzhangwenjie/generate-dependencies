import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.jsoup.Jsoup;
import com.alibaba.fastjson.JSONObject;
public class MakePomFromJars {

    private final String dirname;

    public MakePomFromJars(String dirname) {
        this.dirname = dirname;
    }//            StringBuffer sb = new StringBuffer(jar.getName());


    public void getJarsXml() throws FileNotFoundException, IOException {
        Element dependencys = new DOMElement("dependencys");
        File dir = new File(this.dirname); //需生成pom.xml 文件的 lib路径
        List<File> jars = getJars(dir);

        for (File jar : jars) {
            JarInputStream jis = new JarInputStream(new FileInputStream(jar));
            Manifest mainmanifest = jis.getManifest();
            jis.close();
            String bundleName = null;
            String bundleVersion = null;
            try {
                bundleName = mainmanifest.getMainAttributes().getValue("Bundle-Name");
                bundleVersion = mainmanifest.getMainAttributes().getValue("Bundle-Version");
            }catch (NullPointerException err) {
                err.printStackTrace();
            }
            Element ele = null;
            StringBuffer sb = new StringBuffer(jar.getName());
            if (bundleName != null) {
                bundleName = bundleName.toLowerCase().replace(" ", "-");
                sb.append(bundleName + "\t").append(bundleVersion);
                ele = getDependices(bundleName, bundleVersion);
                System.out.println(ele.asXML());
            }
            if (ele == null || ele.elements().size() == 0) {
                bundleName = "";
                bundleVersion = "";
                String[] ns = jar.getName().replace(".jar", "").split("-");
                for (String s : ns) {
                    if (Character.isDigit(s.charAt(0))) {
                        bundleVersion += s + "-";
                    } else {
                        bundleName += s + "-";
                    }
                }
                if (bundleVersion.endsWith("-")) {
                    bundleVersion = bundleVersion.substring(0, bundleVersion.length() - 1);
                }
                if (bundleName.endsWith("-")) {
                    bundleName = bundleName.substring(0, bundleName.length() - 1);
                }
                ele = getDependices(bundleName, bundleVersion);
                sb.setLength(0);
                sb.append(bundleName + "\t").append(bundleVersion);
            }

            ele = getDependices(bundleName, bundleVersion);
            if (ele.elements().size() == 0) {
                ele.add(new DOMElement("groupId").addText("not find"));
                ele.add(new DOMElement("artifactId").addText(bundleName));
                ele.add(new DOMElement("version").addText(bundleVersion));
            }
            dependencys.add(ele);
        }
        System.out.println(dependencys.asXML());
    }


    private static List<File> getJars(File dir) {
        /*
            如果传入的为文件且后缀为.jar，则直接将该文件add到jars
         */
        List<File> result_list = new ArrayList<File>();
        if (dir.isDirectory()) {
            List<File> jars_list = new ArrayList<File>();
            for (File file: dir.listFiles()) {
                jars_list.addAll(getJars(file));
            }
            return jars_list;
        }
        if (dir.isFile() && dir.getName().endsWith(".jar")) {
            result_list.add(dir);
        }
        return result_list;
    }


    private static Element getDependices(String key, String ver) {
        Element dependency = new DOMElement("dependency");
        // 设置代理
        // System.setProperty("http.proxyHost", "127.0.0.1");
        // System.setProperty("http.proxyPort", "8090");
        try {
            String url = "http://search.maven.org/solrsearch/select?q=a%3A%22" + key + "%22%20AND%20v%3A%22" + ver + "%22&rows=3&wt=json";
            org.jsoup.nodes.Document doc = Jsoup.connect(url).ignoreContentType(true).timeout(30000).get();
            String elem = doc.body().text();
            JSONObject response = JSONObject.parseObject(elem).getJSONObject("response");
            if (response.containsKey("docs") && response.getJSONArray("docs").size() > 0) {
                JSONObject docJson = response.getJSONArray("docs").getJSONObject(0);
                Element groupId = new DOMElement("groupId");
                Element artifactId = new DOMElement("artifactId");
                Element version = new DOMElement("version");
                groupId.addText(docJson.getString("g"));
                artifactId.addText(docJson.getString("a"));
                version.addText(docJson.getString("v"));
                dependency.add(groupId);
                dependency.add(artifactId);
                dependency.add(version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dependency;
    }
}

