/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.rat.ant.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.WordUtils;
import org.apache.rat.DeprecationReporter;
import org.apache.rat.ant.AntOption;
import org.apache.rat.ant.AntOptionCollection;
import org.apache.rat.report.xml.writer.XmlWriter;
import org.apache.rat.utils.CasedString;
import org.apache.rat.utils.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;


/**
 * A simple tool to convert CLI options into an Ant report base class.
 */
public final class CodeGenerator {
    /** The xml version of the license text */
    private static final String XML_LICENSE_TEXT = """
 Licensed to the Apache Software Foundation (ASF) under one   *
 or more contributor license agreements.  See the NOTICE file *
 distributed with this work for additional information        *
 regarding copyright ownership.  The ASF licenses this file   *
 to you under the Apache License, Version 2.0 (the            *
 "License"); you may not use this file except in compliance   *
 with the License.  You may obtain a copy of the License at   *
                                                              *
   https://www.apache.org/licenses/LICENSE-2.0                *
                                                              *
 Unless required by applicable law or agreed to in writing,   *
 software distributed under the License is distributed on an  *
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 KIND, either express or implied.  See the License for the    *
 specific language governing permissions and limitations      *
 under the License.                                           *
""";
    /** The Syntax for this command */
    private static final String SYNTAX = String.format("java -cp ... %s [options]", CodeGenerator.class.getName());
    /** The package name for this AbstractMaven file */
    private final CasedString packageName = new CasedString(CasedString.StringCase.DOT, "org.apache.rat.ant");
    /** The Ant Options for this run */
    private final AntOptionCollection antOptionCollection;
    /** The directory where we will write the output */
    private final String baseDirectory;
    /** The template for the method */
    private final Template elementTemplate;
    /** The template for the class */
    private final Template javaTemplate;
    /** The template for attribute types */
    private final Template attributeTemplate;
    /** The template for element classes */
    private final Template elementClassTemplate;
    /** The argument template mapping */
    private final ArgMethodMapping argMethodMapping;

    private CodeGenerator(final String baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.antOptionCollection = new AntOptionCollection();

        final VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();

        // retrieve the templates
        final String templatePath = new CasedString(CasedString.StringCase.DOT, CodeGenerator.class.getPackageName()).toCase(CasedString.StringCase.SLASH);
        elementTemplate = velocityEngine.getTemplate(templatePath + "/ElementMethod.vm");
        javaTemplate = velocityEngine.getTemplate(templatePath + "/AbstractAnt.vm");
        elementClassTemplate = velocityEngine.getTemplate(templatePath + "/AntElementClass.vm");
        attributeTemplate = velocityEngine.getTemplate(templatePath + "/AttributeMethod.vm");
        argMethodMapping = new ArgMethodMapping(templatePath, velocityEngine);
    }

    /**
     * Gets the options for the command line.
     * @return the command line options.
     */
    private static Options getOptions() {
        return new Options()
                .addOption(Option.builder("h").longOpt("help").desc("Print this message").build())
                .addOption(Option.builder("p").longOpt("path").required().hasArg().desc("The path to the base of the generated java code directory").build());
    }


