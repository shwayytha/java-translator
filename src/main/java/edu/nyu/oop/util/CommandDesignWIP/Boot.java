package edu.nyu.oop.util.CommandDesignWIP;

import edu.nyu.oop.CppAst;
import edu.nyu.oop.CppCommands;
import edu.nyu.oop.CppHeaderAstGenerator;
import edu.nyu.oop.GenerateJavaASTs;
import edu.nyu.oop.util.*;
// import edu.nyu.oop.util.SourceHeaderOutputCommand;
// import edu.nyu.oop.util.SourceCppOutputCommand;
// import edu.nyu.oop.util.SourceMainOutputCommand;


import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;

import xtc.tree.Location;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.util.Tool;
import xtc.lang.JavaPrinter;
import xtc.parser.ParseException;


/**
 * This is the entry point to your program. It configures the user interface, defining
 * the set of valid commands for your tool, provides feedback to the user about their inputs
 * and delegates to other classes based on the commands input by the user to classes that know
 * how to handle them. So, for example, do not put translation code in Boot. Remember the
 * Single Responsiblity Principle https://en.wikipedia.org/wiki/Single_responsibility_principle
 */
public class Boot extends Tool {
    private Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    @Override
    public String getName() {
        return XtcProps.get("app.name");
    }

    @Override
    public String getCopy() {
        return XtcProps.get("group.name");
    }

    @Override
    public void init() {
        super.init();
        // Declare command line arguments.
        runtime.
        bool("printJavaAst", "printJavaAst", false, "Print Java Ast.").
        bool("printJavaCode", "printJavaCode", false, "Print Java code.").
        bool("printJavaImportCode", "printJavaImportCode", false, "Print Java code for imports and package source.").
        bool("translateJava", "translateJava", false, "Translate Java to C++.");
    }

    @Override
    public void prepare() {
        super.prepare();
        // Perform consistency checks on command line arguments.
        // (i.e. are there some commands that cannot be run together?)
    }

    @Override
    public File locate(String name) throws IOException {
        File file = super.locate(name);
        if (Integer.MAX_VALUE < file.length()) {
            throw new IllegalArgumentException("File too large " + file.getName());
        }
        if(!file.getAbsolutePath().startsWith(System.getProperty("user.dir"))) {
            throw new IllegalArgumentException("File must be under project root.");
        }
        return file;
    }

    @Override
    public Node parse(Reader in, File file) throws IOException, ParseException {
        return NodeUtil.parseJavaFile(file);
    }

    @Override
    public void process(Node n) {
        if (runtime.test("printJavaAst")) {
            runtime.console().format(n).pln().flush();
        }

        if (runtime.test("printJavaCode")) {
            new JavaPrinter(runtime.console()).dispatch(n);
            runtime.console().flush();
        }

        if (runtime.test("printJavaImportCode")) {
            List<GNode> nodes = JavaFiveImportParser.parse((GNode) n);
            for(Node node : nodes) {
                runtime.console().pln();
                new JavaPrinter(runtime.console()).dispatch(node);
            }
            runtime.console().flush();
        }

        if (runtime.test("translateJava")) {
            // NOTE: must type in EXACT filename (same case!!) or file is added twice by dependency parser

            String workingDir = System.getProperty("user.dir");

            Location nLocation = n.getLocation();
            Location longLocation = new Location(workingDir + "/" + nLocation.file, nLocation.line, nLocation.column);
            n.setLocation(longLocation);

            List<SourceOutputCommand> cmds = new ArrayList<SourceOutputCommand>();

            //phase 1
            List<GNode> allAsts = GenerateJavaASTs.beginParse((GNode) n);

            //phase 2
            CppAst headerCppAst = CppHeaderAstGenerator.generateNew(allAsts);

            cmds.add(new SourceHeaderOutputCommand(new CppHVisitor(), headerCppAst));

            //phase 4 + 5
           // cmds.addAll(CppCommands.convertToCpp(allAsts));

            TranslationWriter tW = new TranslationWriter();

            for (SourceOutputCommand cmd : cmds)
            {
                tW.execute(cmd);
            }

        }



        // if (runtime.test("Your command here.")) { ... don't forget to add it to init()
    }

    /**
     * Run Boot with the specified command line arguments.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        new Boot().run(args);
    }
}