package project.entities;

public class Message {

    private int messageId;
    private Topic topic;
    private Author author;
    private Message reference = null;
    private int orderNum;
    private String timestamp;
}
