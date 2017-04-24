package project.entities;

public enum DataType {

    TEXT(1),
    EMOTICON(2),
    LINK(3),
    HASHTAG(4),
    DATE(5),
    QUOTE(7);

    int id;

    DataType(int id){
        this.id = id;
    }
}
