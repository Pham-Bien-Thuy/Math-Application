package com.example.ttv.chatbox;

import android.graphics.Bitmap;

public class Message {
    public static String SENT_BY_ME = "me";
    public static String SENT_BY_BOT = "bot";

    String text; // Change from 'message' to 'text'
    String sentBy;
    Bitmap image;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }
    public Bitmap getImage(){
        return image;
    }
    public void setImage(Bitmap image){
        this.image = image;
    }

    public Message(String text, String sentBy, Bitmap image) {
        this.text = text;
        this.sentBy = sentBy;
        this.image = image;
    }
}
