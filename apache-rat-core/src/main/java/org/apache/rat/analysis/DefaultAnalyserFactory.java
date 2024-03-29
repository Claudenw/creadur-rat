/*
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 */
package org.apache.rat.analysis;

import java.util.Collection;

import org.apache.rat.ConfigurationException;
import org.apache.rat.api.Document;
import org.apache.rat.document.IDocumentAnalyser;
import org.apache.rat.document.RatDocumentAnalysisException;
import org.apache.rat.document.impl.guesser.ArchiveGuesser;
import org.apache.rat.document.impl.guesser.BinaryGuesser;
import org.apache.rat.document.impl.guesser.NoteGuesser;
import org.apache.rat.license.ILicense;
import org.apache.rat.utils.Log;

/**
 * Creates default analysers.
 */
public class DefaultAnalyserFactory {

    /**
     * Creates a DocumentAnalyser from a collection of ILicenses.
     * @param licenses The licenses to use in the Analyser.
     * @return A document analyser that uses the provides licenses.
     */
    public static IDocumentAnalyser createDefaultAnalyser(Log log, Collection<ILicense> licenses) {
        if (licenses.isEmpty()) {
            throw new ConfigurationException("At least one license must be defined");
        }
        log.debug("Licenses in Test");
        licenses.forEach(log::debug);
        return new DefaultAnalyser(log, licenses);
    }

    /**
     * A DocumentAnalyser for the license
     */
    private final static class DefaultAnalyser implements IDocumentAnalyser {

        /** The licenses to analyze */
        private final Collection<ILicense> licenses;
        /** The log to use */
        private final Log log;

        /**
         * Constructs a DocumentAnalyser for the specified license.
         * @param license The license to analyse
         */
        public DefaultAnalyser(final Log log, final Collection<ILicense> licenses) {
            this.licenses = licenses;
            this.log = log;
        }

        @Override
        public void analyse(Document document) throws RatDocumentAnalysisException {
            if (NoteGuesser.isNote(document)) {
                document.getMetaData().setDocumentType(Document.Type.notice);
            } else if (ArchiveGuesser.isArchive(document)) {
                document.getMetaData().setDocumentType(Document.Type.archive);
            } else if (BinaryGuesser.isBinary(document)) {
                document.getMetaData().setDocumentType(Document.Type.binary);
            } else {
                document.getMetaData().setDocumentType(Document.Type.standard);
                new DocumentHeaderAnalyser(log, licenses).analyse(document);
            }
        }
    }
}
