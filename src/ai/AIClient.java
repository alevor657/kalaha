package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;
import kalaha.*;


/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    
    private Tree tree; 
    	
    /**
     * Creates a new client.
     */
    public AIClient()
    {
	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    
    /**
     * Construct a tree for the game by using provided Node class.
     * Game tree is built by playing the game.
     * Adds utility values to the leaf nodes based on the difference between scores.
     * 
     * @param Node root node for the tree
     * @param GameState state of the game
     * @param int current depth level
     */
    public void idfs(Node root, GameState board){
        int count=1;
        Instant starts = Instant.now();
        long timer = 0;
        Node newNode = root.clone();
        
        while(timer<5){
            Instant ends = Instant.now();
            timer = Duration.between(starts, ends).getSeconds();
            this.tree.root = newNode;
            newNode = constructTreeEfficient(newNode, board, 0, count, starts);
//            newNode = updatedRoot.clone();
            
            count++;
        }
    }
    public Node constructTreeEfficient(Node root, GameState board, int depth, int threshold,Instant starts){
        int localDepth = 0;
        boolean isOurTurn = this.player == board.getNextPlayer();
        
        for (int k=1; k<=6; k++){
            if(root.alpha<root.beta){
                GameState newBoard = board.clone();
                if (!newBoard.moveIsPossible(k)) {
                    continue;
                };
                newBoard.makeMove(k);

                Node newNode = new Node();
                newNode.mode = isOurTurn ? "max" : "min";
                newNode.parent = root;
                newNode.alpha = root.alpha;
                newNode.beta = root.beta;

                int opponent = this.player == 1 ? 2 : 1;
                newNode.utility = newBoard.getScore(this.player) - newBoard.getScore(opponent);
                
                root.children.add(newNode);
                Instant ends = Instant.now();
                long timer = Duration.between(starts, ends).getSeconds();
                if (depth < threshold && (timer<5)) {

                    constructTreeEfficient(newNode, newBoard.clone(), depth+1, threshold, starts);
                    if (root.mode == "max") {
                        
                        
                        if(root.alpha<newNode.utility){

                            root.alpha = newNode.utility;
                        }
                    } else if (root.mode == "min") {
                        
                        if(root.beta>newNode.utility){
                            root.beta = newNode.utility;
                            
                        }
                    }
                }else{
                    System.out.println(depth);
                }
            }else{
                System.out.println("Pruned");
                break;
            }
        }
        
        if(root.children.isEmpty()){
            System.out.println("I am a leaf");
        }else{
            ArrayList utils = new ArrayList();
            for (Node node : root.children) {
                utils.add(node.utility);
            }

            if (root.mode == "max") {
                int max = (int)Collections.max(utils);
                root.utility = max;
//                if(root.alpha<max){
//                    
//                    root.alpha = max;
//                }
            } else if (root.mode == "min") {
                int min = (int)Collections.min(utils);
                root.utility = min;
//                 System.out.println("3");
//                root.beta=root children utility
//                if(root.beta>min){
//                    root.beta = min;
//                     System.out.println("4");
//                }
            }
        }
        
        return root;
    }
    
    /**
     * Propagates utility values up to the top nodes.
     * 
     * @param Node root node.
     */
//    public void calculateUtility(Node root)
//    {
//        if (!root.children.isEmpty()) {
//            // Go down the tree
//            for (Node node : root.children) {
//                calculateUtility(node);
//            }
//            
//            // Find the penultimate layer (second last)
//            if (this.allContainUtility(root.children)) {
//                ArrayList utils = new ArrayList();
//                
//                for (Node node : root.children) {
//                    utils.add(node.utility);
//                }
//                
//                                
//                if (root.mode == "max") {
//                    root.utility = (int)Collections.max(utils);
//                } else if (root.mode == "min") {
//                    root.utility = (int)Collections.min(utils);
//                }
//            }
//        }
//    }
    
    /**
     * Checks that all nodes are leafs
     */
//    public boolean allContainUtility(ArrayList<Node> nodes) {
//        boolean res = true;
//        for (Node node : nodes) {
//            if (node.utility == Integer.MIN_VALUE) {
//                res = false;
//            }
//        }
//        
//        return res;
//    }
    
    /**
     * Returns the best possible move.
     * 
     * @param board current board
     * @return int ambo id 1-6
     */
    public int getBestMove(GameState board)
    {
        ArrayList<Integer> utilities = new ArrayList<>();
        
        int count = 0;
        
        for(int i=1;i<=6;i++){
            // if this ambo is empty
            if(board.getSeeds(i, this.player) == 0){
                utilities.add(Integer.MIN_VALUE);
            } else {
                utilities.add(this.tree.root.children.get(count).utility );
                count++;
            }
        }
        
        int max = Collections.max(utilities);
        
        return utilities.indexOf(max) + 1;
    }
    
    
    /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     * 
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard)
    {
        this.tree= new Tree();
        Node root = new Node();
        root.mode = "max";
//        root.alpha = Integer.MIN_VALUE;
//        root.beta = Integer.MAX_VALUE;
        tree.root = root;
        
        GameState newBoard = currentBoard.clone();
//        constructTreeEfficient(tree.root, newBoard, 0);
//        calculateUtility(tree.root);
        idfs(tree.root, newBoard);
        int theMove = this.getBestMove(currentBoard);
//        System.out.println("Making move " + theMove);
        
        return theMove;
    }
}
