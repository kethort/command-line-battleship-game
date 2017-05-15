import java.util.*;

class Battleship {
    private static Ship cpuSubmarine;
    private static Ship cpuPatrol;
    private static Ship cpuDestroyer;
    private static Ship cpuBattleship;
    private static Ship cpuCarrier;
    private static Ship userSubmarine;
    private static Ship userPatrol;
    private static Ship userDestroyer;
    private static Ship userBattleship;
    private static Ship userCarrier;
    private static Stack<Point> pointStack = new Stack<Point>();
    private static ArrayList<Point> pointsNearby;
        
    public static void main(String[] args){
        System.out.println("Welcome to Battleship!");
        Scanner input = new Scanner(System.in);
        int answer = 0;
        while(answer != 3){
            System.out.print("Select an option:\n" +
                               "\t1. Explain Rules\n" +
                               "\t2. Start Game\n" + 
                               "\t3. Quit\n" +
                               "Enter (1/2/3): ");
            answer = input.nextInt();
            switch(answer){
                case 1:
                    displayRules();
                    break;
                case 2:
                    startGame();
                    break;
                case 3: 
                    break;
                default:
                    System.out.println("Not a valid choice, please try again.");
                    break;
            }
        }
    }

    protected static void displayRules(){
        System.out.println("\nEach player has a 10x10 gameboard and 5 Battleship pieces\n" +
                           "The 5 Battleship pieces can be placed on the grid anywhere they fit\n" +
                           "Each Battleship has a given length as follows:\n" + 
                           "\n\tCarrier: 5\n" +
                           "\tBattleship: 4\n" + 
                           "\tSubmarine: 3\n" + 
                           "\tDestroyer: 3\n" + 
                           "\tPatrol Boat: 2\n\n" + 
                           "The game pieces can be placed horizontally or vertically on the board\n" + 
                           "Each player takes turns firing a round at the coordinates of another user's\n" + 
                           "board, not knowing where the opponents Battleship pieces are placed.\n" + 
                           "The goal of the game is to destroy all of the enemie's ships before they destroy yours\n");
    }

    protected static void startGame(){
        char[] shipTypes = {'P', 'D', 'S', 'B', 'C'};
        Board computerBoard = new Board();
        createComputerShips(computerBoard, shipTypes);

        Board opponentHub = new Board();
        Board userBoard = new Board();
        createUserShips(userBoard, shipTypes);

        while(gameStatus(userBoard, computerBoard)){
            boolean status = true;
            displayGameBoards(userBoard, opponentHub);
            do{ //user fires
                status = outgoingFire(computerBoard, opponentHub);
            }while(status);

            do{ //cpu fires
                status = incomingFire(userBoard);
            }while(status);
        }
        if(userBoard.remainingGamePieces() > 0){
            System.out.println("Congratulations! You destroyed all the computer's ships!");
        }else{
            System.out.println("Too Bad...the computer destroyed you!");
        }
    }
    
    protected static int currentDirection(Point startPoint, Point endPoint){
        int direction = 0;
        if(startPoint.xValue() == endPoint.xValue() && startPoint.yValue() > endPoint.yValue()){
            direction = 0; // North
        }else if(startPoint.xValue() < endPoint.xValue() && startPoint.yValue() == endPoint.yValue()){
            direction = 1; // East
        }else if(startPoint.xValue() == endPoint.xValue() && startPoint.yValue() < endPoint.yValue()){
            direction = 2; // South
        }else if(startPoint.xValue() > endPoint.xValue() && startPoint.yValue() == endPoint.yValue()){
            direction = 3; // West
        }
        return direction;
    }

    protected static int reverseDirection(int direction){
        int reverse = 0;
        if(direction == 0){
            reverse = 2;
        }else if(direction == 1){
            reverse = 3;
        }else if(direction == 2){
            reverse = 0;
        }else if(direction == 3){
            reverse = 1;
        }
        return reverse;
    }

