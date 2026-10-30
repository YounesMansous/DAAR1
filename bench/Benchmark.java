import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ast.interfaces.AstI;

import automate.AstToAfnVisitor;
import automate.ConstructionAutomate;

import experimentation.Matcher;
import experimentation.MatcherNaif;
import experimentation.MatcherUnePasse;

import kmp.Kmp;

import parser.Parser;

/**
 * Banc de performances dans la JVM : compare les algorithmes sur des
 * lignes déjà chargées en mémoire, sans le coût de lecture et de
 * décodage du fichier (qui domine les mesures de bout en bout).
 *
 * <p>Pour chaque cas : {@value #CHAUFFE} itérations de chauffe, puis
 * {@value #MESURES} mesures avec {@link System#nanoTime()}. Chaque
 * itération reconstruit le matcher (temps de construction : analyse
 * de la regex, puis AFD minimal et table, ou table KMP), puis
 * parcourt toutes les lignes (temps de recherche).</p>
 *
 * <p>Expériences :</p>
 * <ul>
 *   <li>a : KMP contre automate une passe, motifs littéraux tirés
 *       du texte, de longueur 2 à 70 ;</li>
 *   <li>b : matcher naïf contre une passe, sur 6 regex et 3 textes
 *       réels, puis sur un pire cas (une ligne de « a », regex
 *       a.*b) ;</li>
 *   <li>c : passage à l'échelle sur des fichiers de 1 à 32 Mio,
 *       générés dans {@code bench/genere/} (non versionné) ;</li>
 *   <li>d : taille et temps de construction des automates (regex du
 *       banc de vérification, explosion des sous-ensembles,
 *       littéraux de 10 à 400 lettres).</li>
 * </ul>
 *
 * <p>Chaque lancement mesure une seule configuration (une expérience
 * et un algorithme : kmp, unepasse, naif ou thompson) et ajoute ses
 * lignes à {@code resultats/bench_jvm.csv}. Chaque configuration a
 * donc sa propre JVM, comme le programme réel, qui n'utilise qu'un
 * algorithme par lancement : mesurés dans la même JVM, les
 * algorithmes se gênent, car le JIT compile le code commun
 * ({@code AfdCompile.suivant}) d'après les mesures précédentes (le
 * naïf devient deux fois plus lent si KMP et l'une-passe ont tourné
 * avant lui). Le mode {@code --verifier} relit ensuite le CSV et
 * s'arrête avec le code 1 si deux algorithmes ne trouvent pas le même
 * nombre de lignes pour un même cas.</p>
 *
 * <p>Lancement (depuis la racine) : {@code ant bench}, ou
 * {@code java -cp bin:bin-bench Benchmark <a|b|c|d> <kmp|unepasse|naif|thompson>},
 * puis {@code java -cp bin:bin-bench Benchmark --verifier}.</p>
 */
public class Benchmark {

    private static final int CHAUFFE = 5;
    private static final int MESURES = 20;

    // Au-delà, on n'augmente plus la longueur de ligne du pire cas
    private static final double LIMITE_NAIF_MS = 5000;

    private static final long GRAINE = 20261030L;
    private static final long MIO = 1L << 20;

    private static final Path DATA = Path.of("data");
    private static final Path GENERE = Path.of("bench", "genere");
    private static final Path SORTIE = Path.of("resultats", "bench_jvm.csv");

    private static final String ENTETE = "experience,regex,fichier,"
        + "taille_octets,algo,matcher,iteration,t_construction_ms,"
        + "t_recherche_ms,nb_lignes,etats_afn,etats_afd,etats_min,"
        + "etats_min_pointetoile";

    private static PrintWriter csv;
    private static PrintStream out;

    // Configuration mesurée par ce lancement : kmp, unepasse, naif
    // ou thompson ; les autres cas de l'expérience sont sautés
    private static String configuration;

    /** Construit un matcher à partir de la regex (temps mesuré). */
    private interface Construction {
        Matcher construire();
    }

    private Benchmark() {
    }

