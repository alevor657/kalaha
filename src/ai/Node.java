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
public class Node {
    public ArrayList<Node> children;
    
    public int utility;
    public int depth;
    
    public Node() {
        this.children = new ArrayList<Node>();
    }
}