    protected static boolean incomingFire(Board board){
        int x = 0; 
        int y = 0;
        Random rand = new Random();
        //either the first time firing or just sunk a ship
        if(pointStack.empty()){
            x = rand.nextInt(10);
            y = rand.nextInt(10);
        }else if(pointStack.size() == 1){
            Point point = pointStack.peek();
            pointsNearby = point.nearbyNeighbors();
            int random = rand.nextInt(pointsNearby.size()); // might only have 2 nearest neighbors
            x = pointsNearby.get(random).xValue();
            y = pointsNearby.get(random).yValue();
        }else if(pointStack.size() > 1){
            Point endPoint = pointStack.pop();
            // if missed target but hit more than once and ship not sunk yet, reverse direction
            if(board.detectShot(endPoint.xValue(), endPoint.yValue()) == 'M'){
                Point startPoint = pointStack.peek();
                while(!pointStack.empty()){ // exhaust the stack to get first element
                    startPoint = pointStack.pop();
                }
                int direction = reverseDirection(currentDirection(startPoint, endPoint));
                pointStack.push(startPoint); // keep one item on stack because next hit will be pushed
                pointsNearby = startPoint.nearbyNeighbors();
                x = pointsNearby.get(direction).xValue();
                y = pointsNearby.get(direction).yValue();
            }else if(board.detectShot(endPoint.xValue(), endPoint.yValue()) == 'X'){
                // if hit target but ship not sunk yet, maintain current direction
                Point startPoint = pointStack.peek();
                pointsNearby = endPoint.nearbyNeighbors();
                int direction = currentDirection(startPoint, endPoint);
                pointStack.push(endPoint);
                if(direction > pointsNearby.size()){ // if reached end of grid
                    direction = reverseDirection(direction);
                    while(!pointStack.empty()){
                        startPoint = pointStack.pop();
                    }
                    pointStack.push(startPoint); // keep at least one item on stack
                    pointsNearby = startPoint.nearbyNeighbors();
                }
                x = pointsNearby.get(direction).xValue();
                y = pointsNearby.get(direction).yValue();
            }
        }
        switch(board.detectShot(x, y)){
            case 'M':
                return true;
            case 'X': 
                return true;
            case '_':
                System.out.println("\nThe opponent fired but missed!");
                board.addMiss(x, y);
                if(pointStack.size() > 1){
                    pointStack.push(new Point(x, y));
                }
                break;
            case 'P':
                board.addHit(x, y);
                pointStack.push(new Point(x, y));
                System.out.println("\nThe opponent has hit your Patrol Boat");
                userPatrol.updateHits();
                if(userPatrol.isSunk()){
                    System.out.println("\nThe opponent sunk your Patrol Boat!");
                    board.updateGamePieces();
                    pointStack = new Stack<Point>();
                }
                break;
            case 'D':
                board.addHit(x, y);
                pointStack.push(new Point(x, y));
                System.out.println("\nThe opponent has hit your Destroyer");
                userDestroyer.updateHits();
                if(userDestroyer.isSunk()){
                    System.out.println("\nThe opponent sunk your Destroyer!");
                    board.updateGamePieces();
                    pointStack = new Stack<Point>();
                }
                break;
            case 'S':
                board.addHit(x, y);
                pointStack.push(new Point(x, y));
                System.out.println("\nThe opponent has hit your Submarine");
                userSubmarine.updateHits();
                if(userSubmarine.isSunk()){
                    System.out.println("\nThe opponent sunk your Submarine!");
                    board.updateGamePieces();
                    pointStack = new Stack<Point>();
                }
                break;
            case 'B':
                board.addHit(x, y);
                pointStack.push(new Point(x, y));
                System.out.println("\nThe opponent has hit your Battleship");
                userBattleship.updateHits();
                if(userBattleship.isSunk()){
                    System.out.println("\nThe opponent sunk your Battleship!");
                    board.updateGamePieces();
                    pointStack = new Stack<Point>();
                }
                break;
            case 'C':
                board.addHit(x, y);
                pointStack.push(new Point(x, y));
                System.out.println("\nThe opponent has hit your Carrier");
                userCarrier.updateHits();
                if(userCarrier.isSunk()){
                    System.out.println("\nThe opponent sunk your Carrier!");
                    board.updateGamePieces();
                    pointStack = new Stack<Point>();
                }
                break;
        }
        return false;
    }

