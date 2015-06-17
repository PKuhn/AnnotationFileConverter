public class AnnotationEntity {
    private String ID;
    private String label;
    private int startingPosition;
    private int endPosition;
    private String content;

/**
     * constructor that takes all relevant information for an annotation entity
     * @param ID of the annotation
     * @param content of the annotation
     * @param startPos of the annotation
     * @param endPos of the annotation
     * @param label of the annotation
     * @return AnnotationEnity with all properties set
     */

    AnnotationEntity(String ID, String content, int startPos, int endPos, String label){
        this.ID = ID;
        this.content = content;
        this.label = label;
        this.startingPosition = startPos;
        this.endPosition = endPos;
    };

    
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
