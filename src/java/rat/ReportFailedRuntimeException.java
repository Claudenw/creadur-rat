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
package rat;

/**
 * Indicates that a report has failed in a fatal manner.
 */
class ReportFailedRuntimeException extends RuntimeException {
    
    private static final long serialVersionUID = -7605175211996815712L;

    public ReportFailedRuntimeException() {
        super();
    }

    public ReportFailedRuntimeException(String message, Throwable t) {
        super(message, t);
    }

    public ReportFailedRuntimeException(String message) {
        super(message);
    }

    public ReportFailedRuntimeException(Throwable t) {
        super(t);
    }    
}
