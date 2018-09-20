/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai;

import java.util.ArrayList;

/**
 *
 * @author user
 */
public class Tree {
    public ArrayList<Node> nodes;
    
    public Node getNodeById(String id) {
        Node foundNode = null;
        
        for (Node node : this.nodes) {
            if (node.id.equals(id)) {
                foundNode = node;
            }            
        }
        
        return foundNode;
    }
    
    public void save(Node node) {
        this.nodes.add(node);
    }
}
