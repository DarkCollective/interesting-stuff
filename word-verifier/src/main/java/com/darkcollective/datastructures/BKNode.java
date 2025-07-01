package com.darkcollective.datastructures;

import java.util.HashMap;
import java.util.Map;

public class BKNode {
    private final String word;
    private final Map<Integer, BKNode> children;

    public BKNode(String word) {
        this.word = word;
        this.children = new HashMap<>();
    }

    public String getWord() {
        return word;
    }

    public Map<Integer, BKNode> getChildren() {
        return children;
    }

    public boolean hasChild(int distance) {
        return children.containsKey(distance);
    }

    public BKNode getChild(int distance) {
        return children.get(distance);
    }

    public void addChild(int distance, BKNode child) {
        children.put(distance, child);
    }
}
