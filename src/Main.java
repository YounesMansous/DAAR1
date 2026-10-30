
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import affichage.Affichage;

import ast.interfaces.AstI;

import automate.ConstructionAutomate;

import parser.Parser;

import experimentation.FileSearcher;
import experimentation.Matcher;
import experimentation.MatcherNaif;
import experimentation.MatcherUnePasse;
import experimentation.ResultatRecherche;

import kmp.Kmp;

/**
 * Clone de egrep : affiche les lignes d'un fichier (ou de l'entrée
 * standard) qui contiennent le motif, exactement comme grep -E.
 *
 * Usage : java -cp bin Main [options] "regex" [fichier]
 *
 * La sortie standard ne contient que le résultat (lignes ou nombre) ;
 * --debug, --stats et les messages d'erreur vont sur la sortie d'erreur.
 * Codes de sortie : 0 si au moins une ligne est trouvée, 1 si aucune,
 * 2 en cas d'erreur.
 */
public class Main {

    private static final int TROUVE = 0;
    private static final int RIEN_TROUVE = 1;
    private static final int ERREUR = 2;

    private static final String USAGE =
        "Usage : java -cp bin Main [options] \"regex\" [fichier]\n"
        + "Sans fichier (ou avec -), lit l'entrée standard.\n"
        + "Options :\n"
        + "  -c        affiche seulement le nombre de lignes trouvées\n"
        + "  -n        préfixe chaque ligne par son numéro (12:ligne)\n"
        + "  --debug   affiche l'AST et les automates sur la sortie d'erreur\n"
        + "  --stats   affiche le matcher, la taille des automates et les"
        + " temps de construction et de recherche sur la sortie d'erreur\n"
        + "  --algo=auto       KMP si le motif est littéral, sinon"
        + " l'automate (par défaut)\n"
        + "  --algo=automate   toujours l'automate\n"
        + "  --algo=kmp        toujours KMP (motif littéral obligatoire)\n"
        + "  --matcher=unepasse  recherche en une passe avec l'automate de"
        + " .*R (par défaut)\n"
        + "  --matcher=naif      relance l'automate de R depuis chaque"
        + " position\n"
        + "  --        termine les options (pour un motif qui commence par -)";

    /**
     * Point d'entrée : force l'UTF-8 sur les sorties, lance la
     * recherche et quitte avec le code de sortie d'egrep.
     *
     * @param args options, regex et fichier éventuel
     */
    public static void main(String[] args) {

        // UTF-8 quelle que soit la locale du terminal ; sortie standard
        // tamponnée, vidée une seule fois à la fin
        PrintStream out = new PrintStream(
            new BufferedOutputStream(
                new FileOutputStream(FileDescriptor.out), 1 << 16),
            false,
            StandardCharsets.UTF_8);

        PrintStream err = new PrintStream(
            new FileOutputStream(FileDescriptor.err),
            true,
            StandardCharsets.UTF_8);

        System.setOut(out);
        System.setErr(err);

        int code = executer(args, out, err);

        out.flush();
        err.flush();

        System.exit(code);
    }

