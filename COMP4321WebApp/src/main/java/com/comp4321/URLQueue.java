package com.comp4321;

import java.util.LinkedList;
import java.util.Queue;
import javafx.util.Pair;

public class URLQueue {
    private Queue<Pair<String, Boolean>> queue;

    public URLQueue() {
        this.queue = new LinkedList<Pair<String, Boolean>>();
    }

    public boolean contains(String url) {
        if (queue.contains(new Pair<String, Boolean>(url, true)) || queue.contains(new Pair<String, Boolean>(url, false))) {
            return true;
        } else {
            return false;
        }
    }

    public int size() {
        return queue.size();
    }

    public Pair<String, Boolean> poll() {
        return queue.poll();
    }

    public boolean add(String url, boolean hasToIndex) {
        return queue.add(new Pair<String, Boolean>(url, hasToIndex));
    }
}
