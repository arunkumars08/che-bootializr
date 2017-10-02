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
package com.redhat.developers.service;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

@Service
@Slf4j
public class TemplateService {

    private final Template template;

    public TemplateService(Mustache.Compiler compiler) throws Exception {
        File templatefile = ResourceUtils.getFile("classpath:templates/.factory.json.tpl");
        Reader tplReader = new FileReader(templatefile);
        template = compiler.compile(tplReader);
    }

    /**
     *
     * @param context
     * @return
     * @throws Exception
     */
    public String buildFactoryJsonFromTemplate(Map<String, String> context) throws Exception {
        String factoryJson = template.execute(context);
        return factoryJson;
    }
}
