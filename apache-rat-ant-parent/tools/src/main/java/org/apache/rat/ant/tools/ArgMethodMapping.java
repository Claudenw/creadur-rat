/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.rat.ant.tools;

import org.apache.rat.ant.AntOption;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * Maps Options to their data processing templates.
 */
public final class ArgMethodMapping {
    /** The template for FileSet processing */
    private final Template fileSetTemplate;
    /** The template for Text processing */
    private final Template textValueTemplate;
    /** The default template */
    private final Template defaultTemplate;

    ArgMethodMapping(final String templatePath, final VelocityEngine velocityEngine) {
        fileSetTemplate = velocityEngine.getTemplate(templatePath + "/fileSetTemplate.vm");
        textValueTemplate = velocityEngine.getTemplate(templatePath + "/textValueTemplate.vm");
        defaultTemplate = velocityEngine.getTemplate(templatePath + "/defaultTypeTemplate.vm");
    }

    /**
     * Gets the template for the option and updates the context to contain any special options.
     * @param antOption the Option to get the template for.
     * @param context the context to update.
     * @return the template of @{code null} if there is no template.
     */
    Template getTemplate(final AntOption antOption, final VelocityContext context) {
        if (antOption.hasArg()) {
            switch (antOption.getArgType()) {
                case FILE:
                case DIRORARCHIVE:
                    context.put("argType", "FileSet");
                    context.put("argName", "fileSet");
                    return fileSetTemplate;
                case NONE:
                    return null;
                case STANDARDCOLLECTION:
                    context.put("argType", "Std");
                    return textValueTemplate;
                case EXPRESSION:
                    context.put("argType", "Expr");
                    return textValueTemplate;
                case COUNTERPATTERN:
                    context.put("argType", "Cntr");
                    return textValueTemplate;
                case LICENSEID:
                case FAMILYID:
                    context.put("argType", "Lst");
                    return textValueTemplate;
                default:
                    return defaultTemplate;
            }
        }
        return null;
    }
}
