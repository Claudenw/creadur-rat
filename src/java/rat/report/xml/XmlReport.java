/*
 * Copyright 2006 Robert Burrell Donkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package rat.report.xml;

import java.io.IOException;

import rat.document.IDocument;
import rat.document.IDocumentAnalyser;
import rat.document.RatDocumentAnalysisException;
import rat.report.RatReport;
import rat.report.RatReportFailedException;
import rat.report.xml.writer.IXmlWriter;

class XmlReport implements RatReport {
   
    private final IDocumentAnalyser analyser;
    private final IXmlWriter writer;
    
    public XmlReport(final IXmlWriter writer, IDocumentAnalyser analyser) {
        this.analyser = analyser;
        this.writer = writer;
    }

    public void startReport() throws RatReportFailedException {
        try {
            writer.openElement("rat-report");
        } catch (IOException e) {
            throw new RatReportFailedException("Cannot open start element", e);
        }
    }

    public void endReport() throws RatReportFailedException {
        try {
            writer.closeDocument();
        } catch (IOException e) {
            throw new RatReportFailedException("Cannot close last element", e);
        }
    }

    public void report(IDocument document) throws RatReportFailedException {
        try {
            analyser.analyse(document);
        } catch (RatDocumentAnalysisException e) {
            throw new RatReportFailedException("Analysis failed", e);
        }
    }    
}