    protected static boolean outgoingFire(Board computerBoard, Board opponentHub){
        Scanner input = new Scanner(System.in);
        System.out.print("\nEnter x coordinate to fire shot: ");
        int x = input.nextInt();
        System.out.print("Enter y coordinate to fire shot: ");
        int y = input.nextInt();

        switch(computerBoard.detectShot(x, y)){
            case 'M':
                System.out.println("\nYou've already fired in that area! Try again.");
                return true;
            case 'X':
                System.out.println("\nYou've already fired in that area! Try again.");
                return true;
            case '_':
                System.out.println("\nYou missed the target!");
                opponentHub.addMiss(x, y);
                computerBoard.addMiss(x, y);
                break;
            case 'P':
                System.out.println("\nYou hit the opponents Patrol Boat!");
                opponentHub.addHit(x, y);
                computerBoard.addHit(x, y);
                cpuPatrol.updateHits();
                if(cpuPatrol.isSunk()){
                    System.out.println("\nYou sunk the opponents Patrol Boat!");
                    computerBoard.updateGamePieces();
                }
                break;
            case 'D':
                System.out.println("\nYou hit the opponents Destroyer!");
                opponentHub.addHit(x, y);
                computerBoard.addHit(x, y);
                cpuDestroyer.updateHits();
                if(cpuDestroyer.isSunk()){
                    System.out.println("\nYou sunk the opponents Destroyer!");
                    computerBoard.updateGamePieces();
                }
                break;
            case 'S':
                System.out.println("\nYou hit the opponents Submarine!");
                opponentHub.addHit(x, y);
                computerBoard.addHit(x, y);
                cpuSubmarine.updateHits();
                if(cpuSubmarine.isSunk()){
                    System.out.println("\nYou sunk the opponents Submarine!");
                    computerBoard.updateGamePieces();
                }
                break;
            case 'B':
                System.out.println("\nYou hit the opponents Battleship!");
                opponentHub.addHit(x, y);
                computerBoard.addHit(x, y);
                cpuBattleship.updateHits();
                if(cpuBattleship.isSunk()){
                    System.out.println("\nYou sunk the opponents Battleship!");
                    computerBoard.updateGamePieces();
                }
                break;
            case 'C':
                System.out.println("\nYou hit the opponents Carrier!");
                opponentHub.addHit(x, y);
                computerBoard.addHit(x, y);
                cpuCarrier.updateHits();
                if(cpuCarrier.isSunk()){
                    System.out.println("\nYou sunk the opponents Carrier!");
                    computerBoard.updateGamePieces();
                }
                break;
        }
        return false;
    }

    protected static boolean gameStatus(Board userBoard, Board computerBoard){
        return(userBoard.remainingGamePieces() != 0 && computerBoard.remainingGamePieces() != 0);
    }

    protected static void displayGameBoards(Board userBoard, Board opponentBoard){
        System.out.println("\nOpponent's Board");
        opponentBoard.displayBoard();
        System.out.println("\nYour Board");
        userBoard.displayBoard();
    }

    protected static void createUserShips(Board userBoard, char[] shipTypes){
        for(char type : shipTypes){
            userBoard.displayBoard();
            switch(type){
                case 'P':
                    userPatrol = addShipToUserBoard(type, userBoard);
                    break;
                case 'D':
                    userDestroyer = addShipToUserBoard(type, userBoard);
                    break;
                case 'S':
                    userSubmarine = addShipToUserBoard(type, userBoard);
                    break;
                case 'B':
                    userBattleship = addShipToUserBoard(type, userBoard);
                    break;
                case 'C':
                    userCarrier = addShipToUserBoard(type, userBoard);
                    break;
            }
        }
    }

    protected static Ship addShipToUserBoard(char type, Board userBoard){
        Ship ship = new Ship(0, 0, type, 'V');
        Scanner input = new Scanner(System.in);
        int xCoord = 0;
        int yCoord = 0;
        char orientation = 'a';
        while(true){
            System.out.print("\nEnter the x coordinate for the " + ship.shipName() + ": ");
            xCoord = input.nextInt();
            System.out.print("Enter the y coordinate for the " + ship.shipName() + ": ");
            yCoord = input.nextInt();
            System.out.print("Enter the orientation for the " + ship.shipName() +"(Vertical[V]/Horizontal[H]): ");
            orientation = input.next("[a-zA-Z]").toUpperCase().charAt(0);
            ship = new Ship(xCoord, yCoord, type, orientation);
            if(userBoard.doesPieceFit(ship)){
                userBoard.addShip(ship); 
                break;
            }
        }
        return ship;
    }

    protected static void createComputerShips(Board computerBoard, char[] shipTypes){
        for(char type : shipTypes){
            switch(type){
                case 'P':
                    cpuPatrol = addShipToComputerBoard(type, computerBoard);
                    break;
                case 'D':
                    cpuDestroyer = addShipToComputerBoard(type, computerBoard);
                    break;
                case 'S':
                    cpuSubmarine = addShipToComputerBoard(type, computerBoard);
                    break;
                case 'B':
                    cpuBattleship = addShipToComputerBoard(type, computerBoard);
                    break;
                case 'C':
                    cpuCarrier = addShipToComputerBoard(type, computerBoard);
                    break;
            }
        }
    }

    protected static Ship addShipToComputerBoard(char type, Board computerBoard){
        Ship ship;
        while(true){
            Random rand = new Random();
            int randX = rand.nextInt(10);
            int randY = rand.nextInt(10);
            char orientation = rand.nextBoolean() ? 'V' : 'H';
            ship = new Ship(randX, randY, type, orientation);
            if(computerBoard.doesPieceFit(ship)){
                computerBoard.addShip(ship); 
                break;
            }
        }
        return ship;
    }
}