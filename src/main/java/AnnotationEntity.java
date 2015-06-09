public class AnnotationEntity {
    private String ID;
    private String label;
    private int startingPosition;
    private int endPosition;
    private String content;

    AnnotationEntity(String annotationLine) {
        annotationLine = annotationLine.trim().replaceAll(" +", " ");
        String[] annotationParts = annotationLine.split("\t");
        this.ID = annotationParts[0];
        this.content = annotationParts[2];

        String[] annotationContent = annotationParts[1].split(" ");
        this.label = annotationContent[0];
        this.startingPosition = Integer.parseInt(annotationContent[1]);
        this.endPosition = Integer.parseInt(annotationContent[2]);

    }

    public String getContent(){
        return content;
    }

    public String getLabel() {
        return label;
    }

    public int getStart() {
        return startingPosition;
    }
}
