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

import org.apache.commons.lang3.StringUtils;

public class FileParser {
    public static void main(String[] args) {
        FileParser parser = new FileParser();
        List<String> fileNames = parser.getFileNames();
        System.out.println(fileNames);
        for (String fileName : fileNames) {
            if (fileName.contains(".txt") && fileNames.contains(StringUtils.substringBefore(fileName, ".txt") +".ann")) {
                System.out.println("started creating tsv for: " + fileName);
                createTSVFile(StringUtils.substringBefore(fileName, ".txt"));
                System.out.println("created tsv for: " + fileName);
            }
        }
    }

    public static void createTSVFile(String textName) {
        FileParser parser = new FileParser();
        List<AnnotationEntity> entities = parser.readInAnnotationFile(textName);

        try {
            List<String> tokens = parser.tokenizeText(textName);
            String text = parser.readInText(textName);
            List<Integer> startingPositions = parser.findStartingPositionsOfTokens(tokens, text);
            List<String> labels = parser.matchTokens(tokens, entities, startingPositions);
            parser.writeAnnotationsToFile(tokens, labels);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFileNames() {
        File folder = new File("/home/patrick/text_annotation_tools/brat-v1.3_Crunchy_Frog/data/testdata");
        File[] files = folder.listFiles();
        List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }
    public List<AnnotationEntity> readInAnnotationFile(String textName) {
        String fileName = textName + ".ann";

        List<String> annotations = readFileToLines(fileName);
        List<AnnotationEntity> entities= new ArrayList<>();

        for (String annotation : annotations) {
            AnnotationEntity entity = new AnnotationEntity(annotation);
            entities.add(entity);
        }

        return entities;
    }

    private void writeAnnotationsToFile(List<String> tokens, List<String> labels, String fileName) {
        try {
            fileName += "tsv";
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            for (int i = 0; i < tokens.size(); i++) {
                if (labels.size() <= i) {
                    writer.println(tokens.get(i) + "\t" + "O");
                } else {
                    writer.println(tokens.get(i) + "\t" + labels.get(i));
                }
            }
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> findStartingPositionsOfTokens(List<String> tokens, String text) {
        int tokenIndex = 0;
        int textIndex= 0;
        List<Integer> startPositions = new ArrayList<Integer>();

        while (tokenIndex < tokens.size() && textIndex <= text.length()) {

            String nextToken = tokens.get(tokenIndex);
            int tokenSize = nextToken.length();

            String nextTextToken = text.substring(textIndex, textIndex + tokenSize);

            //skip text characters as long as not next token is reached
            if (!nextTextToken.equals(nextToken)) {
                textIndex++;
                continue;
            }

            startPositions.add(textIndex);
            //set textIndex after recoqnized token and go to next token
            textIndex += tokenSize;
            tokenIndex++;
        }

        if (startPositions.size() != tokens.size()) {
            throw new RuntimeException();
        }

        return startPositions;
    }

    private String readInText(String textName) {
        String fileName = textName += ".txt";
        String text = "";
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName));
            text = String.join(" ", lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    private List<String> tokenizeText(String textName) throws IOException {
        String fileName = textName += ".txt";

        List<String> tokens = new ArrayList<>();

        PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(fileName),
                new CoreLabelTokenFactory(), "");
        while (ptbt.hasNext()) {
            CoreLabel label = ptbt.next();
            tokens.add(label.value());
        }

        return tokens;
    }

    private List<String> matchTokens(List<String> tokens, List<AnnotationEntity> annotations, List<Integer> startPositions) {
        if (tokens.size() != startPositions.size()) {
            throw new RuntimeException();
        }

        List<String> labels = new ArrayList<>();
        int tokenIndex = 0;

        for (AnnotationEntity annotation : annotations) {
            String token = tokens.get(tokenIndex);
            int textPosition = startPositions.get(tokenIndex);

            while (!(token.equals(annotation.getContent()) && textPosition == annotation.getStart())) {
                tokenIndex++;
                labels.add("O");
                textPosition = startPositions.get(tokenIndex);
                token = tokens.get(tokenIndex);
            }
            labels.add(annotation.getLabel());
        }
        return labels;
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
