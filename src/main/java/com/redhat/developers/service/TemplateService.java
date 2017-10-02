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
