package org.example.skiplist;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class Node {
    final Integer key;

    // Массив ссылок, на каждом уровне своя атомарная ссылка.
    final AtomicMarkableReference<Node>[] next;

    // Макс уровень
    static final int MAX_LEVEL = 16;

    public Node(int key, int height) {
        this.key = key;
        @SuppressWarnings("unchecked")
        AtomicMarkableReference<Node>[] tmp =
                (AtomicMarkableReference<Node>[]) new AtomicMarkableReference[height + 1];
        for (int i = 0; i <= height; i++) {
            tmp[i] = new AtomicMarkableReference<>(null, false);
        }
        this.next = tmp;
    }

    public int getHeight() {
        return next.length - 1;
    }

    @Override
    public String toString() {
        return "Node{" + "key=" + key + ", height=" + getHeight() + '}';
    }
}
