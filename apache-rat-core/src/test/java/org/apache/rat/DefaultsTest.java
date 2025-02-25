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
package org.apache.rat;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.rat.license.ILicense;
import org.apache.rat.license.LicenseSetFactory.LicenseFilter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class DefaultsTest {
    private static final String[] FAMILIES = { "BSD-3", "GPL  ", "AL   ", "OASIS", "W3CD ", "W3C  ", "MIT  ", "CDDL1" };

    @Test
    public void defaultConfigTest() {
        Defaults defaults = Defaults.builder().build();

        Set<ILicense> licenses = defaults.getLicenseSetFactory().getLicenses(LicenseFilter.ALL);

        Set<String> names = new TreeSet<>();
        licenses.forEach(x -> names.add(x.getLicenseFamily().getFamilyCategory()));
        assertThat(names).hasSize(FAMILIES.length);
        names.removeAll(Arrays.asList(FAMILIES));
        assertThat(names).isEmpty();
    }
}