    /**
     * Exécute la commande sans quitter la JVM.
     *
     * @param args options, regex et fichier éventuel
     * @param out  sortie standard (résultat uniquement)
     * @param err  sortie d'erreur (debug, stats, erreurs)
     * @return le code de sortie : 0 trouvé, 1 rien trouvé, 2 erreur
     */
    public static int executer(
            String[] args,
            PrintStream out,
            PrintStream err) {

        // ======================================
        // 1. LECTURE DES ARGUMENTS
        // ======================================

        boolean compter = false;
        boolean numeroter = false;
        boolean debug = false;
        boolean stats = false;
        String typeMatcher = "unepasse";
        String algo = "auto";

        List<String> positionnels = new ArrayList<>();
        boolean finOptions = false;

        for (String arg : args) {

            if (finOptions || arg.equals("-") || !arg.startsWith("-")) {
                positionnels.add(arg);
            }
            else if (arg.equals("--")) {
                finOptions = true;
            }
            else if (arg.equals("--debug")) {
                debug = true;
            }
            else if (arg.equals("--stats")) {
                stats = true;
            }
            else if (arg.startsWith("--algo=")) {

                algo = arg.substring("--algo=".length());

                if (!algo.equals("auto")
                        && !algo.equals("automate")
                        && !algo.equals("kmp")) {
                    return erreur(err, "algorithme inconnu : " + algo
                        + " (auto, automate ou kmp)");
                }
            }
            else if (arg.startsWith("--matcher=")) {

                typeMatcher = arg.substring("--matcher=".length());

                if (!typeMatcher.equals("naif")
                        && !typeMatcher.equals("unepasse")) {
                    return erreur(err, "matcher inconnu : " + typeMatcher
                        + " (naif ou unepasse)");
                }
            }
            else if (arg.startsWith("--")) {
                return erreur(err, "option inconnue : " + arg);
            }
            else {

                // Options courtes, éventuellement groupées (-cn)
                for (char option : arg.substring(1).toCharArray()) {

                    if (option == 'c') {
                        compter = true;
                    }
                    else if (option == 'n') {
                        numeroter = true;
                    }
                    else {
                        return erreur(err, "option inconnue : -" + option);
                    }
                }
            }
        }

        if (positionnels.isEmpty()) {
            return erreur(err, "regex manquante");
        }

        if (positionnels.size() > 2) {
            return erreur(err, "trop d'arguments");
        }

        String regex = positionnels.get(0);
        String fichier =
            positionnels.size() == 2 ? positionnels.get(1) : "-";

        avertirSiMotifMalDecode(regex, err);

        // ======================================
        // 2. CONSTRUCTION DE L'AUTOMATE
        // ======================================

        long debutConstruction = System.nanoTime();

        AstI ast;

        try {
            ast = new Parser(regex).parse();
        }
        catch (RuntimeException e) {
            return erreur(
                err,
                "regex invalide « " + regex + " » : " + e.getMessage());
        }

        // KMP ne s'applique qu'a une concatenation de lettres
        String motifLitteral = Kmp.motifLitteral(ast);

        if (algo.equals("kmp") && motifLitteral == null) {
            return erreur(
                err,
                "--algo=kmp exige un motif littéral (uniquement des"
                + " caractères concaténés, sans |, *, . ni mot vide) : « "
                + regex + " »");
        }

        boolean utiliserKmp = algo.equals("kmp")
            || (algo.equals("auto") && motifLitteral != null);

        // Le matcher une passe travaille sur l'automate de .*R
        boolean unePasse = typeMatcher.equals("unepasse");

        AstI astUtilise = ast;
        ConstructionAutomate automate = null;
        Kmp kmp = null;
        Matcher matcher;

        if (utiliserKmp) {

            kmp = new Kmp(motifLitteral);
            matcher = kmp;
        }
        else {

            astUtilise = unePasse ? MatcherUnePasse.envelopper(ast) : ast;

            automate = new ConstructionAutomate(astUtilise);

            matcher = unePasse
                ? new MatcherUnePasse(automate.getCompile())
                : new MatcherNaif(automate.getCompile());
        }

        long finConstruction = System.nanoTime();

        if (debug) {

            Affichage.afficherAst(astUtilise, err);

            if (utiliserKmp) {
                Affichage.afficherKmp(kmp, err);
            }
            else {
                Affichage.afficherAutomate(
                    automate.getAfn(), "AFN-EPSILON", err);
                Affichage.afficherAutomate(automate.getAfd(), "AFD", err);
                Affichage.afficherAutomate(
                    automate.getAfdMinimal(), "AFD MINIMAL", err);
                Affichage.afficherBilan(
                    unePasse ? ".*(" + regex + ")" : regex,
                    automate.getAfn(),
                    automate.getAfd(),
                    automate.getAfdMinimal(),
                    err);
            }
        }

        // ======================================
        // 3. RECHERCHE
        // ======================================

        FileSearcher recherche = new FileSearcher(matcher);

        ResultatRecherche resultat;

        long debutRecherche = System.nanoTime();

        try (BufferedReader lecteur = ouvrir(fichier)) {
            resultat = recherche.rechercher(lecteur);
        }
        catch (NoSuchFileException e) {
            return erreur(err, fichier + " : fichier introuvable");
        }
        catch (MalformedInputException e) {
            return erreur(err, fichier + " : texte non UTF-8");
        }
        catch (IOException e) {
            return erreur(err, fichier + " : lecture impossible ("
                + e.getMessage() + ")");
        }

        long finRecherche = System.nanoTime();

        // ======================================
        // 4. AFFICHAGE DU RESULTAT
        // ======================================

        // Fins de ligne en \n, comme grep, quel que soit le système
        if (compter) {
            out.print(resultat.getNombreTrouvees() + "\n");
        }
        else {
            for (ResultatRecherche.LigneTrouvee ligne :
                    resultat.getLignes()) {

                if (numeroter) {
                    out.print(ligne.getNumero() + ":");
                }

                out.print(ligne.getTexte());
                out.print('\n');
            }
        }

        if (stats) {

            // Les temps de construction incluent l'analyse de la regex
            double construction =
                (finConstruction - debutConstruction) / 1_000_000.0;

            if (utiliserKmp) {
                err.println(
                    "Algorithme                 : kmp (motif de "
                    + motifLitteral.length() + " caracteres)");
                err.printf(
                    "Construction table KMP     : %.3f ms%n",
                    construction);
            }
            else {
                err.println(
                    "Algorithme                 : automate");
                err.println(
                    "Matcher                    : " + typeMatcher
                    + (unePasse
                        ? " (automate de .*R)"
                        : " (automate de R)"));
                err.println(
                    "Etats AFN-epsilon          : "
                    + automate.getAfn().getEtats().size());
                err.println(
                    "Etats AFD                  : "
                    + automate.getAfd().getEtats().size());
                err.println(
                    "Etats AFD minimal          : "
                    + automate.getAfdMinimal().getEtats().size());
                err.printf(
                    "Construction de l'automate : %.3f ms%n",
                    construction);
            }

            err.printf(
                "Recherche                  : %.3f ms%n",
                (finRecherche - debutRecherche) / 1_000_000.0);
            err.println(
                "Lignes lues                : "
                + resultat.getLignesLues());
            err.println(
                "Caracteres lus             : "
                + resultat.getCaracteresLus());
            err.println(
                "Lignes trouvees            : "
                + resultat.getNombreTrouvees());
        }

        return resultat.getNombreTrouvees() > 0 ? TROUVE : RIEN_TROUVE;
    }

