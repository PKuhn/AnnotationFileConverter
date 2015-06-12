import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

import org.apache.commons.lang3.StringUtils;

public class FileParser {
    public static void main(String[] args) {
        String path = args[0];
        convertFileFromSlashTagToAnn(path);
        /*FileParser parser = new FileParser();
        List<String> fileNames = parser.getFileNames(path);

        fileNames.stream().filter(fileName -> fileName.contains(".txt")
                && fileNames.contains(StringUtils.substringBefore(fileName, ".txt") + ".ann")).forEach(fileName -> {
            System.out.println("started creating tsv for: " + fileName);
            createTSVFile(StringUtils.substringBefore(fileName, ".txt"), path);
            System.out.println("created tsv for: " + fileName);
        });
        mergeTSVFiles(path);
        */
    }
    public static void convertFileFromSlashTagToAnn(String filePath) {
        List<String> lines = readFileToLines("SlashTags.txt",filePath);
        List<String> annotations = lines.stream()
                                        .map(FileParser::convertLineToAnn)
                                        .flatMap(Collection::stream)
                                        .collect(Collectors.toList());
        System.out.println(annotations);
    }

    public static List<String> convertLineToAnn(String line) {
        List<String> taggedLines = new ArrayList<>();
        List<String> annotations = Arrays.asList(line.split(" "));

        annotations.stream().forEach((annotation) -> {
            String[] annotationParts = annotation.split("/");
            String annotationInAnnFormat = annotationParts[0] +"\t" + annotationParts[1];
            taggedLines.add(annotationInAnnFormat);
        });

        return taggedLines;
    }

    public static void mergeTSVFiles(String path) {
        FileParser parser = new FileParser();
        List<String> fileNames = parser.getFileNames(path);
        String fileName = "merged";
        try {
            fileName += ".tsv";
            PrintWriter writer = new PrintWriter(path + File.separator + fileName, "UTF-8");
            fileNames.stream().filter(file -> file.contains(".tsv")).forEach(file -> {
                List<String> lines = readFileToLines(file, path);
                lines.forEach(writer::println);
                writer.println();
            });

            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public static void createTSVFile(String textName, String path) {
        FileParser parser = new FileParser();
        List<AnnotationEntity> entities = parser.readInAnnotationFile(textName, path);

        try {
            List<String> tokens = parser.tokenizeText(textName, path);
            String text = parser.readInText(textName, path);
            List<Integer> startingPositions = parser.findStartingPositionsOfTokens(tokens, text);
            List<String> labels = parser.matchTokens(tokens, entities, startingPositions);
            parser.writeAnnotationsToFile(tokens, labels, textName, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFileNames(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }
    public List<AnnotationEntity> readInAnnotationFile(String textName, String path) {
        String fileName = textName + ".ann";

        List<String> annotations = readFileToLines(fileName, path);
        List<AnnotationEntity> entities= new ArrayList<>();

        for (String annotation : annotations) {
            AnnotationEntity entity = new AnnotationEntity(annotation);
            entities.add(entity);
        }

        return entities;
    }

    private void writeAnnotationsToFile(List<String> tokens, List<String> labels, String fileName, String path) {
        try {
            fileName += ".tsv";
            PrintWriter writer = new PrintWriter(path + File.separator + fileName, "UTF-8");
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

    private String readInText(String textName, String path) {
        String fileName = textName + ".txt";
        String text = "";
        try {
            List<String> lines = Files.readAllLines(Paths.get(path + File.separator +fileName));
            text = String.join(" ", lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    private List<String> tokenizeText(String textName, String path) throws IOException {
        String fileName = textName + ".txt";

        List<String> tokens = new ArrayList<>();

        PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(path+ File.separator +fileName),
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
    private static List<String> readFileToLines(String fileName, String path) {
        File file = new File(path + File.separator + fileName);
        List<String> lines = new ArrayList<>();


        Scanner fileScanner = null;
        try {
            fileScanner = new Scanner(file);
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                lines.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return lines;
    }


    private List<String> parseConfigurationFile(String path) {
        List<String> configuration = readFileToLines("annotation.conf", path);
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
