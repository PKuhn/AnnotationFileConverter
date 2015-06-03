import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileParser {
    public static void main(String[] args) {
        FileParser parser = new FileParser();
        System.out.println(parser.parseConfigurationFile());
        List<AnnotationEntity> entities = parser.readInAnnotationFile("");
        for (AnnotationEntity entity : entities) {
            System.out.println(entity.getContent());
        }
    }

    public List<AnnotationEntity> readInAnnotationFile(String textName) {
        textName = "antother_text";
        String fileName = textName + ".ann";

        List<String> annotations = readFileToLines(fileName);
        List<AnnotationEntity> entities= new ArrayList<>();

        for (String annotation : annotations) {
            AnnotationEntity entity = new AnnotationEntity(annotation);
            System.out.println(annotation);
        }

        return entities;
    }

    private List<String> readFileToLines(String fileName) {
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


    private List<String> parseConfigurationFile() {
        List<String> configuration = readFileToLines("annotation.conf");
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
