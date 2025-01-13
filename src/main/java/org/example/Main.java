package org.example;

import org.example.skiplist.SkipList;

public class Main {
    public static void main(String[] args) {
        SkipList skipList = new SkipList();

        skipList.insert(10);
        skipList.insert(5);
        skipList.insert(20);

        System.out.println("Search(10): " + skipList.search(10));
        System.out.println("Search(15): " + skipList.search(15));
        System.out.println("Remove(10): " + skipList.remove(10));
        System.out.println("Search(10): " + skipList.search(10));
        System.out.println("Size: " + skipList.getSize());
    }
}
