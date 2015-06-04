import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class FileParser {
    public static void main(String[] args) {
        FileParser parser = new FileParser();
        System.out.println(parser.parseConfigurationFile());
        List<AnnotationEntity> entities = parser.readInAnnotationFile("");

        /* for (AnnotationEntity entity : entities) {
            System.out.println(entity.getContent());
        } */
        try {
            parser.tokenizeText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<AnnotationEntity> readInAnnotationFile(String textName) {
        textName = "antother_text";
        String fileName = textName + ".ann";

        List<String> annotations = readFileToLines(fileName);
        List<AnnotationEntity> entities= new ArrayList<>();

        for (String annotation : annotations) {
            AnnotationEntity entity = new AnnotationEntity(annotation);
            entities.add(entity);
        }

        return entities;
    }
    private void tokenizeText(String textName) throws IOException {
        textName = "antother_text";
        String fileName = textName += ".txt";

        String text = "";
        //List<String> lines = Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());
        //StringReader reader = new StringReader(text);
        PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(fileName),
                new CoreLabelTokenFactory(), "");
        while (ptbt.hasNext()) {
            CoreLabel label = ptbt.next();
            System.out.println(label);
        }

            //List<CoreLabel> rawWords = tokenizerFactory.getTokenizer(new StringReader(sentence)).tokenize();
        //System.out.println(rawWords.get(0).value());
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
