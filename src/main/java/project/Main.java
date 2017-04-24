package project;

import gate.*;
import gate.util.GateException;
import gate.util.Out;
import gate.util.SimpleFeatureMapImpl;
import gate.util.persistence.PersistenceManager;
import org.apache.tools.ant.util.regexp.Regexp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final FeatureMap QUESTION_AND_ANSWERS_FEATURE_MAP = new SimpleFeatureMapImpl() {{
        put("class", "page-question");
    }};
    private static final FeatureMap QUESTION_OWNER_FEATURE_MAP = new SimpleFeatureMapImpl() {{
        put("class", "fn nickname");
        put("itemprop", "name");
    }};
    private static final FeatureMap ANSWERS_FEATURE_MAP = new SimpleFeatureMapImpl() {{
        put("itemtype", "http://schema.org/Answer");
    }};
    private static final FeatureMap ANSWERS_VALUE_FEATURE_MAP = new SimpleFeatureMapImpl() {{
        put("class", "a--atext atext");
        put("itemprop", "text");
    }};
    private static AnnotationSet tokens;
    /**
     * The Corpus Pipeline application to contain ANNIE
     */
    private CorpusController annieController;

    public static void main(String[] args) throws GateException, IOException {

        args = new String[4];
        args[0] = "https://otvet.mail.ru/question/88777513";
        args[1] = "https://otvet.mail.ru/question/200120719";
        args[2] = "https://otvet.mail.ru/question/200120718";
        args[3] = "https://otvet.mail.ru/question/200120707";

        Out.prln("Initialising GATE...");
        Gate.init();
        Out.prln("...GATE initialised");
        Main annie = new Main();
        annie.initAnnie();
        Corpus corpus = Factory.newCorpus("StandAloneAnnie corpus");
        executeAnnie(args, annie, corpus);

        for (Document document : corpus) {
            System.out.println("=======================================");
            System.out.println("Ссылка " + document.getSourceUrl());
            tokens = document.getAnnotations().get("Token");
            AnnotationSet page = document.getNamedAnnotationSets().get("Original markups");
            AnnotationSet questionAndAnswers = page.get("div", QUESTION_AND_ANSWERS_FEATURE_MAP);
            questionAndAnswers = page.get(
                    questionAndAnswers.firstNode().getOffset(),
                    questionAndAnswers.lastNode().getOffset()
            );
            AnnotationSet authors = page.get("b", QUESTION_OWNER_FEATURE_MAP);
            TreeSet<Annotation> annotations = new TreeSet<>(authors);
            String authorText = getValueInTags(annotations.first());
            System.out.print(authorText + " спрашивает === ");
            AnnotationSet text = questionAndAnswers.get("index");
            String questionTitle = getValueInTags(text);
            System.out.println(questionTitle);

            Set<Annotation> answers = new TreeSet<>(page.get("div", ANSWERS_FEATURE_MAP));
            for (Annotation answer : answers) {
                AnnotationSet tempAnswers = page.get(
                        answer.getStartNode().getOffset(),
                        answer.getEndNode().getOffset()
                );
                authors = tempAnswers.get("b", QUESTION_OWNER_FEATURE_MAP);
                authorText = getValueInTags(authors);
                System.out.print(authorText + " отвечает ===");
                text = tempAnswers.get("div", ANSWERS_VALUE_FEATURE_MAP);
                questionTitle = getValueInTags(text);
                System.out.println(questionTitle);
            }
        }
    }

    private static String getValueInTags(Annotation annotation) {
        Long startOffset = annotation.getStartNode().getOffset();
        Long endOffset = annotation.getEndNode().getOffset();
        return getValueInTags(startOffset, endOffset);
    }

    private static String getValueInTags(Long startOffset, Long endOffset) {
        StringBuilder value = new StringBuilder();
        AnnotationSet annotations = tokens.get(startOffset, endOffset);
        TreeSet<Annotation> sortedAnnotations = new TreeSet<>(annotations);
        Iterator<Annotation> iterator = sortedAnnotations.iterator();
        while (iterator.hasNext()) {
            Annotation next = iterator.next();
            FeatureMap features = next.getFeatures();
            value.append(String.valueOf(features.get("string")) + " ");
        }
        Pattern compile = Pattern.compile("(http(s)?( )*:( )*\\/( )*\\/)?( )*([\\w -.]+)\\.( )*([a-z]{2,6}\\.?)( )*((/( )*[(\\w. )|?|=]*))*");
        Matcher matcher = compile.matcher(value);
        if (matcher.find()){
            String group = matcher.group(0);
            String withoutSpaces = group.replaceAll(" ", "");
            int startIndex = value.indexOf(group);
            value.replace(startIndex, startIndex + group.length(), withoutSpaces + " ");
        }
        return value.toString();
    }

    private static String getValueInTags(AnnotationSet tag) {
        Long startOffset = tag.firstNode().getOffset();
        Long endOffset = tag.lastNode().getOffset();
        return getValueInTags(startOffset, endOffset);
    }

    private static void executeAnnie(String[] args, Main annie, Corpus corpus) throws MalformedURLException, GateException {
        for (int i = 0; i < args.length; i++) {
            URL u = new URL(args[i]);
            FeatureMap params = Factory.newFeatureMap();
            params.put("sourceUrl", u);
            params.put("preserveOriginalContent", new Boolean(true));
            params.put("collectRepositioningInfo", new Boolean(true));
            params.put("encoding", "utf-8");
            Out.prln("Creating doc for " + u);
            Document doc = (Document)
                    Factory.createResource("gate.corpora.DocumentImpl", params);
            corpus.add(doc);
        } // for each of args

        // tell the pipeline about the corpus and run it
        annie.annieController.setCorpus(corpus);
        annie.execute();
    }

    /**
     * Initialise the ANNIE system. This creates a "corpus pipeline"
     * application that can be used to run sets of documents through
     * the extraction system.
     */
    public void initAnnie() throws GateException, IOException {
        Out.prln("Initialising ANNIE...");

        // load the ANNIE application from the saved state in plugins/ANNIE
        File pluginsHome = Gate.getPluginsHome();
        File anniePlugin = new File(pluginsHome, "ANNIE");
        File annieGapp = new File(anniePlugin, "ANNIE_with_defaults.gapp");
        annieController =
                (CorpusController) PersistenceManager.loadObjectFromFile(annieGapp);

        Out.prln("...ANNIE loaded");
    } // initAnnie()

    /**
     * Run ANNIE
     */
    public void execute() throws GateException {
        Out.prln("Running ANNIE...");
        annieController.execute();
        Out.prln("...ANNIE complete");
    } // execute()
}
