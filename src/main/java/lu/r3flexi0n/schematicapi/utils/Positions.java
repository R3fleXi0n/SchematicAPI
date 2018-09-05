package lu.r3flexi0n.schematicapi.utils;

public class Positions {

    private Vector position1, position2;

    public Vector getPosition1() {
        return position1;
    }

    public Vector getPosition2() {
        return position2;
    }

    public void setPosition1(Vector position1) {
        this.position1 = position1;
    }

    public void setPosition2(Vector position2) {
        this.position2 = position2;
    }

    public boolean isIncomplete() {
        return position1 == null || position2 == null;
    }

}
