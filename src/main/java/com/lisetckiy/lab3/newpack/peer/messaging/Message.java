package com.lisetckiy.lab3.newpack.peer.messaging;

abstract public class Message {
    protected int type;
    private int priority = 0;

    public Message(){}

    public Message(int type){
        this(type, 0);
    }

    public Message(int type, int priority){
        this.type = type;
        this.priority = priority;
    }

    public int getPriority(){
        return this.priority;
    }

    public int getType(){
        return this.type;
    }

    abstract public byte[] generate();
}