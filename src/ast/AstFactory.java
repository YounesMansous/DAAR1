package ast;

import ast.interfaces.AstFactoryI;
import ast.interfaces.AstI;

/**
 * Fabrique des nœuds de l'AST, utilisée par le Parser.
 */
public class AstFactory implements AstFactoryI{

    /** @return une feuille pour la lettre {@code valeur} */
    public static AstI createChar(Character valeur) {
        return new AstChar(valeur);
    }

    /** @return une feuille pour le point (caractère universel) */
    public static AstI createDot() {
        return new AstDot();
    }

    /** @return une feuille qui reconnaît le mot vide */
    public static AstI createVide() {
        return new AstVide();
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