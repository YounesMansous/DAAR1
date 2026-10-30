package parser;

import ast.AstFactory;
import ast.interfaces.AstI;

/**
 * Analyseur syntaxique par descente récursive d'une ERE simplifiée
 * (parenthèses, alternative |, concaténation, étoile *, point .,
 * lettres). Produit l'AST de la regex.
 *
 * Grammaire :
 * <pre>
 *   expression ::= concat ('|' concat)*
 *   concat     ::= repeat+ | (vide)
 *   repeat     ::= atome '*'*
 *   atome      ::= '(' expression ')' | '.' | lettre
 * </pre>
 */
public class Parser {

    private String re;
    private int position = 0;

    /**
     * @param re la regex à analyser
     */
    public Parser(String re){
        this.re = re;
    }

    /**
     * Analyse toute la regex.
     *
     * @return la racine de l'AST
     * @throws RuntimeException si la regex est invalide
     */
    public AstI parse() {
        AstI ast = parseExpression();

        // À la fin du parsing, on doit avoir consommé toute la regex
        if (position != re.length()) {
            throw new RuntimeException(
                "Expression invalide à la position " + position
            );
        }

        return ast;
    }

    /**
     * Analyse une alternative : concat ('|' concat)*.
     *
     * @return l'AST de l'alternative
     */
    public AstI parseExpression() {
        AstI gauche = parseConcat();

        while (position < re.length()
                && courant() != ')'
                && courant() == '|') {

            avancer(); // consomme |

            AstI droite = parseConcat();

            gauche = AstFactory.createOr(gauche, droite);
        }

        return gauche;
    }

    /**
     * Analyse une concaténation. Si elle est vide (fin de la regex,
     * « | » ou « ) »), renvoie un nœud mot vide, comme grep -E pour
     * « », « a| », « |a », « a||b » et « () ».
     *
     * @return l'AST de la concaténation
     */
    public AstI parseConcat() {
        if (finDeConcat()) {
            return AstFactory.createVide();
        }

        AstI gauche = parseRepeat();

        while (!finDeConcat()) {

            AstI droite = parseRepeat();

            gauche = AstFactory.createConcat(gauche, droite);
        }

        return gauche;
    }

    /**
     * Analyse un atome suivi d'étoiles éventuelles.
     *
     * @return l'AST de la répétition
     */
    public AstI parseRepeat() {
        AstI ast = parseAtom();

        while (position < re.length() && courant() == '*') {
            avancer(); // consomme *
            ast = AstFactory.createRepeat(ast);
        }

        return ast;
    }

    /**
     * Analyse un atome : sous-expression parenthésée, point ou lettre.
     *
     * @return l'AST de l'atome
     */
    public AstI parseAtom(){
        if (position >= re.length()) {
            throw new RuntimeException("Expression invalide : fin inattendue");
        }
        char c = courant();

        // Parenthèse ouvrante : on parse une sous-expression complète
        if (c == '(') {
            avancer();

            AstI interieur = parseExpression();

            if (position >= re.length() || courant() != ')') {
                throw new RuntimeException("Parenthèse fermante manquante");
            }

            avancer(); // consomme ')'
            return interieur;
        }

        // Le point représente le caractère universel
        if (c == '.') {
            avancer();
            return AstFactory.createDot();
        }

        // ) n'est pas un atome : il termine une sous-expression
        if (c == ')') {
            throw new RuntimeException("Parenthèse fermante inattendue");
        }

        // *, | ne peuvent pas commencer un atome
        if (c == '*' || c == '|') {
            throw new RuntimeException("Opérateur inattendu : " + c);
        }

        // Sinon, on considère que c'est un caractère ASCII
        avancer();
        return AstFactory.createChar(c);
    }

    /** Passe au caractère suivant de la regex. */
    public void avancer(){
        position++;
    }

    /** @return le caractère courant de la regex */
    public char courant() {
        return re.charAt(position);
    }

    // Une concaténation s'arrête à la fin de la regex, sur | ou sur )
    private boolean finDeConcat() {
        return position >= re.length()
            || courant() == '|'
            || courant() == ')';
    }
}
