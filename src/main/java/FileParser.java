import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileParser {
    public static void main(String[] args) {
        FileParser parser = new FileParser();
        List<String> configuration = parser.readInConfigurationFile();
        System.out.println(parser.parseConfigurationFile(configuration));
    }

    public List<String> readInConfigurationFile() {
        String fileName = "annotation.conf";
        File file = new File(fileName);
        List<String> lines = new ArrayList<>();

        try {
            Scanner fileScanner = new Scanner(file);
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                lines.add(line);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return lines;
    }
    private List<String> parseConfigurationFile(List<String> configuration) {
        List<String> entityLabels = new ArrayList<>();
        for (String line : configuration) {
            if (line.equals("[entities]") || line.equals("")){
                continue;
            }
            else if (line.equals("[relations]")) {
                //for now only read the entities
                break;
            }
            else {
                entityLabels.add(line);
            }
        }
        return entityLabels;
    }
}
