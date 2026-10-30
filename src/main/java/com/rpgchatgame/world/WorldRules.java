package com.rpgchatgame.world;

/** Project 2.0 coordinate/render contract. No JavaFX or database dependencies. */
public final class WorldRules {
    public static final int MAP_FORMAT=1;
    public static final int TILE_PIXELS=64;
    public static final int STEP_MILLIS=250;
    private WorldRules(){}
    public enum Direction {
        UP(0,-1), DOWN(0,1), LEFT(-1,0), RIGHT(1,0);
        public final int dx,dy;
        Direction(int dx,int dy){this.dx=dx;this.dy=dy;}
    }
    public record Tile(int x,int y){
        public Tile facing(Direction direction){return new Tile(x+direction.dx,y+direction.dy);}
    }
}