    /**
     * Mesure une configuration, ou vérifie le CSV.
     *
     * @param args une expérience (a, b, c ou d) et un algorithme
     *             (kmp, unepasse, naif ou thompson), ou {@code --verifier}
     * @throws IOException si un fichier ne peut pas être lu ou écrit
     */
    public static void main(String[] args) throws IOException {

        out = new PrintStream(
            new FileOutputStream(java.io.FileDescriptor.out),
            true, StandardCharsets.UTF_8);

        if (args.length == 1 && args[0].equals("--verifier")) {
            verifierCsv();
            return;
        }

        if (args.length != 2
                || !List.of("a", "b", "c", "d").contains(args[0])
                || !List.of("kmp", "unepasse", "naif", "thompson")
                    .contains(args[1])) {
            System.err.println("usage : Benchmark <a|b|c|d> "
                + "<kmp|unepasse|naif|thompson> | --verifier");
            System.exit(2);
        }

        configuration = args[1];

        Files.createDirectories(SORTIE.getParent());

        boolean nouveau = !Files.exists(SORTIE) || Files.size(SORTIE) == 0;

        try (BufferedWriter ecrivain = Files.newBufferedWriter(
                SORTIE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            csv = new PrintWriter(ecrivain);

            if (nouveau) {
                csv.println(ENTETE);
            }

            long debut = System.nanoTime();

            switch (args[0]) {
                case "a": experienceA(); break;
                case "b": experienceB(); break;
                case "c": experienceC(); break;
                default: experienceD(); break;
            }

            csv.flush();

            out.printf(Locale.ROOT, "%s %s terminé en %.0f s%n",
                args[0], configuration, (System.nanoTime() - debut) / 1e9);
        }
    }

    // Relit le CSV : pour chaque cas (expérience, regex, fichier), tous
    // les algorithmes doivent trouver le même nombre de lignes
    private static void verifierCsv() throws IOException {

        Map<String, Set<String>> parCas = new LinkedHashMap<>();
        int cas = 0;

        for (String ligne : Files.readAllLines(SORTIE, StandardCharsets.UTF_8)) {

            if (ligne.equals(ENTETE)) {
                continue;
            }

            List<String> champs = decouper(ligne);
            String nombre = champs.get(9);

            if (nombre.isEmpty()) {
                continue;
            }

            String cle = champs.get(0) + " | « " + champs.get(1) + " » | "
                + champs.get(2);
            parCas.computeIfAbsent(cle, k -> new HashSet<>())
                .add(champs.get(4) + "/" + champs.get(5) + "=" + nombre);
        }

        for (Map.Entry<String, Set<String>> e : parCas.entrySet()) {

            Set<String> nombres = new HashSet<>();

            for (String v : e.getValue()) {
                nombres.add(v.substring(v.indexOf('=') + 1));
            }

            if (nombres.size() > 1) {
                System.err.println("ERREUR : nombres de lignes différents "
                    + "pour " + e.getKey() + " : " + e.getValue());
                System.exit(1);
            }

            cas++;
        }

        out.println("Vérification : même nombre de lignes pour tous les "
            + "algorithmes, dans les " + cas + " cas mesurés.");
    }

    // Découpe une ligne du CSV (champs éventuellement entre guillemets)
    private static List<String> decouper(String ligne) {

        List<String> champs = new ArrayList<>();
        StringBuilder champ = new StringBuilder();
        boolean guillemets = false;

        for (int i = 0; i < ligne.length(); i++) {

            char c = ligne.charAt(i);

            if (guillemets) {
                if (c == '"' && i + 1 < ligne.length()
                        && ligne.charAt(i + 1) == '"') {
                    champ.append('"');
                    i++;
                }
                else if (c == '"') {
                    guillemets = false;
                }
                else {
                    champ.append(c);
                }
            }
            else if (c == '"') {
                guillemets = true;
            }
            else if (c == ',') {
                champs.add(champ.toString());
                champ.setLength(0);
            }
            else {
                champ.append(c);
            }
        }

        champs.add(champ.toString());
        return champs;
    }

    // ==================================================
    // a. KMP CONTRE AUTOMATE UNE PASSE
    // ==================================================

    private static void experienceA() throws IOException {

        out.println("== a : KMP contre automate une passe ==");

        Path fichier = DATA.resolve("don-quixote-996.txt");
        List<String> lignes = charger(fichier);
        Random hasard = new Random(GRAINE);

        for (int longueur : new int[] {2, 3, 5, 8, 12, 20, 40, 70}) {

            String motif = tirerLitteral(lignes, longueur, hasard);
            String nom = fichier.getFileName().toString();
            long taille = Files.size(fichier);
            int[] etats = etats(motif);

            mesurer("a", motif, nom, taille, "kmp", "-",
                () -> kmp(motif), lignes, etats);
            mesurer("a", motif, nom, taille, "automate", "unepasse",
                () -> unePasse(motif), lignes, etats);
        }
    }

    // Sous-chaîne du texte de la longueur voulue, sans | * . ( ),
    // sans espace au début ni à la fin (graine fixe)
    private static String tirerLitteral(
            List<String> lignes, int longueur, Random hasard) {

        while (true) {

            String ligne = lignes.get(hasard.nextInt(lignes.size()));

            if (ligne.length() < longueur) {
                continue;
            }

            int debut = hasard.nextInt(ligne.length() - longueur + 1);
            String motif = ligne.substring(debut, debut + longueur);

            boolean valide = !motif.startsWith(" ")
                && !motif.endsWith(" ");

            for (char c : motif.toCharArray()) {
                valide &= "|*.()﻿".indexOf(c) < 0;
            }

            if (valide) {
                return motif;
            }
        }
    }

    // ==================================================
    // b. NAIF CONTRE UNE PASSE
    // ==================================================

    private static void experienceB() throws IOException {

        out.println("== b : naïf contre une passe, textes réels ==");

        String[] regex = {
            "the",
            "S(a|g|r)*on",
            "(Mr|Mrs) (Darcy|Bennet)",
            "h.*e.*r",
            "((a|b)*c)*d",
            ".(Bab|Ass)y",
        };

        String[] fichiers = {
            "yellow-wallpaper-1952.txt", "text1.txt", "don-quixote-996.txt"
        };

        for (String nom : fichiers) {

            Path fichier = DATA.resolve(nom);
            List<String> lignes = charger(fichier);
            long taille = Files.size(fichier);

            for (String r : regex) {

                int[] etats = etats(r);

                mesurer("b_textes", r, nom, taille, "automate", "naif",
                    () -> naif(r), lignes, etats);
                mesurer("b_textes", r, nom, taille, "automate", "unepasse",
                    () -> unePasse(r), lignes, etats);
            }
        }

        out.println("== b : pire cas, une ligne de « a », regex a.*b ==");

        String r = "a.*b";
        int[] etats = etats(r);

        for (int n : new int[] {1_000, 5_000, 10_000, 20_000, 50_000}) {

            List<String> lignes = List.of("a".repeat(n));
            String nom = "a^" + n;

            mesurer("b_pire_cas", r, nom, n, "automate", "unepasse",
                () -> unePasse(r), lignes, etats);
            double pire = mesurer("b_pire_cas", r, nom, n, "automate",
                "naif", () -> naif(r), lignes, etats);

            if (pire > LIMITE_NAIF_MS) {
                out.printf(Locale.ROOT,
                    "  arrêt : une mesure du naïf dépasse %.0f s%n",
                    LIMITE_NAIF_MS / 1000);
                break;
            }
        }
    }

    // ==================================================
    // c. PASSAGE A L'ECHELLE
    // ==================================================

    private static void experienceC() throws IOException {

        out.println("== c : passage à l'échelle ==");

        String litteral = "together";
        String etoile = "S(a|g|r)*on";
        int[] etatsLitteral = etats(litteral);
        int[] etatsEtoile = etats(etoile);

        for (int mio : new int[] {1, 2, 4, 8, 16, 32}) {

            Path fichier = generer(mio);
            List<String> lignes = charger(fichier);
            String nom = fichier.getFileName().toString();
            long taille = Files.size(fichier);

            mesurer("c_echelle", litteral, nom, taille, "kmp", "-",
                () -> kmp(litteral), lignes, etatsLitteral);
            mesurer("c_echelle", litteral, nom, taille, "automate",
                "unepasse", () -> unePasse(litteral), lignes,
                etatsLitteral);
            mesurer("c_echelle", litteral, nom, taille, "automate",
                "naif", () -> naif(litteral), lignes, etatsLitteral);
            mesurer("c_echelle", etoile, nom, taille, "automate",
                "unepasse", () -> unePasse(etoile), lignes, etatsEtoile);
            mesurer("c_echelle", etoile, nom, taille, "automate",
                "naif", () -> naif(etoile), lignes, etatsEtoile);
        }
    }

    /**
     * Génère (si besoin) {@code bench/genere/genere-NNMio.txt} : les
     * lignes des textes de data/ (par ordre de nom de fichier),
     * répétées jusqu'à la taille voulue, sans couper de ligne. La
     * même construction est reprise par bench_bout_en_bout.py.
     */
    private static Path generer(int mio) throws IOException {

        Path fichier = GENERE.resolve(
            String.format(Locale.ROOT, "genere-%02dMio.txt", mio));

        if (Files.exists(fichier)) {
            return fichier;
        }

        Files.createDirectories(GENERE);

        List<Path> textes = new ArrayList<>();

        try (DirectoryStream<Path> dossier =
                Files.newDirectoryStream(DATA, "*.txt")) {
            dossier.forEach(textes::add);
        }

        textes.sort(null);

        long cible = mio * MIO;
        long total = 0;

        try (BufferedWriter ecrivain =
                Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {

            boucle:
            while (true) {

                for (Path texte : textes) {

                    for (String ligne : charger(texte)) {

                        long octets =
                            ligne.getBytes(StandardCharsets.UTF_8).length + 1;

                        if (total + octets > cible) {
                            break boucle;
                        }

                        ecrivain.write(ligne);
                        ecrivain.write('\n');
                        total += octets;
                    }
                }
            }
        }

        return fichier;
    }

    // ==================================================
    // d. TAILLE ET CONSTRUCTION DES AUTOMATES
    // ==================================================

    private static void experienceD() throws IOException {

        out.println("== d : regex du banc de vérification ==");

        for (String r : Files.readAllLines(
                Path.of("tests", "regex.txt"), StandardCharsets.UTF_8)) {

            if (r.startsWith("#")) {
                continue;
            }

            construction("d_regex", r);
        }

        out.println("== d : explosion des sous-ensembles ==");

        for (int n = 1; n <= 8; n++) {
            construction("d_explosion", "(a|b)*a" + "(a|b)".repeat(n));
        }

        out.println("== d : construction de Thompson ==");

        Random hasard = new Random(GRAINE);

        for (int k : new int[] {10, 25, 50, 100, 200, 300, 400}) {

            StringBuilder mot = new StringBuilder();

            for (int i = 0; i < k; i++) {
                mot.append((char) ('a' + hasard.nextInt(26)));
            }

            String r = mot.toString();
            int[] etats = etats(r);

            // Thompson seul : analyse de la regex et AFN-ε
            mesurer("d_thompson", r, "-", 0, "thompson", "-",
                () -> {
                    new Parser(r).parse().accept(new AstToAfnVisitor());
                    return null;
                },
                null, etats);
            mesurer("d_thompson", r, "-", 0, "automate", "naif",
                () -> naif(r), null, etats);
        }
    }

    // Temps de construction de l'automate de R et de celui de .*R
    private static void construction(String experience, String r) {

        int[] etats = etats(r);

        mesurer(experience, r, "-", 0, "automate", "naif",
            () -> naif(r), null, etats);
        mesurer(experience, r, "-", 0, "automate", "unepasse",
            () -> unePasse(r), null, etats);
    }

    // ==================================================
    // MESURE
    // ==================================================

    /**
     * Mesure un cas : chauffe, puis mesures, une ligne de CSV par
     * mesure. Sans lignes, seule la construction est mesurée. Un cas
     * d'une autre configuration que celle du lancement est sauté.
     *
     * @return le plus long temps de recherche mesuré (ms), 0 si sauté
     */
    private static double mesurer(
            String experience,
            String regex,
            String fichier,
            long taille,
            String algo,
            String matcher,
            Construction construction,
            List<String> lignes,
            int[] etats) {

        String config = algo.equals("automate") ? matcher : algo;

        if (!config.equals(configuration)) {
            return 0;
        }

        double sommeConstruction = 0;
        double sommeRecherche = 0;
        double pire = 0;
        long nombre = -1;

        for (int i = 0; i < CHAUFFE + MESURES; i++) {

            long t0 = System.nanoTime();
            Matcher m = construction.construire();
            long t1 = System.nanoTime();
            long n = lignes == null ? -1 : compter(m, lignes);
            long t2 = System.nanoTime();

            double tConstruction = (t1 - t0) / 1e6;
            double tRecherche = (t2 - t1) / 1e6;

            nombre = n;

            if (i < CHAUFFE) {
                continue;
            }

            sommeConstruction += tConstruction;
            sommeRecherche += tRecherche;
            pire = Math.max(pire, tRecherche);

            csv.printf(Locale.ROOT,
                "%s,%s,%s,%d,%s,%s,%d,%.4f,%s,%s,%d,%d,%d,%d%n",
                experience, champ(regex), champ(fichier), taille, algo,
                matcher, i - CHAUFFE + 1, tConstruction,
                lignes == null
                    ? "" : String.format(Locale.ROOT, "%.4f", tRecherche),
                lignes == null ? "" : String.valueOf(n),
                etats[0], etats[1], etats[2], etats[3]);
        }

        out.printf(Locale.ROOT,
            "  %-11s %-9s %-8s %-26s %-26s constr. %9.3f ms  "
            + "recherche %9.3f ms  %s lignes%n",
            experience, algo, matcher, abreger(regex), abreger(fichier),
            sommeConstruction / MESURES, sommeRecherche / MESURES,
            lignes == null ? "-" : String.valueOf(nombre));

        return pire;
    }

    // Une boucle par type de matcher : chaque appel de contientMotif
    // ne voit qu'une seule classe, et le JIT peut l'inliner.
    private static long compter(Matcher m, List<String> lignes) {

        if (m instanceof Kmp) {
            return compterKmp((Kmp) m, lignes);
        }

        if (m instanceof MatcherUnePasse) {
            return compterUnePasse((MatcherUnePasse) m, lignes);
        }

        return compterNaif((MatcherNaif) m, lignes);
    }

    private static long compterKmp(Kmp m, List<String> lignes) {
        long n = 0;
        for (String ligne : lignes) {
            if (m.contientMotif(ligne)) {
                n++;
            }
        }
        return n;
    }

    private static long compterUnePasse(
            MatcherUnePasse m, List<String> lignes) {
        long n = 0;
        for (String ligne : lignes) {
            if (m.contientMotif(ligne)) {
                n++;
            }
        }
        return n;
    }

    private static long compterNaif(MatcherNaif m, List<String> lignes) {
        long n = 0;
        for (String ligne : lignes) {
            if (m.contientMotif(ligne)) {
                n++;
            }
        }
        return n;
    }

    // ==================================================
    // CONSTRUCTIONS (ce qui est chronométré)
    // ==================================================

    private static Matcher kmp(String regex) {
        AstI ast = new Parser(regex).parse();
        return new Kmp(Kmp.motifLitteral(ast));
    }

    private static Matcher unePasse(String regex) {
        AstI ast = new Parser(regex).parse();
        return new MatcherUnePasse(
            new ConstructionAutomate(MatcherUnePasse.envelopper(ast))
                .getCompile());
    }

    private static Matcher naif(String regex) {
        AstI ast = new Parser(regex).parse();
        return new MatcherNaif(new ConstructionAutomate(ast).getCompile());
    }

    // États de l'AFN-ε, de l'AFD et de l'AFD minimal de R, puis de
    // l'AFD minimal de .*R (hors mesure)
    private static int[] etats(String regex) {

        AstI ast = new Parser(regex).parse();
        ConstructionAutomate r = new ConstructionAutomate(ast);
        ConstructionAutomate pointEtoile = new ConstructionAutomate(
            MatcherUnePasse.envelopper(ast));

        return new int[] {
            r.getAfn().getEtats().size(),
            r.getAfd().getEtats().size(),
            r.getAfdMinimal().getEtats().size(),
            pointEtoile.getAfdMinimal().getEtats().size(),
        };
    }

    // ==================================================
    // OUTILS
    // ==================================================

    // Même découpage que FileSearcher (BufferedReader.readLine)
    private static List<String> charger(Path fichier) throws IOException {
        return Files.readAllLines(fichier, StandardCharsets.UTF_8);
    }

    // Champ CSV entre guillemets (les regex contiennent des virgules)
    private static String champ(String valeur) {
        return "\"" + valeur.replace("\"", "\"\"") + "\"";
    }

    private static String abreger(String texte) {
        return texte.length() <= 26 ? texte : texte.substring(0, 25) + "…";
    }
}
