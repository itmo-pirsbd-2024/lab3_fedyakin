package org.example.skiplist;

import lombok.Getter;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class SkipList {
    private static final double PROBABILITY = 0.5; // Вероятность увеличения уровня

    // Голова и хвост
    private final Node head;
    private final Node tail;

    // Для генерации случайных уровней
    private final Random random = ThreadLocalRandom.current();

    // Подсчитываем количество элементов
    @Getter
    private final AtomicInteger size = new AtomicInteger(0);

    public SkipList() {
        head = new Node(Integer.MIN_VALUE, Node.MAX_LEVEL);
        tail = new Node(Integer.MAX_VALUE, Node.MAX_LEVEL);

        // Связать все уровни head -> tail
        for (int i = 0; i <= Node.MAX_LEVEL; i++) {
            head.next[i].set(tail, false);
        }
    }

    /**
     * Поиск элемента по ключу.
     */
    public boolean search(int key) {
        Node current = head;
        for (int level = Node.MAX_LEVEL; level >= 0; level--) {
            boolean[] markHolder = {false};
            Node next;
            while (true) {
                next = current.next[level].getReference();
                markHolder[0] = current.next[level].isMarked();
                if (next == null) break;
                if (markHolder[0]) {
                    // Если помечена ссылка, значит узел логически удалён, уходим
                    break;
                }
                if (next.key < key) {
                    current = next;
                } else {
                    break;
                }
            }
        }
        current = current.next[0].getReference();
        return (current != null && current.key == key && !current.next[0].isMarked());
    }

    /**
     * Вставка элемента (если его нет).
     * @return true, если элемент вставлен, false, если такой ключ уже был
     */
    public boolean insert(int key) {
        Node[] preds = new Node[Node.MAX_LEVEL + 1];
        Node[] succs = new Node[Node.MAX_LEVEL + 1];

        while (true) {
            boolean found = find(key, preds, succs);
            if (found) {
                return false; // ключ уже есть, не повторяемся
            }
            int newLevel = randomLevel();
            Node newNode = new Node(key, newLevel);

            // Привязываем новый узел к succ на каждом уровне
            for (int level = 0; level <= newLevel; level++) {
                Node succ = succs[level];
                newNode.next[level].set(succ, false);
            }

            // Сшиваем предшественников с новым узлом
            for (int level = 0; level <= newLevel; level++) {
                Node pred = preds[level];
                Node succ = succs[level];
                while (true) {
                    // Читаем текущее состояние
                    boolean[] markHolder = {false};
                    Node ref = pred.next[level].get(markHolder);
                    // Проверяем, что пред всё ещё указывает на succ
                    if (!markHolder[0] && ref == succ) {
                        // CAS: если успешно, идём дальше
                        if (pred.next[level].compareAndSet(succ, newNode, false, false)) {
                            break;
                        }
                    } else {
                        // если CAS не прошёл — пересобираем preds и succs и начинаем заново
                        find(key, preds, succs);
                        break;
                    }
                }
            }
            // Увеличиваем size (без блокировки)
            size.incrementAndGet();
            return true;
        }
    }

    /**
     * Удаление узла по ключу.
     */
    public boolean remove(int key) {
        Node[] preds = new Node[Node.MAX_LEVEL + 1];
        Node[] succs = new Node[Node.MAX_LEVEL + 1];
        Node nodeToRemove;
        boolean isMarked;

        while (true) {
            boolean found = find(key, preds, succs);
            if (!found) {
                return false; // нет такого ключа
            }
            nodeToRemove = succs[0];

            // Помечаем все уровни выше 0
            for (int level = nodeToRemove.getHeight(); level >= 1; level--) {
                boolean[] markHolder = {false};
                Node succ = nodeToRemove.next[level].get(markHolder);
                while (!markHolder[0]) {
                    nodeToRemove.next[level].attemptMark(succ, true);
                    succ = nodeToRemove.next[level].get(markHolder);
                }
            }

            // Помечаем 0-й уровень
            boolean[] markHolder = {false};
            Node succ = nodeToRemove.next[0].get(markHolder);
            while (true) {
                isMarked = nodeToRemove.next[0].compareAndSet(succ, succ, false, true);
                succ = nodeToRemove.next[0].get(markHolder);
                if (isMarked) {
                    size.decrementAndGet();
                    // «откусываем» узел из skip list
                    find(key, preds, succs);
                    return true;
                } else if (markHolder[0]) {
                    // уже помечен
                    return false;
                }
            }
        }
    }

    /**
     * Вспомогательный метод, чтобы найти место вставки/удаления.
     *
     * @param key   ключ
     * @param preds предшественники
     * @param succs преемники
     * @return true, если элемент с таким ключом найден
     */
    private boolean find(int key, Node[] preds, Node[] succs) {
        Node pred;
        Node curr = null;
        Node succ;
        boolean[] marked = {false};
        boolean snip;

        retry:
        for (int level = Node.MAX_LEVEL; level >= 0; level--) {
            pred = head;
            curr = pred.next[level].getReference();
            while (true) {
                if (curr == tail) {
                    preds[level] = pred;
                    succs[level] = tail;
                    break;
                }
                succ = curr.next[level].get(marked);
                // Удалён ли curr?
                while (marked[0]) {
                    snip = pred.next[level].compareAndSet(curr, succ, false, false);
                    if (!snip) {
                        // Перезапустим поиск
                        continue retry;
                    }
                    curr = pred.next[level].getReference();
                    if (curr == tail) {
                        break;
                    }
                    succ = curr.next[level].get(marked);
                }
                if (curr == tail || curr.key >= key) {
                    preds[level] = pred;
                    succs[level] = curr;
                    break;
                }
                pred = curr;
                curr = succ;
            }
        }
        return (curr != tail && curr.key == key && !curr.next[0].isMarked());
    }

    /**
     * Генерация случайного уровня (0..MAX_LEVEL)
     */
    private int randomLevel() {
        int lvl = 0;
        while (lvl < Node.MAX_LEVEL && random.nextDouble() < PROBABILITY) {
            lvl++;
        }
        return lvl;
    }
}
