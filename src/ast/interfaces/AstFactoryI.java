package ast.interfaces;

public interface AstFactoryI {
    public static AstI createChar(String valeur) {
        throw new UnsupportedOperationException();
    }
    public static AstI createDot(){
        throw new UnsupportedOperationException();
    }
    public static AstI createRepeat(AstI fils){
        throw new UnsupportedOperationException();
    }
    public static AstI createConcat(AstI gauche, AstI droite){
        throw new UnsupportedOperationException();
    }
    public static AstI createOr(AstI gauche, AstI droite){
        throw new UnsupportedOperationException();
    }
}
