package ast;

import ast.interfaces.AstFactoryI;
import ast.interfaces.AstI;

public class AstFactory implements AstFactoryI{

    public static AstI createChar(Character valeur) {
        return new AstChar(valeur);
    }

    public static AstI createDot() {
        return new AstDot();
    }

    public static AstI createRepeat(AstI fils) {
        return new AstRepeat(fils);
    }

    public static AstI createConcat(AstI gauche, AstI droite) {
        return new AstConcat(gauche, droite);
    }

    public static AstI createOr(AstI gauche, AstI droite) {
        return new AstOr(gauche, droite);
    }
}