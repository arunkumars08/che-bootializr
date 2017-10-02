/**
 * Copyright (c) 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.developers;

import com.redhat.developers.utils.GeneralUtil;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlTest {

    @Test
    public void should_return_url_without_dotgit() {
        String url = "https://github.com/skshiftio/demoadd2.git";
        url = GeneralUtil.sanitizeGitUrl(url);
        assertThat(url).isEqualTo("https://github.com/skshiftio/demoadd2");

    }
}
