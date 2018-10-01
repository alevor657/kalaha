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
     * ************************************************************************
     */
    
    /**
     * This is a runner for recursive constructTreeEfficient() function.
     * It sets up a timer and manages objects to be passed to the 
     * constructTreeEfficient() function. It uses iterative deepening to make 
     * algorithm more effective.
     * 
     * @param root root node
     * @param board current GameState
     */
    public void ids(Node root, GameState board){
        // Set up depth treshold
        int count = 1;
        // Start up a timer
        Instant starts = Instant.now();
        long timer = 0;
        // Make a copy of passed node
        Node newNode = root.clone();
        
        while (timer < 5) {
            // Time passed
            Instant ends = Instant.now();
            // Update the timer
            timer = Duration.between(starts, ends).getSeconds();
            // Copy new tree to current working tree
            this.tree.root = newNode;
            // Get a new tree with set depth treshold
            newNode = constructTreeEfficient(newNode, board, 0, count, starts);           
            // Increment depth level
            count++;
        }
    }
    
    /**
     * Recursive function that builds the tree using depth first approach and 
     * propagates utilities up the tree. 
     * It also uses alpha-beta pruning.
     * 
     * @param root root node
     * @param board current game board
     * @param depth current depth of the tree
     * @param threshold maximal tree depth 
     * @param starts timer
     * @return tree
     */
    public Node constructTreeEfficient(Node root, GameState board, int depth, int threshold, Instant starts) {
        // Check if the turn is ours
        boolean isOurTurn = this.player == board.getNextPlayer();
        
        // Try build 6 children for current root node
        for (int k=1; k <= 6; k++) {
            // Prune if possible
            if (root.alpha < root.beta) {
                // Clone current GameState
                GameState newBoard = board.clone();
                
                // If move is not impossible, skip this branch
                if (!newBoard.moveIsPossible(k)) {
                    continue;
                };
                
                // make a move
                newBoard.makeMove(k);

                // create node and assign it as a child to current root
                Node newNode = new Node();
                newNode.mode = isOurTurn ? "max" : "min";
                newNode.alpha = root.alpha;
                newNode.beta = root.beta;
                int opponent = this.player == 1 ? 2 : 1;
                newNode.utility = newBoard.getScore(this.player) - newBoard.getScore(opponent);
                
                // Add created child to current root node
                root.children.add(newNode);
                
                // See if the timer har run out
                Instant ends = Instant.now();
                long timer = Duration.between(starts, ends).getSeconds();
                
                // If timer has not run out and treshhold is not reached
                if (depth < threshold && (timer < 5)) {
                    
                    // recurse with the new node
                    constructTreeEfficient(newNode, newBoard.clone(), depth+1, threshold, starts);
                    
                    // add AB values to new node
                    if ("max".equals(newNode.mode)) {
                        if(newNode.alpha < newNode.utility) {
                            newNode.alpha = newNode.utility;
                        }
                    } else if ("min".equals(newNode.mode)) {
                        if(newNode.beta > newNode.utility) {
                            newNode.beta = newNode.utility;
                        }
                    }
                    
                    // propagate ab to root node
                    if ("max".equals(root.mode)) {
                        if(root.alpha < newNode.utility) {
                            root.alpha = newNode.utility;
                        }
                    } else if ("min".equals(root.mode)) {
                        if(root.beta > newNode.utility) {
                            root.beta = newNode.utility;
                        }
                    }
                } else {
                    System.out.println(depth);
                }
            } else {
                // Prune branch
                System.out.println("Pruned");
                break;
            }
        }
        
        //if we are at leaf
        if (root.children.isEmpty()) {
            System.out.println("I am a leaf");
        } else {
            // propagate util to the parent
            ArrayList utils = new ArrayList();
            for (Node node : root.children) {
                utils.add(node.utility);
            }

            if ("max".equals(root.mode)) {
                int max = (int)Collections.max(utils);
                root.utility = max;
            } else if ("min".equals(root.mode)) {
                int min = (int)Collections.min(utils);
                root.utility = min;
            }
        }
        
        return root;
    }
    

    
    /**
     * Returns the first index of an ambo with highest utility. 
     * 
     * @param board Current board
     * @return index of selected ambo
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
                utilities.add(this.tree.root.children.get(count).utility);
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
        this.tree.root = root;
        
        GameState newBoard = currentBoard.clone();
        ids(this.tree.root, newBoard);
        int theMove = this.getBestMove(currentBoard);
        
        return theMove;
    }
}
