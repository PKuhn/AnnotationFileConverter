import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Scanner;

public class FileParser {
    public static void main(String[] args) {
        FileParser parser = new FileParser();
        parser.readInConfigurationFile();
    }
    public void readInConfigurationFile() {
        String filePath = "~/text_annotation_tools/brat-v1.3_Crunchy_Frog/data/testdata/annotation.conf";
        File file = new File(filePath);
        System.out.println(file.exists());
        System.out.println(file.getName());
        System.out.println(file.canRead());
        System.out.println(file.getAbsolutePath());
        file.setReadable(true);
        try {
            Scanner fileScanner = new Scanner(file);
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                System.out.println(line);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
