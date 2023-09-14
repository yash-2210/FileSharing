package com.example.botimei;

import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

public class HuffmanCoding {
    public static HuffmanNode buildHuffmanTree(String text) {
        Map<Character, Integer> frequencyMap = new HashMap<>();
        for (char c : text.toCharArray()) {
            frequencyMap.put(c, frequencyMap.getOrDefault(c, 0) + 1);
        }

        Queue<HuffmanNode> priorityQueue = new PriorityQueue<>();
        for (Entry<Character, Integer> entry : frequencyMap.entrySet()) {
            priorityQueue.add(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        while (priorityQueue.size() > 1) {
            HuffmanNode left = priorityQueue.poll();
            HuffmanNode right = priorityQueue.poll();
            HuffmanNode mergedNode = new HuffmanNode('\0', left.frequency + right.frequency);
            mergedNode.left = left;
            mergedNode.right = right;
            priorityQueue.add(mergedNode);
        }

        return priorityQueue.poll();
    }

    public static void buildHuffmanCodes(HuffmanNode node, String currentCode, Map<Character, String> huffmanCodes) {
        if (node == null) {
            return;
        }

        if (node.character != '\0') {
            huffmanCodes.put(node.character, currentCode);
            return;
        }

        buildHuffmanCodes(node.left, currentCode + "0", huffmanCodes);
        buildHuffmanCodes(node.right, currentCode + "1", huffmanCodes);
    }

    public static String encodeHuffman(String text, Map<Character, String> huffmanCodes) {
        StringBuilder encodedText = new StringBuilder();
        for (char c : text.toCharArray()) {
            encodedText.append(huffmanCodes.get(c));
        }
        return encodedText.toString();
    }

    public static String decodeHuffman(String encodedText, HuffmanNode huffmanTree) {
        StringBuilder decodedText = new StringBuilder();
        HuffmanNode currentNode = huffmanTree;

        for (char bit : encodedText.toCharArray()) {
            if (bit == '0') {
                currentNode = currentNode.left;
            } else {
                currentNode = currentNode.right;
            }

            if (currentNode.character != '\0') {
                decodedText.append(currentNode.character);
                currentNode = huffmanTree;
            }
        }

        return decodedText.toString();
    }

    public static void main(String[] args) {
        String inputStr = "3744ac02980ed301;Dhruvish:+1 814-441-8548 Dr. Ajay:+91 98 24 122642; address:IMLICIND body:Premium for Policy No. 830558475 of Rs. *****1660.40 is due of 15/03/2018. You can pay online at www.licindia.in or from mobile at www.licindia.in/mobile.;";

        HuffmanNode huffmanTree = buildHuffmanTree(inputStr);
        Map<Character, String> huffmanCodes = new HashMap<>();
        buildHuffmanCodes(huffmanTree, "", huffmanCodes);

        String encodedStr = encodeHuffman(inputStr, huffmanCodes);
        System.out.println("Encoded String:");
        System.out.println(encodedStr);

        String decodedStr = decodeHuffman(encodedStr, huffmanTree);
        System.out.println("\nDecoded String:");
        System.out.println(decodedStr);
    }
}
