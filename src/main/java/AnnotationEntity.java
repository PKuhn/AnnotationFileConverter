public class AnnotationEntity {
    private String ID;
    private String label;
    private int startingPosition;
    private int endPosition;
    private String content;

    AnnotationEntity(String annotationLine) {
        annotationLine = annotationLine.trim().replaceAll(" +", " ");
        String[] annotationParts = annotationLine.split(" ");
        this.ID = annotationParts[0];
        this.label = annotationParts[1];
        this.startingPosition = Integer.parseInt(annotationParts[2]);
        this.endPosition = Integer.parseInt(annotationParts[3]);
        this.content = annotationParts[4];
    }

    public String getContent(){
        return content;
    }
}
