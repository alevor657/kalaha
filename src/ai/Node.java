/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai;

import java.util.ArrayList;

/**
 *
 * @author Alexey and Uttejh
 * 
 * This is a node to be used in the tree
 */
public class Node {
    public ArrayList<Node> children;
    
    public int utility;
    public int depth;
    public String mode;
    public int alpha;
    public int beta;
    
    public Node() {
        this.children = new ArrayList<Node>();
        this.utility = Integer.MIN_VALUE;
        this.alpha = Integer.MIN_VALUE;
        this.beta = Integer.MAX_VALUE;
    }
    
    public Node clone() {
        Node newNode = new Node();
        newNode.utility = this.utility;
        newNode.depth = this.depth;
        newNode.mode = this.mode;
        
        for (Node node : this.children) {
            newNode.children.add(this.clone());
        }
        
        return newNode;
    }
}
