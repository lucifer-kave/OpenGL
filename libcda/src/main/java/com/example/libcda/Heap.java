package com.example.libcda;

import java.util.Arrays;

public class Heap {
    public static void sort() {
        int[] arr = new int[]{2, 7, 8, 6, 1, 9, 3, 4, 5};
        /**
         * 构建一个初始堆，选出最小的和最大的分别位于堆顶和堆尾
         */
        for (int i = arr.length / 2 - 1; i >= 0; i--) {
            resize(arr, i, arr.length);
        }
        System.out.println("-------");
        /**
         * 每次把最小值和最大值交换位置
         */
        for (int i = arr.length - 1; i >= 0; i--) {
            int tmp = arr[0];
            arr[0] = arr[i];
            arr[i] = tmp;
            resize(arr, 0, i);
        }
    }

    private static void resize(int[] arr, int i, int len) {
        int tmp = arr[i];
        System.out.println(tmp);
        for (int k = 2 * i + 1; k < len; k = 2 * k + 1) {
            if (k + 1 < len && arr[k] < arr[k + 1]) {
                k++;
            }
            if (arr[k] > tmp) {
                arr[i] = arr[k];
                i = k;
            } else {
                break;
            }
        }
        arr[i] = tmp;
        System.out.println(Arrays.toString(arr));
    }

    private static void resize1(int[] arr, int i, int len) {
        int tmp = arr[i];
        System.out.println(tmp);
        for (int k = 2 * i + 1; k < len; k = 2 * k + 1) {
            if (k + 1 < len && arr[k] > arr[k + 1]) {
                k++;
            }
            if (arr[k] < tmp) {
                arr[i] = arr[k];
                i = k;
            } else {
                break;
            }
        }
        arr[i] = tmp;
        System.out.println(Arrays.toString(arr));
    }

    public static void qucksort() {
        int[] arr = new int[]{2, 7, 8, 6, 5, 9, 3, 4, 1};
        qucksort(arr, 0, arr.length - 1);
        System.out.println(Arrays.toString(arr));
    }

    private static void qucksort(int[] arr, int left, int right) {
        int mid;
        if (left < right) {
            mid = prepoint(arr, left, right);
            qucksort(arr, left, mid - 1);
            qucksort(arr, mid + 1, right);
        }
    }

    private static int prepoint(int[] arr, int left, int right) {
        int prepos = arr[left];
        while (left < right) {
            while (left < right && arr[right] >= prepos) {
                right--;
            }
            arr[left] = arr[right];
            while (left < right && arr[left] <= prepos) {
                left++;
            }
            arr[right] = arr[left];

        }
        arr[left] = prepos;
        return left;
    }

    public static void choose() {
        int[] arr = new int[]{2, 7, 8, 6, 1, 9, 3, 4, 5};
        for (int i = 0; i < arr.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[j] > arr[min]) {
                    min = j;
                }
            }
            int tmp = arr[i];
            arr[i] = arr[min];
            arr[min] = tmp;
        }
        System.out.println(Arrays.toString(arr));
    }

    public static Node test() {
        Node nodes = buildNodes();
        Node temp = nodes;
        while (temp != null) {
            System.out.print(temp.val + ",");
            temp = temp.next;
        }
        System.out.print("\n");
        Node head = null;
        Node cur = nodes;
        while (cur != null) {
            Node tmp = cur.next;
            cur.next = head;
            head = cur;
            cur = tmp;
        }
        while (head != null) {
            System.out.print(head.val + ",");
            head = head.next;
        }
        return head;
    }

    private static Node buildNodes() {
        Node head = new Node();
        head.val = 9;
        Node p = head;
        int len = 5;
        while (head.next == null && len >= 0) {
            head.next = new Node();
            head.next.val = len;
            head = head.next;
            len--;
        }
        return p;
    }

    private static class Node {
        int val;
        Node next;
    }
}
