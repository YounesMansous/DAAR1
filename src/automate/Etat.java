package automate;

public class Etat{
    private boolean initialState;
    private boolean finalState;

    public Etat(){
        this.initialState = false;
        this.finalState = false;
    }

    public Etat(boolean initialState, boolean finalState){
        this.initialState = initialState;
        this.finalState = finalState;
    }

    public void setIsInitial(){

    }

    public void setIsFinalState(){

    }

    public boolean isInitialState() {
        return initialState;
    }

    public boolean isFinalState(){
        return finalState;
    }
}