    // Ouvre le fichier, ou l'entrée standard pour « - ».
    // Le décodage UTF-8 est strict : un texte non UTF-8
    // lève MalformedInputException.
    private static BufferedReader ouvrir(String fichier)
            throws IOException {

        if (fichier.equals("-")) {
            return new BufferedReader(
                new InputStreamReader(
                    System.in,
                    StandardCharsets.UTF_8.newDecoder()));
        }

        return Files.newBufferedReader(
            Path.of(fichier),
            StandardCharsets.UTF_8);
    }

    // Sans locale UTF-8 (LANG vide, LC_ALL=C...), la JVM décode les
    // arguments de la ligne de commande en ASCII (sun.jnu.encoding) :
    // chaque octet non ASCII du motif devient le caractère de
    // remplacement U+FFFD, et la recherche est fausse. Ce décodage ne
    // peut plus être corrigé une fois la JVM lancée : on prévient
    // seulement, sur stderr. Un motif non ASCII ou abîmé contient
    // forcément un caractère de code > 127 (U+FFFD compris).
    private static void avertirSiMotifMalDecode(
            String regex,
            PrintStream err) {

        String encodage = System.getProperty("sun.jnu.encoding", "");

        if (encodage.isEmpty() || estUtf8(encodage)) {
            return;
        }

        boolean nonAscii = false;

        for (int i = 0; i < regex.length() && !nonAscii; i++) {
            nonAscii = regex.charAt(i) > 127;
        }

        if (nonAscii) {
            err.println(
                "Main : attention, les arguments sont décodés en "
                + encodage + " et non en UTF-8 : les caractères non"
                + " ASCII du motif ont pu être abîmés.");
            err.println(
                "       Lancez le programme dans une locale UTF-8, par"
                + " exemple : LC_ALL=C.UTF-8 java -jar egrep-clone.jar ...");
        }
    }

    private static boolean estUtf8(String encodage) {
        try {
            return Charset.forName(encodage).equals(StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Affiche le message et l'usage sur la sortie d'erreur
    private static int erreur(PrintStream err, String message) {
        err.println("Main : " + message);
        err.println(USAGE);
        return ERREUR;
    }
}
