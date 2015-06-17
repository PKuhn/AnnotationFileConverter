import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import org.apache.commons.lang3.StringUtils;

public class FileParser {

    /**
     * Scans directory for all txt files where an according .ann file is present and converts them in
     * a single TSV file named merged.tsv
     * @param path path of directory where txt and ann files should be searched
     */
    public void parseAnnotationFilesInDirectory(String path) {
        List<String> fileNames = getFileNames(path);

        fileNames.stream().filter(fileName -> fileName.contains(".txt")
                && fileNames.contains(StringUtils.substringBefore(fileName, ".txt") + ".ann")).forEach(fileName -> {
            System.out.println("started creating tsv for: " + fileName);
            createTSVFile(StringUtils.substringBefore(fileName, ".txt"), path);
            System.out.println("created tsv for: " + fileName);
        });
        mergeTSVFiles(path);
    }

    /**
     * Merges together all TSV files files in a given directory, produced from single .ann .txt file pairs.
     * These files are separated with a free line according to Standford NER input format
     * @param path path of directory
     */
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

    /**
     * Creates TSV File for a single text. Requires according .ann File to be present.
     * @param textName name of text wanted to convert to TSV format
     * @param path path to directory where .txt and .ann file are
     */
    public static void createTSVFile(String textName, String path) {
        FileParser parser = new FileParser();
        List<AnnotationEntity> entities = parser.readInAnnotationFile(textName, path);
        try {
            List<String> tokens = parser.tokenizeText(textName, path);
            String text = parser.readInText(textName, path);
            List<Integer> startingPositions = parser.findStartingPositionsOfTokens(tokens, text);
            List<String> labels = parser.matchTokens(tokens, entities, startingPositions);
            parser.writeAnnotationsToTSV(tokens, labels, textName, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * Scans directory to list all file names present. This is used to find all pairs of files where an
    * .ann and .txt file is present 
    * @param path path to directory which should be checked for filenames
    * @return all file names as string containing the file ending as well  
    */
    private List<String> getFileNames(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    /**
     * Reads annotation file for a text and generates AnnotationEntities for that file.
     * @param textName name of the text for which the .ann file should be parsed
     * @param path path to directory where name.txt and name.ann are present in this directory
     * @return List of entities in own AnnotationEntity format
     */
    private List<AnnotationEntity> readInAnnotationFile(String textName, String path) {
        String fileName = textName + ".ann";

        List<String> annotations = readFileToLines(fileName, path);
        List<AnnotationEntity> entities= new ArrayList<>();

        int id = 0;
        for (String annotation : annotations) {
            List<AnnotationEntity> entitiesInLine =  parseAnnotationLine(annotation, id);
            entities.addAll(entitiesInLine);
            id += entitiesInLine.size();
        }
        return entities;
    }

    /**
     * Parses one line in the .ann File into a List of AnnotationEntity. This is needed because it is possible to have
     * annotations that contain multiple Elements.
     * @param line Line in .ann file to be parsed. This line is in format ID \t Label StartPosition EndPosition \t Content
     * @param currentIDCounter current annotation ID needed fro create new Annotation
     * @return List of Annotation Entities which then have only one word as content.
     */
    private List<AnnotationEntity> parseAnnotationLine(String line, int currentIDCounter) {
        List<AnnotationEntity> entities = new ArrayList<>();

        line = line.trim().replaceAll(" +", " ");
        String[] annotationParts = line.split("\t");

        String content = annotationParts[2];
        // split content into multiple words if needed
        List<String> contentParts = Arrays.asList(content.split(" "));

        // annotation Information contains [Label StartPosition EndPosition]
        String[] annotationInformation = annotationParts[1].split(" ");
        String label = annotationInformation[0];
        int startPosition = Integer.parseInt(annotationInformation[1]);

        // build annotation for each word
        for (String annotatedWord : contentParts) {
            String ID = "T" + currentIDCounter;
            int startPositionOfPart = startPosition + content.indexOf(annotatedWord);
            int endPositionOfPart = startPositionOfPart + annotatedWord.length();
            AnnotationEntity entity = new AnnotationEntity(ID, annotatedWord, startPositionOfPart,
                    endPositionOfPart, label);
            entities.add(entity);
        }

        return entities;
    }

    /**
     * prints matched tokens and labels generated by machtTokens() to an TSV file which can be read by the Standford NER  
     * @param tokens list of tokens which the Standford PTB Tokenizer produces for the text which should be parsed to TSV 
     * @param labels List of labels, from the annoteted file or "O" for other. This is the output generated of the matchTokens method
     * @param fileName name of TSV file which will be created  
     * @param path output path where the TSV File will be created
     */
    private void writeAnnotationsToTSV(List<String> tokens, List<String> labels, String fileName, String path) {
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

    /**
     * Match the positions of the Standford Tokenizer to the positions in the text to compare
     * them with the positions in the .ann files later on.
     * @param tokens List of token Output generated by Standford tokenizer
     * @param text text for which the tokens where generated
     * @return returns the starting position of the tokens as list of Integer
     */
    private List<Integer> findStartingPositionsOfTokens(List<String> tokens, String text) {
        int tokenIndex = 0;
        int textIndex= 0;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("''") || tokens.get(i).equals("``")) {
                tokens.set(i, "\"");
            }
        }
        List<Integer> startPositions = new ArrayList<Integer>();

        while (tokenIndex < tokens.size() && textIndex < text.length()) {

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

        assert (startPositions.size() == tokens.size());

        return startPositions;
    }

    /**
     * helper method that reads in the text and returns it as string with lines seperated by " " 
     * @param textName name of the text which should be searched
     * @param path directory in which the text should be searched
     * @return text in given text file with lines seperated by " "
     */
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


    /**
     * Method use the generate tokens which have to be labeled to train the Standford NER. This is needed to  
     * partition the text into chunks like expexted from the CRF-Classifier
     * @param textName name of the text which should be tokenized
     * @param path directory in which the text should be searched
     * @return list of tokens produced by PTBTokenizer
     */
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

    /**
     * Converts each word of the tokenized text into the representation of the TSV file
     * (word \t label)
     * @param tokens text to convert in token representation
     * @param annotations annotations parsed from according .ann file
     * @param startPositions starting positions of tokens in text
     * @return list of labels in TSV format
     */
    private List<String> matchTokens(List<String> tokens, List<AnnotationEntity> annotations, List<Integer> startPositions) {
        assert(tokens.size() == startPositions.size());

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
            tokenIndex++;
        }
        return labels;
    }

    /**
     * helper method needed to convert a file to a list of string, where each string represents one line in the file    
     * @param fileName name of the file which should be read in 
     * @param path directory in which the text should be searched
     * @return list if lines of the file as Strings 
     */
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


    /**
     * method needed toread in annotation configurations which can be used to verify if alle the labels present in the current 
     * annotation file are definined and valid. For now it is enough to just read all entities from the configuration file. 
     * Unlike in Brat for the moment the method requires the ConfiguratioNFile to be present in the given location and does not 
     * search for it in higher directories     
     * @param path path to the location of the annotation file  
     * @return list of labels of entities defined in the condifuration file  
     */
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
