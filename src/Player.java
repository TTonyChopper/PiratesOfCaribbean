import java.util.*;
import java.io.*;
import java.math.*;

class Player { 
	static final int nextOddX[] ={
		1,1,0,-1,0,1
	};
	static final int nextOddY[] ={
		0,-1,-1,0,1,1
	};
	static final int nextEvenX[] ={
		1,0,-1,-1,-1,0
	};
	static final int nextEvenY[] ={
		0,-1,-1,0,1,1
	};
	
    static final String SHIP = "SHIP";
    static final String BARREL = "BARREL";
    static final String CANNONBALL = "CANNONBALL";
    static final String MINE = "MINE";
    
    static final int EAST = 0;
    static final int NORTH_EAST = 1;
    static final int NORTH_WEST = 2;
    static final int WEST = 3;
    static final int SOUTH_WEST = 4;
    static final int SOUTH_EAST = 5;
    
    static final int MAP_WIDTH = 23;
    static final int MAP_LENGTH = 21;
    static final int SHIP_WIDTH = 1;
    static final int SHIP_LENGTH = 3;
    static final int SHIP_MAX_RHUM = 100;
    static final int MIN_RHUM = 10;
    static final int MAX_RHUM = 20;

    public static void main(String args[]) {
        Map oldMap = new Map();
        oldMap.initTree();

        Scanner in = new Scanner(System.in);
        
        // game loop
        while (true) {
                   
            Map newMap = new Map(oldMap);
            int myShipCount = initTurn(in, newMap);
            playTurnForAll(newMap);
            oldMap = newMap;
        }
    }
    
    static void initTree()
    {
    	
    }
    
    static int initTurn(Scanner in, Map newMap)
    {
        int myShipCount = in.nextInt(); // the number of remaining ships
        int entityCount = in.nextInt();// the number of entities (e.g. ships, mines or cannonballs)
        for (int i = 0; i < entityCount; i++) {
            int entityId = in.nextInt();
            String entityType = in.next();
            int x = in.nextInt();
            int y = in.nextInt();
            int arg1 = in.nextInt();
            int arg2 = in.nextInt();
            int arg3 = in.nextInt();
            int arg4 = in.nextInt();
            switch (entityType)
            {
                case Player.SHIP:
                    initEntityShip(newMap, entityId, x, y, arg1, arg2, arg3, arg4);
                    break;
                case Player.BARREL:
                    initEntityBarrel(newMap, entityId, x, y, arg1);
                    break;
                case Player.CANNONBALL:
                case Player.MINE:
                    break;
                default:
                throw new IllegalArgumentException("Unknown entity type!");
            }
        }
        return myShipCount;
    }
    static void initEntityShip(Map newMap, int id, int x, int y, int rotation, int speed, int rhum, int owner)
    {
    	switch (owner) {
		case 0:
			newMap.addShip(new Ship(id, x, y, rotation, speed, rhum, false));
			break;
		case 1:
			newMap.addShip(new Ship(id, x, y, rotation, speed, rhum, true));
			break;
		}
    }
    static void initEntityBarrel(Map newMap, int id, int x, int y, int qty)
    {
    	newMap.barrels.add(new Barrel(id, x, y, qty));
    }
    
    static void playTurnForAll(Map newMap)
    {
    	System.err.println(newMap.me.ships);
        if (newMap.me.ships.isEmpty())
        {
            new Action(BASE.WAIT).send();
        }
            
    	for(Ship ship : newMap.me.ships.values())
    	{
    		ship.print();
    		playTurnForOne(newMap, ship).send();
    	}
    }
    
