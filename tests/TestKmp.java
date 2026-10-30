import java.util.Random;

import kmp.Kmp;

/**
 * Tests de {@link Kmp}, sans JUnit : chaque test compare
 * {@code Kmp.contientMotif} à {@code String.contains}, sur des cas
 * pièges, des cas limites et 20 000 couples (motif, ligne) tirés au
 * hasard avec une graine fixe. Affiche le nombre de tests réussis,
 * et s'arrête avec le code 1 au premier échec.
 *
 * <p>Lancement : {@code ant test}, ou à la main :</p>
 * <pre>
 *   javac --release 11 -encoding UTF-8 -d bin $(find src -name '*.java')
 *   javac --release 11 -encoding UTF-8 -cp bin -d bin-tests tests/*.java
 *   java -cp bin:bin-tests TestKmp
 * </pre>
 */
public class TestKmp {

    /** Classe de test : seul {@link #main(String[])} est utile. */
    private TestKmp() {
    }

    // Graine fixe : les tests aleatoires sont reproductibles
    private static final long GRAINE = 20261030L;
    private static final int NOMBRE_ALEATOIRES = 20_000;

    private static int reussis = 0;

    /**
     * Lance tous les tests ; quitte avec le code 1 au premier échec.
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {

        // Cas pieges : un echec partiel ne doit pas faire sauter
        // une occurrence qui commence dans la partie deja lue
        verifier("aab", "aaab");
        verifier("aa", "aaa");
        verifier("abab", "abaabab");
        verifier("ababc", "abababc");
        verifier("aaaab", "aaaaaaab");

        // Cas limites
        verifier("a", "a");
        verifier("a", "bbb");
        verifier("a", "bba");
        verifier("abcdef", "abc");
        verifier("abc", "");
        verifier("Danatoff’s", "Mr. Danatoff’s Appointment");
        verifier("’s", "Danatoff's");

        // Couples aleatoires sur l'alphabet {a, b}
        Random hasard = new Random(GRAINE);

        for (int i = 0; i < NOMBRE_ALEATOIRES; i++) {

            String motif = motAleatoire(hasard, 1 + hasard.nextInt(6));
            String ligne = motAleatoire(hasard, hasard.nextInt(30));

            verifier(motif, ligne);
        }

        System.out.println(reussis + " tests réussis");
    }

    // Compare Kmp a String.contains ; quitte avec le code 1 si
    // les deux ne sont pas d'accord
    private static void verifier(String motif, String ligne) {

        boolean attendu = ligne.contains(motif);
        boolean obtenu = new Kmp(motif).contientMotif(ligne);

        if (obtenu != attendu) {
            System.out.println(
                "ECHEC apres " + reussis + " tests reussis : motif « "
                + motif + " », ligne « " + ligne + " », attendu "
                + attendu + ", obtenu " + obtenu);
            System.exit(1);
        }

        reussis++;
    }

    // Mot de la longueur donnee sur l'alphabet {a, b} : un petit
    // alphabet multiplie les bords et les echecs partiels
    private static String motAleatoire(Random hasard, int longueur) {

        StringBuilder mot = new StringBuilder(longueur);

        for (int i = 0; i < longueur; i++) {
            mot.append(hasard.nextBoolean() ? 'a' : 'b');
        }

        return mot.toString();
    }
}
