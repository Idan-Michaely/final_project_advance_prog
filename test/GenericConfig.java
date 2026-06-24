package test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import test.TopicManagerSingleton.TopicManager;

/**
 * A {@link Config} that instantiates agents from a plain-text configuration file
 * using Java reflection, then wraps each in a {@link ParallelAgent} for
 * asynchronous processing.
 *
 * <h3>Configuration file format</h3>
 * Each agent is described by three consecutive lines:
 * <pre>
 * fully.qualified.ClassName
 * inputTopic1,inputTopic2,...
 * outputTopic1,outputTopic2,...
 * </pre>
 * The class must have a constructor {@code (String[] subs, String[] pubs)}.
 *
 * <p>Example ({@code simple.conf}):
 * <pre>
 * test.PlusAgent
 * A,B
 * C
 * test.IncAgent
 * C
 * D
 * </pre>
 */
public class GenericConfig implements Config {

    private String confFile;

    /** Parallel agents created during {@link #create()}. */
    private final List<ParallelAgent> agents = new ArrayList<>();

    /** Subscription topics per agent (parallel list with {@link #agents}). */
    private final List<String[]> subsList = new ArrayList<>();

    /** Publication topics per agent (parallel list with {@link #agents}). */
    private final List<String[]> pubsList = new ArrayList<>();

    /** Error messages collected during {@link #create()}; empty if everything succeeded. */
    private final List<String> errors = new ArrayList<>();

    /** Returns all errors collected during the last {@link #create()} call. */
    public List<String> getErrors() { return new ArrayList<>(errors); }

    /** Returns {@code true} if the last {@link #create()} produced any errors. */
    public boolean hasErrors()      { return !errors.isEmpty(); }

    /**
     * Sets the path to the configuration file.
     * Can be an absolute path or relative to the current working directory.
     *
     * @param path path to the {@code .conf} file
     */
    public void setConfFile(String path) {
        this.confFile = path;
    }

    /**
     * Reads the configuration file, instantiates each agent via reflection,
     * wraps it in a {@link ParallelAgent}, and subscribes the wrapper to the
     * configured topics.  One background thread is started per agent.
     */
    @Override
    public void create() {
        errors.clear();
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(confFile));
        } catch (IOException e) {
            errors.add("Cannot read config file: " + e.getMessage());
            return;
        }

        // Remove blank lines and comments so the file is more forgiving
        List<String> cleaned = new ArrayList<>();
        for (String l : lines) {
            String t = l.trim();
            if (!t.isEmpty() && !t.startsWith("#")) cleaned.add(t);
        }

        if (cleaned.isEmpty()) {
            errors.add("Config file is empty.");
            return;
        }

        if (cleaned.size() % 3 != 0) {
            errors.add("Config format error: expected groups of 3 lines (className / inputs / outputs),"
                    + " but got " + cleaned.size() + " non-blank lines.");
            return;
        }

        TopicManager tm = TopicManagerSingleton.get();

        for (int i = 0; i + 2 < cleaned.size(); i += 3) {
            String className = cleaned.get(i);
            String[] subs    = cleaned.get(i + 1).split(",");
            String[] pubs    = cleaned.get(i + 2).split(",");

            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> ctor = clazz.getConstructor(String[].class, String[].class);
                Agent inner = (Agent) ctor.newInstance(new Object[]{subs, pubs});

                ParallelAgent pa = new ParallelAgent(inner, 10);

                for (String sub : subs) tm.getTopic(sub.trim()).subscribe(pa);
                for (String pub : pubs) tm.getTopic(pub.trim()).addPublisher(pa);

                agents.add(pa);
                subsList.add(subs);
                pubsList.add(pubs);

            } catch (ClassNotFoundException e) {
                errors.add("Line " + (i + 1) + ": class not found — \"" + className
                        + "\". Check the class name and package prefix (e.g. test.PlusAgent).");
            } catch (NoSuchMethodException e) {
                errors.add("Line " + (i + 1) + ": \"" + className
                        + "\" has no constructor (String[] subs, String[] pubs).");
            } catch (Exception e) {
                errors.add("Line " + (i + 1) + ": failed to create \"" + className
                        + "\" — " + e.getMessage());
            }
        }
    }

    /**
     * Unsubscribes all agents from their topics and shuts down their
     * background threads.  Blocks until all threads have terminated.
     */
    @Override
    public void close() {
        TopicManager tm = TopicManagerSingleton.get();
        for (int i = 0; i < agents.size(); i++) {
            ParallelAgent pa = agents.get(i);
            for (String sub : subsList.get(i)) {
                tm.getTopic(sub.trim()).unsubscribe(pa);
            }
            for (String pub : pubsList.get(i)) {
                tm.getTopic(pub.trim()).removePublisher(pa);
            }
            pa.close();
        }
        agents.clear();
        subsList.clear();
        pubsList.clear();
    }

    @Override
    public String getName() { return "GenericConfig"; }

    @Override
    public int getVersion() { return 1; }
}