    static Action playTurnForOne(Map newMap, Ship ship)
    {
    	TargetCoord targetRum = newMap.findClosestBarrelOrNull(ship);
    	
    	TargetCoord targetShip = newMap.findClosestShipOrNull(ship);
    	
    	if (targetShip!= null)
    	{
    		if(targetShip.dist <= 3)
    		{
    			return new Action(BASE.FIRE, newMap.predictPosition(targetShip));
    		}
    		else if (targetShip.dist <= 10 && newMap.shipsById.get(targetShip.id).speed == 0)
    		{
    			return new Action(BASE.FIRE, targetShip);
    		}
    	}
    	if (targetRum == null)
    	{
        	if (targetShip == null)
        	{
        		return new Action(BASE.SLOWER);
        	}
        	return new Action(BASE.MOVE, targetShip);
    	}
    	else
    	{
    		return new Action(BASE.MOVE, targetRum).print();
    	}
    }
}

class Map{
	boolean isMoving = false;
	ArrayList<Barrel> barrels = new ArrayList<>();
	final Position[][] grid = new Position[Player.MAP_WIDTH][Player.MAP_LENGTH];
	HashMap<String, Position> positions;
	Captain me = new Captain();
	Captain other = new Captain();
	HashMap<Integer, Ship> shipsById = new HashMap<>();;
    Map()
    {
    }
    Map(Map oldMap)
    {
    	this.isMoving = oldMap.isMoving;
    }
    void initTree()
    {
    	for (int j = 0 ; j < Player.MAP_LENGTH ; j++)
    	{
    		for (int i = 0 ; i < Player.MAP_WIDTH ; i++)
    		{
    			grid[i][j] = new Position(i, j);
    		}
    	}
    }
    void addShip(Ship ship)
    {
    	if (ship.isMyShip)
    	{
    		me.ships.put(ship.id, ship);
    		shipsById.put(ship.id, ship);
    	}
    	else
    	{
    		other.ships.put(ship.id, ship);
    		shipsById.put(ship.id, ship);
    	}
    	ship.printFull();
    }
    TargetCoord findClosestBarrelOrNull(Ship ship)
    {
    	int currentX = ship.coord.x;
    	int currentY = ship.coord.y;
    	int diff = Player.MAP_LENGTH;
    	Barrel result = null; 
    	for(Barrel barrel : barrels)
    	{
    		int diffX = Math.abs(currentX - barrel.coord.x);
    		int diffY = Math.abs(currentY - barrel.coord.y);
    		int candDiff = diffX + diffY;
    		if (diff > candDiff)
    		{
    			diff = candDiff;
    			result = barrel;
    		}
    	}
    	if(result != null)
    	{
    		result.coord.print();
    		return new TargetCoord(result.coord, result.id, diff);
    	}
    	else
    	{
    		return null;
    	}
    }
    TargetCoord findClosestShipOrNull(Ship ship)
    {
    	int currentX = ship.coord.x;
    	int currentY = ship.coord.y;
    	int currentID;
    	int diff = Player.MAP_LENGTH;
    	Ship result = null; 
    	for(Ship otherShip : other.ships.values())
    	{
    		int diffX = Math.abs(currentX - otherShip.coord.x);
    		int diffY = Math.abs(currentY - otherShip.coord.y);
    		int candDiff = diffX + diffY;
    		if (diff > candDiff)
    		{
    			diff = candDiff;
    			result = otherShip;
    		}
    	}
    	if(result != null)
    	{
    		result.print();
    		return new TargetCoord(result.coord, result.id, diff);
    	}
    	else
    	{
    		return null;
    	}
    }
    
    Coord predictPosition(TargetCoord ship)
    {
    	Ship target = shipsById.get(ship.id);
    	int x = ship.x;
    	int y = ship.y;
    	return new Coord(x,y,y%2 == 0,target.orientation, target.speed);	
    }
}

