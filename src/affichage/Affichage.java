package affichage;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import ast.AstChar;
import ast.AstConcat;
import ast.AstDot;
import ast.AstOr;
import ast.AstRepeat;
import ast.AstVide;
import ast.interfaces.AstI;

import automate.Afd;
import automate.Afn;
import automate.AutomateI;
import automate.Etat;

/**
 * Affichage de l'AST et des automates (mode --debug).
 * Toutes les méthodes écrivent sur le flux donné en paramètre
 * (stderr dans Main), jamais directement sur la sortie standard.
 */
public final class Affichage {

    private Affichage() {
    }

    // ==================================================
    // AFFICHAGE DE L'AST
    // ==================================================

    /**
     * Affiche l'AST sous forme d'arbre.
     *
     * @param ast racine de l'AST
     * @param out flux de sortie
     */
    public static void afficherAst(AstI ast, PrintStream out) {

        out.println("========== AST ==========");
        out.println();

        afficherNoeud(ast, "", true, out);
        out.println();
    }

    private static void afficherNoeud(
            AstI ast,
            String prefix,
            boolean dernier,
            PrintStream out) {

        if (ast == null) {
            return;
        }

        out.print(prefix);
        out.print(dernier ? "└── " : "├── ");

        if (ast instanceof AstChar) {

            AstChar caractere = (AstChar) ast;

            out.println(
                "CHAR(\"" + caractere.getValeur() + "\")"
            );

        }
        else if (ast instanceof AstDot) {

            out.println("DOT(.)");

        }
        else if (ast instanceof AstVide) {

            out.println("VIDE(ε)");

        }
        else if (ast instanceof AstConcat) {

            out.println("CONCAT");

        }
        else if (ast instanceof AstOr) {

            out.println("OR");

        }
        else if (ast instanceof AstRepeat) {

            out.println("STAR(*)");

        }
        else {

            out.println(
                ast.getClass().getSimpleName()
            );
        }

        String nouveauPrefix =
            prefix + (dernier ? "    " : "│   ");

        AstI gauche = ast.getFilsGauche();
        AstI droite = ast.getFilsDroit();

        if (gauche != null && droite != null) {

            afficherNoeud(gauche, nouveauPrefix, false, out);
            afficherNoeud(droite, nouveauPrefix, true, out);

        }
        else if (gauche != null) {

            afficherNoeud(gauche, nouveauPrefix, true, out);

        }
        else if (droite != null) {

            afficherNoeud(droite, nouveauPrefix, true, out);
        }
    }

    // ==================================================
    // COMPTER LES TRANSITIONS
    // ==================================================

    /**
     * @param afn un AFN
     * @return son nombre de transitions (epsilon comprises)
     */
    public static int compterTransitions(Afn afn) {

        int total = 0;

        for (Map<String, Set<Etat>> sorties :
                afn.getTransitions().values()) {

            for (Set<Etat> destinations : sorties.values()) {
                total += destinations.size();
            }
        }

        return total;
    }

    /**
     * @param afd un AFD
     * @return son nombre de transitions
     */
    public static int compterTransitions(Afd afd) {

        int total = 0;

        for (Map<String, Etat> sorties :
                afd.getTransitions().values()) {

            total += sorties.size();
        }

        return total;
    }

    // ==================================================
    // AFFICHAGE COMMUN DES ETATS
    // ==================================================

    private static void afficherEtats(
            AutomateI<?> automate,
            String titre,
            int nombreTransitions,
            PrintStream out) {

        out.println("========== " + titre + " ==========");
        out.println();

        out.println(
            "Nombre d'etats : " + automate.getEtats().size()
        );

        out.println(
            "Nombre de transitions : " + nombreTransitions
        );

        int indexInitial =
            automate.getEtats().indexOf(automate.getInitial());

        out.println("Etat initial : q" + indexInitial);

        out.print("Etats finaux : ");

        if (automate.getFinaux().isEmpty()) {

            out.print("aucun");

        } else {

            for (int i = 0; i < automate.getEtats().size(); i++) {

                Etat etat = automate.getEtats().get(i);

                if (automate.getFinaux().contains(etat)) {
                    out.print("q" + i + " ");
                }
            }
        }

        out.println();
        out.println();

        out.println("Etats :");

        for (int i = 0; i < automate.getEtats().size(); i++) {

            Etat etat = automate.getEtats().get(i);

            out.print("  q" + i);

            if (etat == automate.getInitial()) {
                out.print(" [INITIAL]");
            }

            if (automate.getFinaux().contains(etat)) {
                out.print(" [FINAL]");
            }

            out.println();
        }

        out.println();
        out.println("Transitions :");
    }

