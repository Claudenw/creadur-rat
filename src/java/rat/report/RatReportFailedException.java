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
package rat.report;

import org.apache.commons.lang.exception.NestableException;

public class RatReportFailedException extends NestableException {

    private static final long serialVersionUID = 4940711222435919034L;

    public RatReportFailedException() {
        super();
    }

    public RatReportFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RatReportFailedException(String message) {
        super(message);
    }

    public RatReportFailedException(Throwable cause) {
        super(cause);
    }
}
