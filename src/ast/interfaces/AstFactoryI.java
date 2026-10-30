package ast.interfaces;

/**
 * Signatures de la fabrique de nœuds de l'AST.
 * Les implémentations ({@code ast.AstFactory}) reprennent
 * exactement ces signatures.
 */
public interface AstFactoryI {
    public static AstI createChar(Character valeur) {
        throw new UnsupportedOperationException();
    }
    public static AstI createDot(){
        throw new UnsupportedOperationException();
    }
    public static AstI createVide(){
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