class Coord{
	int x;
	int y;
	int ord;
	Coord()
	{
		
	}
	Coord(int x, int y)
	{
    	this.x = x;
    	this.y = y;
    	this.ord = x + y*Player.MAP_LENGTH;
	}
	Coord(int x, int y, boolean isEven, int orientation, int speed)
	{
		if (speed == 0)
		{
			this.x = x;
			this.y = y;
			return;
		}
		
		if (isEven)
		{
			int predictionX = x + Player.nextEvenX[orientation];
			int predictionY = y + Player.nextEvenY[orientation];
			if (isXout(predictionX) || isXout(predictionY))
			{
				this.x = x;
				this.y = y;
			}
			else
			{
				this.x = predictionX;
				this.y = predictionY;
			}
		}
		else
		{
			int predictionX = x + Player.nextOddX[orientation];
			int predictionY = y + Player.nextOddY[orientation];
			if (isXout(predictionX) || isXout(predictionY))
			{
				this.x = x;
				this.y = y;
			}
			else
			{
				this.x = predictionX;
				this.y = predictionY;
			}
		}
    	this.ord = x + y*Player.MAP_LENGTH;
	}	
	boolean isXout(int x)
	{
		return x < 0 || x >= Player.MAP_WIDTH;
	}
	boolean isYout(int y)
	{
		return y < 0 || y >= Player.MAP_LENGTH;
	}
	void print()
	{
		System.err.println("x" + x + "y" + y + "=" + ord);
	}
}

class TargetCoord extends Coord{
	int id;
	int dist;
	TargetCoord()
	{
		dist = -1;
	}
	TargetCoord(int x, int y, int id, int dist)
	{
		super(x, y);
		this.id = id;
    	this.dist = dist;
	}
	TargetCoord(Coord coord, int id, int dist)
	{
		super(coord.x, coord.y);
		this.id = id;
    	this.dist = dist;
	}
}

class Position
{
	final Coord coord;
	ArrayList<HashSet<Position>> neighbours;
	Position(int x, int y)
	{
		this.coord = new Coord(x, y);
	}
}

class Captain {
    HashMap<Integer, Ship> ships = new HashMap<>();
}

class Entity{
	int id;
	Coord coord;
	Entity(int id, int x, int y)
    {
		this.coord = new Coord(x, y);
		this.id = id;
    }
}

class Ship extends Entity{
	Coord currentTarget = null;
    int orientation = -1;
    int speed = -1;
    int rhum = -1;
    Boolean isMyShip = null;
    
    Ship(int id, int x, int y, int orientation, int speed, int rhum, Boolean isMyShip)
    {
    	super(id, x, y);
        this.orientation = orientation;
        this.speed = speed;
        this.rhum = rhum;
        this.isMyShip = isMyShip;
    }
    public void print() {
		System.err.println((isMyShip ? "M " : "O ") + "id" + id + "x" + coord.x + "y" + coord.y);
	}
    public void printFull() {
    	print();
		System.err.println("ori" + orientation + "speed" + speed + "rhum" + rhum);
	}
}

class Barrel extends Entity{
	int qty;
	Barrel(int id, int x, int y, int qty)
	{
		super(id, x, y);
		this.qty = qty;
	}
}

class CannonBall extends Entity{
	CannonBall(int id, int x, int y)
	{
		super(id, x, y);
	}
}

class Mine extends Entity{
	Mine(int id, int x, int y)
	{
		super(id, x, y);
	}
}

enum BASE{
    MOVE, FIRE, MINE, SLOWER, WAIT
}

class Action
{
    BASE base;
    int x = -1;
    int y = -1;
    Action(BASE base){
    	this.base = base;
    }
    Action(BASE base, Coord coord){
    	this(base);
    	this.x = coord.x;
    	this.y = coord.y;
    }
    void send(){
        System.out.println(toString());
    }
    public String toString(){
        switch (base)
        {
            case MOVE:
                return "MOVE " + x + " " + y;
            case FIRE:
                return "FIRE " + x + " " + y;
            case MINE:
                return "MINE";
            case SLOWER:
                return "SLOWER";
            case WAIT:
                return "WAIT";
            default:
                return "";
        }
    }
    Action print(){
        System.err.println(toString());
        return this;
    }
}
    