    /**
     * Executable entry point.
     * @param args the arguments for the executable
     * @throws IOException on IO error.
     */
    public static void main(final String[] args) throws IOException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        CommandLine commandLine = null;
        try {
            commandLine = DefaultParser.builder().setDeprecatedHandler(DeprecationReporter.getLogReporter())
                    .build().parse(getOptions(), args);
        } catch (ParseException pe) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(SYNTAX, pe.getMessage(), getOptions(), "");
            System.exit(1);
        }

        if (commandLine.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(SYNTAX, getOptions());
            System.exit(0);
        }
        CodeGenerator codeGenerator = new CodeGenerator(commandLine.getOptionValue("p"));
        codeGenerator.execute();
       // AbstractCodeGenerator.processArgs(SYNTAX, CodeGenerator::new, args);
    }

    private void createJavaFile() throws IOException {
        Set<AntOption> elementClasses = new HashSet<>();
        final VelocityContext context = new VelocityContext();
        TemplateContextList templateContexts = gatherMethods(elementClasses);
        elementClasses.forEach(antOption -> templateContexts.add(getElementClass(antOption, context)));
        context.put("methods", templateContexts);

        File javaFile = Path.of(baseDirectory).resolve(packageName.toCase(CasedString.StringCase.SLASH))
                .resolve("AntReport.java").toFile();
        FileUtils.mkDir(javaFile.getParentFile());
        try (FileWriter fileWriter = new FileWriter(javaFile, StandardCharsets.UTF_8)) {
            javaTemplate.merge(context, fileWriter);
        }
    }

    /**
     * Executes the code generation.
     * @throws IOException on IO error
     */
    private void execute() throws IOException {
        createJavaFile();
        createAntLib();
    }

    private void createAntLib() throws IOException {
        File antlibFile = Path.of(baseDirectory).resolve("../resources")
                .resolve(packageName.toCase(CasedString.StringCase.SLASH))
                .resolve("antlib.xml").toFile();
        FileUtils.mkDir(antlibFile.getParentFile());
        try (FileWriter fWriter = new FileWriter(antlibFile, StandardCharsets.UTF_8);
             XmlWriter writer = new XmlWriter(fWriter)) {
            writer.startDocument()
                    .comment(XML_LICENSE_TEXT)
                    .startElement("antlib")
                    .startElement("taskdef")
                    .attribute("name", "report")
                    .attribute("classname", packageName.toString())
                    .closeElement();
            for (String subclass : new String[]{"Std", "Expr", "Cntr"}) {
                writer.startElement("taskdef")
                        .attribute("name", subclass.toLowerCase(Locale.ROOT))
                        .attribute("classname", packageName.toString() + "AbstractAnTReport$" + subclass)
                        .closeElement();
            }
            writer.closeDocument();
        }
    }

    private void taskDef(XmlWriter writer, String subclass, String packageName) throws IOException {
        writer.startElement("taskdef")
                .attribute("name", subclass)
                .attribute("classname", packageName);
    }


    /**
     * Gathers all method definitions into a single string.
     * @return the definition of all the methods.
     */
    private TemplateContextList gatherMethods(Set<AntOption> children) {
        TemplateContextList result = new TemplateContextList();
        List<AntOption> optionsToProcess = antOptionCollection.getMappedOptions().toList();
        for (AntOption antOption : optionsToProcess) {
            VelocityContext context = new VelocityContext();
            context.put("option", antOption);
            String desc = StringEscapeUtils.escapeHtml4(antOption.getDescription());
            context.put("desc", desc);
            context.put("argDesc", StringEscapeUtils.escapeHtml4(createArgDesc(antOption, desc)));
            String methodName = WordUtils.capitalize(antOption.getName());
            context.put("methodName", methodName);
            String argumentType = antOption.isAttribute() ?
                    (antOption.hasArg() ? "String" : "boolean") :
                    (antOption.hasArg() ? antOption.getArgName() : "");
            context.put("argType", argumentType);
            if (antOption.isAttribute()) {
                result.add(new TemplateContext(attributeTemplate, context));
            } else {
                children.add(antOption);
                children.addAll(antOption.convertedFrom());
            }
        }
        return result;
    }

    /**
     * Gets the argument description for the method.
     * @param antOption the ant option generating the method.
     * @param desc the description of the argument.
     * @return the argument description for the method.}.
     */
    private String createArgDesc(final AntOption antOption, final String desc) {
        return antOption.hasArg() ? desc.substring(desc.indexOf(" ") + 1, desc.indexOf(".") + 1) :
                "The state";
    }

    private TemplateContextOutput getElementClass(final AntOption option, final VelocityContext innerContext) {

        final VelocityContext classContext = new VelocityContext(innerContext);
        classContext.put("className", WordUtils.capitalize(option.getName()));

        Set<AntOption> options = option.convertedFrom();
        options.add(option);
        TemplateContextList children = new TemplateContextList();
        for (AntOption childOption : options) {
            final VelocityContext childContext = new VelocityContext(innerContext);
            childContext.put("argName", WordUtils.uncapitalize(childOption.getArgName()));
            childContext.put("argType", childOption.getArgName());
            final Template innerTemplate = argMethodMapping.getTemplate(childOption, childContext);
            if (innerTemplate != null) {
                children.add(new TemplateContext(innerTemplate, childContext));
            }
        }
        classContext.put("children", children);
        return new TemplateContext(elementClassTemplate, classContext);
    }

    /**
     * A template context pair that will produce the output when requested.
     */
    interface TemplateContextOutput {
        /** Applies the Template to the Context and put the result on the {@code writer}.
         *
         * @param writer the writer to write to.
         * @return the {@code writer} parameter.
         */
        Writer writeTo(Writer writer);
    }

    /**
     * A simple implementation of {@link TemplateContextOutput}.
     * @param template the Template.
     * @param context the VelocityContext.
     */
    private record TemplateContext(Template template, VelocityContext context) implements TemplateContextOutput {
        @Override
        public String toString() {
            return writeTo(new StringWriter()).toString();
        }

        @Override
        public Writer writeTo(final Writer writer) {
            template.merge(context, writer);
            return writer;
        }
    }

    /**
     * An implementation of a list of {@link TemplateContextOutput} that renders each
     * contained template context output when {@code toString} is called.
     */
    private static class TemplateContextList implements TemplateContextOutput {
        /** THe list of TemplateContextOutput objects */
        private List<TemplateContextOutput> templateContextMappings = new ArrayList<>();

        public void add(final TemplateContextOutput mapping) {
            templateContextMappings.add(mapping);
        }

        @Override
        public String toString() {
            return writeTo(new StringWriter()).toString();
        }

        @Override
        public Writer writeTo(final Writer writer) {
            templateContextMappings.forEach(templateContextMapping -> {
                templateContextMapping.writeTo(writer);
//                try {
//                    writer.append(System.lineSeparator());
//                } catch (IOException e) {
//                    // do nothing.
//                }
            });
            return writer;
        }
    }
}
