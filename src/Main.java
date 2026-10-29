
import java.util.*;
import java.io.IOException;

import ast.AstChar;
import ast.AstConcat;
import ast.AstDot;
import ast.AstOr;
import ast.AstRepeat;
import ast.interfaces.AstI;

import automate.Afn;
import automate.Afd;
import automate.AstToAfnVisitor;
import automate.Determinisation;
import automate.Minimisation;
import automate.AutomateI;
import automate.Etat;

import parser.Parser;

import experimentation.AfdMatcher;
import experimentation.FileSearcher;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println("  REGEX -> AST -> AFN -> AFD -> MINIMAL");
        System.out.println("==========================================");
        System.out.println();

        System.out.print("Entrez une expression régulière : ");
        String regex = scanner.nextLine();

        try {

            // ======================================
            // 1. REGEX -> AST
            // ======================================

            Parser parser = new Parser(regex);
            AstI ast = parser.parse();

            System.out.println();
            System.out.println("Expression analysée avec succès !");
            System.out.println();

            // ======================================
            // 2. AFFICHAGE DE L'AST
            // ======================================

            System.out.println("========== AST ==========");
            System.out.println();

            afficherAst(ast, "", true);
            System.out.println();

            // ======================================
            // 3. AST -> AFN-EPSILON
            // ======================================

            AstToAfnVisitor visitor = new AstToAfnVisitor();
            Afn afn = ast.accept(visitor);

            afficherAutomate(afn, "AFN-EPSILON");

            // ======================================
            // 4. AFN-EPSILON -> AFD
            // ======================================

            Determinisation determinisation = new Determinisation(afn);
            Afd afd = determinisation.convertir();

            afficherAutomate(afd, "AFD");

            // ======================================
            // 5. AFD -> AFD MINIMAL
            // ======================================

            Minimisation minimisation = new Minimisation(afd);
            Afd afdMinimal = minimisation.minimiser();

            afficherAutomate(afdMinimal, "AFD MINIMAL");

            // ======================================
            // 6. BILAN DES AUTOMATES
            // ======================================

            System.out.println("========== BILAN ==========");
            System.out.println();

            System.out.println("Expression : " + regex);

            System.out.println(
                "AFN-Epsilon : "
                + afn.getEtats().size()
                + " etats, "
                + compterTransitions(afn)
                + " transitions"
            );

            System.out.println(
                "AFD         : "
                + afd.getEtats().size()
                + " etats, "
                + compterTransitions(afd)
                + " transitions"
            );

            System.out.println(
                "AFD minimal : "
                + afdMinimal.getEtats().size()
                + " etats, "
                + compterTransitions(afdMinimal)
                + " transitions"
            );

            System.out.println();

            int avant = afd.getEtats().size();
            int apres = afdMinimal.getEtats().size();

            System.out.println(
                "Difference d'etats (AFD - AFD minimal) : "
                + (avant - apres)
            );

            if (avant > 0) {

                double reduction =
                    (avant - apres) * 100.0 / avant;

                System.out.printf(
                    "Variation du nombre d'etats : %.2f %%\n",
                    reduction
                );
            }

            // ======================================
            // 7. RECHERCHE DANS LE FICHIER
            // ======================================

            System.out.println();
            System.out.println("========== RECHERCHE ==========");
            System.out.println();

            System.out.print("Chemin du fichier : ");
            String chemin = scanner.nextLine();

            System.out.print("Afficher les lignes trouvees ? (o/n) : ");
            String reponse = scanner.nextLine();

            boolean afficher = reponse.equalsIgnoreCase("o");

            AfdMatcher matcher = new AfdMatcher(afdMinimal);
            FileSearcher recherche = new FileSearcher(matcher);

            System.out.println();
            System.out.println("Recherche en cours...");
            System.out.println();

            recherche.rechercher(chemin, afficher);

            System.out.println();
            System.out.println("Traitement termine avec succes !");

        }
        catch (IOException e) {

            System.err.println();
            System.err.println(
                "Erreur de lecture du fichier : " + e.getMessage()
            );

        }
        catch (RuntimeException e) {

            System.err.println();
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }

        scanner.close();
    }

    // ==================================================
    // AFFICHAGE DE L'AST
    // ==================================================

    private static void afficherAst(
            AstI ast,
            String prefix,
            boolean dernier) {

        if (ast == null) {
            return;
        }

        System.out.print(prefix);
        System.out.print(dernier ? "└── " : "├── ");

        if (ast instanceof AstChar) {

            AstChar caractere = (AstChar) ast;

            System.out.println(
                "CHAR(\"" + caractere.getValeur() + "\")"
            );

        }
        else if (ast instanceof AstDot) {

            System.out.println("DOT(.)");

        }
        else if (ast instanceof AstConcat) {

            System.out.println("CONCAT");

        }
        else if (ast instanceof AstOr) {

            System.out.println("OR");

        }
        else if (ast instanceof AstRepeat) {

            System.out.println("STAR(*)");

        }
        else {

            System.out.println(
                ast.getClass().getSimpleName()
            );
        }

        String nouveauPrefix =
            prefix + (dernier ? "    " : "│   ");

        AstI gauche = ast.getFilsGauche();
        AstI droite = ast.getFilsDroit();

        if (gauche != null && droite != null) {

            afficherAst(gauche, nouveauPrefix, false);
            afficherAst(droite, nouveauPrefix, true);

        }
        else if (gauche != null) {

            afficherAst(gauche, nouveauPrefix, true);

        }
        else if (droite != null) {

            afficherAst(droite, nouveauPrefix, true);
        }
    }

    // ==================================================
    // COMPTER LES TRANSITIONS
    // ==================================================

    private static int compterTransitions(Afn afn) {

        int total = 0;

        for (Map<String, Set<Etat>> sorties :
                afn.getTransitions().values()) {

            for (Set<Etat> destinations : sorties.values()) {
                total += destinations.size();
            }
        }

        return total;
    }

    private static int compterTransitions(Afd afd) {

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
            int nombreTransitions) {

        System.out.println("========== " + titre + " ==========");
        System.out.println();

        System.out.println(
            "Nombre d'etats : " + automate.getEtats().size()
        );

        System.out.println(
            "Nombre de transitions : " + nombreTransitions
        );

        int indexInitial =
            automate.getEtats().indexOf(automate.getInitial());

        System.out.println("Etat initial : q" + indexInitial);

        System.out.print("Etats finaux : ");

        if (automate.getFinaux().isEmpty()) {

            System.out.print("aucun");

        } else {

            for (int i = 0; i < automate.getEtats().size(); i++) {

                Etat etat = automate.getEtats().get(i);

                if (automate.getFinaux().contains(etat)) {
                    System.out.print("q" + i + " ");
                }
            }
        }

        System.out.println();
        System.out.println();

        System.out.println("Etats :");

        for (int i = 0; i < automate.getEtats().size(); i++) {

            Etat etat = automate.getEtats().get(i);

            System.out.print("  q" + i);

            if (etat == automate.getInitial()) {
                System.out.print(" [INITIAL]");
            }

            if (automate.getFinaux().contains(etat)) {
                System.out.print(" [FINAL]");
            }

            System.out.println();
        }

        System.out.println();
        System.out.println("Transitions :");
    }

    // ==================================================
    // AFFICHAGE DE L'AFN
    // ==================================================

    private static void afficherAutomate(Afn afn, String titre) {

        afficherEtats(afn, titre, compterTransitions(afn));

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

                    System.out.println(
                        "  q" + i
                        + " --" + symbole + "--> "
                        + "q" + indexDestination
                    );
                }
            }
        }

        System.out.println();
        System.out.println("===============================");
        System.out.println();
    }

    // ==================================================
    // AFFICHAGE DE L'AFD
    // ==================================================

    private static void afficherAutomate(Afd afd, String titre) {

        afficherEtats(afd, titre, compterTransitions(afd));

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

                System.out.println(
                    "  q" + i
                    + " --" + symbole + "--> "
                    + "q" + indexDestination
                );
            }
        }

        System.out.println();
        System.out.println("===============================");
        System.out.println();
    }
}
