/**
 * Copyright (c) 2017 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.developers;

import com.redhat.developers.config.CheBootalizrProperties;
import com.redhat.developers.config.CheBootializrConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("ut")
public class CheBootializrApplicationTests {

    @Autowired
    private CheBootalizrProperties cheBootalizrProperties;

    @Test
    public void test_properties_are_loaded() {


        assertThat(cheBootalizrProperties).isNotNull();
        assertThat(cheBootalizrProperties.getChe()).isNotNull();
        assertThat(cheBootalizrProperties.getChe().getUrl()).isNull();
    }

}