    // ==================================================
    // AFFICHAGE DE L'AFN
    // ==================================================

    /**
     * Affiche les états et les transitions d'un AFN.
     *
     * @param afn   l'automate
     * @param titre titre de la section
     * @param out   flux de sortie
     */
    public static void afficherAutomate(
            Afn afn,
            String titre,
            PrintStream out) {

        afficherEtats(afn, titre, compterTransitions(afn), out);

        for (int i = 0; i < afn.getEtats().size(); i++) {

            Etat source = afn.getEtats().get(i);

            Map<String, Set<Etat>> sorties =
                afn.getTransitions().getOrDefault(
                    source,
                    Collections.emptyMap()
                );

            for (Map.Entry<String, Set<Etat>> entree :
                    sorties.entrySet()) {

                String symbole = entree.getKey();

                for (Etat destination : entree.getValue()) {

                    int indexDestination =
                        afn.getEtats().indexOf(destination);

                    out.println(
                        "  q" + i
                        + " --" + symbole + "--> "
                        + "q" + indexDestination
                    );
                }
            }
        }

        out.println();
        out.println("===============================");
        out.println();
    }

    // ==================================================
    // AFFICHAGE DE L'AFD
    // ==================================================

    /**
     * Affiche l'alphabet, les états et les transitions d'un AFD.
     *
     * @param afd   l'automate
     * @param titre titre de la section
     * @param out   flux de sortie
     */
    public static void afficherAutomate(
            Afd afd,
            String titre,
            PrintStream out) {

        afficherEtats(afd, titre, compterTransitions(afd), out);

        for (int i = 0; i < afd.getEtats().size(); i++) {

            Etat source = afd.getEtats().get(i);

            Map<String, Etat> sorties =
                afd.getTransitions().getOrDefault(
                    source,
                    Collections.emptyMap()
                );

            for (Map.Entry<String, Etat> entree :
                    sorties.entrySet()) {

                String symbole = entree.getKey();
                Etat destination = entree.getValue();

                int indexDestination =
                    afd.getEtats().indexOf(destination);

                out.println(
                    "  q" + i
                    + " --" + symbole + "--> "
                    + "q" + indexDestination
                );
            }
        }

        out.println();
        out.println("Alphabet : " + afd.getAlphabet());
        out.println();
        out.println("===============================");
        out.println();
    }

    // ==================================================
    // BILAN DES AUTOMATES
    // ==================================================

    /**
     * Affiche la taille de chaque automate et la réduction
     * obtenue par la minimisation.
     *
     * @param regex      la regex
     * @param afn        l'AFN-epsilon
     * @param afd        l'AFD obtenu par déterminisation
     * @param afdMinimal l'AFD minimal
     * @param out        flux de sortie
     */
    public static void afficherBilan(
            String regex,
            Afn afn,
            Afd afd,
            Afd afdMinimal,
            PrintStream out) {

        out.println("========== BILAN ==========");
        out.println();

        out.println("Expression : " + regex);

        out.println(
            "AFN-Epsilon : "
            + afn.getEtats().size()
            + " etats, "
            + compterTransitions(afn)
            + " transitions"
        );

        out.println(
            "AFD         : "
            + afd.getEtats().size()
            + " etats, "
            + compterTransitions(afd)
            + " transitions"
        );

        out.println(
            "AFD minimal : "
            + afdMinimal.getEtats().size()
            + " etats, "
            + compterTransitions(afdMinimal)
            + " transitions"
        );

        out.println();

        int avant = afd.getEtats().size();
        int apres = afdMinimal.getEtats().size();

        out.println(
            "Difference d'etats (AFD - AFD minimal) : "
            + (avant - apres)
        );

        if (avant > 0) {

            double reduction =
                (avant - apres) * 100.0 / avant;

            out.printf(
                "Variation du nombre d'etats : %.2f %%%n",
                reduction
            );
        }

        out.println();
    }
}
